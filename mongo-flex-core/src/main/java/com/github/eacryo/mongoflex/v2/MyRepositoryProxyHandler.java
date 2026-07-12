package com.github.eacryo.mongoflex.v2;

import com.github.eacryo.mongoflex.convertor.MongoMappingConvertor;
import com.github.eacryo.mongoflex.strategy.ExecutorProxy;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.lang.reflect.*;
import java.util.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.function.Supplier;

@Slf4j
public class MyRepositoryProxyHandler<T, ID> implements InvocationHandler {

    private final Class<?> targetInterface = MongoRepository.class;
    private final Class<?> repositoryInterface;
    private final Supplier<MongoDatabase> databaseSupplier;
    private final QueryParser queryParser = new QueryParser();
    private final MongoMappingConvertor mongoMappingConvertor;
    private final SimpleMongoRepository<T, ID> baseRepository;
    private final ExecutorProxy executorProxy;

    public MyRepositoryProxyHandler(Class<?> repositoryInterface,
                                    Supplier<MongoDatabase> databaseSupplier,
                                    MongoMappingConvertor mongoMappingConvertor,
                                    SimpleMongoRepository<T, ID> baseRepository,
                                    ExecutorProxy executorProxy) {
        this.repositoryInterface = Objects.requireNonNull(repositoryInterface, "repositoryInterface must not be null");
        this.databaseSupplier = Objects.requireNonNull(databaseSupplier, "databaseSupplier must not be null");
        this.mongoMappingConvertor = Objects.requireNonNull(mongoMappingConvertor, "mongoMappingConvertor must not be null");
        this.baseRepository = Objects.requireNonNull(baseRepository, "baseRepository must not be null");
        this.executorProxy = Objects.requireNonNull(executorProxy, "executorProxy must not be null");
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            return doInvoke(proxy, method, args);
        } catch (InvocationTargetException e) {
            // Unwrap so the caller sees the original exception, not UndeclaredThrowableException / 解包使调用方能捕获原始异常，而非 UndeclaredThrowableException
            throw e.getCause();
        }
    }

    private Object doInvoke(Object proxy, Method method, Object[] args) throws Throwable {
        Type genericReturnType = method.getGenericReturnType();
        if (method.isAnnotationPresent(Mql.class)) {
            Mql myQuery = method.getAnnotation(Mql.class);
            String shellCommand = myQuery.value();
            //TODO
            Parameter[] parameters = method.getParameters();
            shellCommand = replaceShellCommand(shellCommand, parameters, args);
            // Parse MongoDB shell command / 解析 MongoDB Shell 语句
            QueryParser.QueryCommand parsedCommand = queryParser.parse(shellCommand);

            // TODO: consider moving this into QueryParser / 下面这考虑挪到QueryParser里面
            MongoCollection<Document> collection = databaseSupplier.get().getCollection(parsedCommand.collectionName);

            // Execute based on parsed command / 根据解析出的命令执行相应的操作
            return executorProxy.execute(parsedCommand.operation, collection, parsedCommand.arguments,
                    parsedCommand.skip, parsedCommand.limit, method, args);
        } else if (isMethodFromTargetInterface(method, targetInterface)) {
            log.info("Method {} inherit from parent interface", method.getName());
            return method.invoke(baseRepository, args);
        } else if (isMethodFromTargetInterface(method, Object.class)) {
            if ("toString".equals(method.getName())) {
                return "Repository proxy for " + repositoryInterface.getName();
            }
            return method.invoke(baseRepository, args);
        } else {
            // Neither @Mql nor inherited from MongoRepository / 不是 @Mql 方法，也不是继承自 MongoRepository 的方法
            throw new UnsupportedOperationException("Method " + method.getName() +
                    " is neither annotated with @Mql nor inherited from MongoRepository.");
        }
    }

    private String replaceShellCommand(String shellCommand, Parameter[] parameters, Object[] args) {
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            // 检查是否有 @Param 注解
            Param paramAnnotation = param.getAnnotation(Param.class);
            if (paramAnnotation != null) {
                // 获取注解值作为参数名
                String paramName = paramAnnotation.value();
                // 将参数名和实际参数值存入 Map
                shellCommand = shellCommand.replace("#{" + paramName + "}", args[i].toString());
            }
        }
        return shellCommand;
    }

    private boolean isMethodFromTargetInterface(Method method, Class<?> targetInterface) {
        for (Method interfaceMethod : targetInterface.getMethods()) {
            if (interfaceMethod.getName().equals(method.getName()) &&
                    Arrays.equals(interfaceMethod.getParameterTypes(), method.getParameterTypes())) {
                return true;
            }
        }
        return false;
    }
}
