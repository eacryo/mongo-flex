package com.github.eacryo.mongoflex.v2;

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
    private final Supplier<MongoDatabase> databaseSupplier;
    private final QueryParser queryParser = new QueryParser();
    private final JacksonDocumentConverter jacksonDocumentConverter;
    private final SimpleMongoRepository<T, ID> baseRepository;
    //只能通过下面的构造器来注入
    private final ExecutorProxy executorProxy;

    // 这里的 collection 不再是固定的，因为查询语句可以指定不同的集合
    // private final MongoCollection<Document> collection;

    public MyRepositoryProxyHandler(Supplier<MongoDatabase> databaseSupplier,
                                    JacksonDocumentConverter jacksonDocumentConverter,
                                    SimpleMongoRepository<T, ID> baseRepository,
                                    ExecutorProxy executorProxy) {
        this.databaseSupplier = databaseSupplier;
        this.jacksonDocumentConverter = jacksonDocumentConverter;
        this.baseRepository = baseRepository;
        this.executorProxy = executorProxy;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Type genericReturnType = method.getGenericReturnType();
        if (method.isAnnotationPresent(Mql.class)) {
            Mql myQuery = method.getAnnotation(Mql.class);
            String shellCommand = myQuery.value();
            //TODO
            Parameter[] parameters = method.getParameters();
            shellCommand = replaceShellCommand(shellCommand, parameters, args);
            //shellCommand = shellCommand.replace("#{name}", (String) args[0]);
            // 1. 解析 MongoDB Shell 语句
            // 这里的 args 数组将用于替换占位符，但你希望解析完整的shell语句，所以先不处理占位符
            QueryParser.QueryCommand parsedCommand = queryParser.parse(shellCommand);

            // 2. 获取对应的 MongoCollection
            MongoCollection<Document> collection = databaseSupplier.get().getCollection(parsedCommand.collectionName);
            //TODO：下面这考虑挪到QueryParser里面

            // 3. 根据解析出的命令执行相应的操作
            //处理来自IBaseRepositoryV2中的方法
            return executorProxy.execute(parsedCommand.operation, collection, parsedCommand.queryDoc, method, args);
        } else if (isMethodFromTargetInterface(method, targetInterface)) {
            log.info("Method {} inherit from parent interface", method.getName());
            Object invoked = method.invoke(baseRepository, args);
            return invoked;
        } else {
            //不是通过@Mql注解的方法，也不是继承自IBaseRepositoryV2的方法，抛出异常
            throw new UnsupportedOperationException("Method " + method.getName() +
                    " is neither annotated with @Mql nor inherited from IBaseRepositoryV2.");

        }

    }

    // 辅助方法：将Document映射回Java对象
    private <T> T mapDocumentToEntity(Document doc, Class<T> clazz) {
        // ... (保持不变，或使用更完善的映射逻辑)
        try {
            T instance = clazz.getDeclaredConstructor().newInstance();
            instance = jacksonDocumentConverter.convert(doc, clazz);
            //SimpleMongoConverter simpleMongoConverter = new SimpleMongoConverter();
            //instance = simpleMongoConverter.convert(doc, clazz);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Error mapping document to entity", e);
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
