package com.github.eacryo.mongoflex.v2;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MyRepositoryProxyHandler implements InvocationHandler {

    private final MongoDatabase database;
    private final QueryParser queryParser = new QueryParser();

    // 这里的 collection 不再是固定的，因为查询语句可以指定不同的集合
    // private final MongoCollection<Document> collection;

    public MyRepositoryProxyHandler(MongoDatabase database) {
        this.database = database;
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
            MongoCollection<Document> collection = database.getCollection(parsedCommand.collectionName);

            // 3. 根据解析出的命令执行相应的操作
            if ("find".equals(parsedCommand.command)) {
                ParameterizedType pType = (ParameterizedType) genericReturnType;
                Type rawType = pType.getRawType();
                Type actualType = pType.getActualTypeArguments()[0]; // 获取泛型参数T
                Class<?> listElementClass = (Class<?>) actualType;
                List<Object> results = new ArrayList<>();
                collection.find(parsedCommand.queryDoc).forEach(doc -> {
                    Object entity = mapDocumentToEntity(doc, listElementClass);
                    results.add(entity);
                });

                // 处理返回类型
                if (method.getReturnType().equals(List.class)) {
                    return results;
                } else if (!results.isEmpty()) {
                    return results.get(0);
                }
                return null;

            } else if ("findOne".equals(parsedCommand.command)) {
                Document doc = collection.find(parsedCommand.queryDoc).first();
//                if (doc != null) {
//                    return mapDocumentToEntity(doc, entityClass);
//                }
                return null;
            } else {
                throw new UnsupportedOperationException("Unsupported MongoDB command: " + parsedCommand.command);
            }
        }

        throw new UnsupportedOperationException("Method " + method.getName() + " is not annotated with @MyQuery");
    }

    // 辅助方法：将Document映射回Java对象
    private <T> T mapDocumentToEntity(Document doc, Class<T> clazz) {
        // ... (保持不变，或使用更完善的映射逻辑)
        try {
            T instance = clazz.getDeclaredConstructor().newInstance();
            SimpleMongoConverter simpleMongoConverter = new SimpleMongoConverter();
            instance = simpleMongoConverter.convert(doc, clazz);
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
}
