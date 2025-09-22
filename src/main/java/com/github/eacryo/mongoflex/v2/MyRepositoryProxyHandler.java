package com.github.eacryo.mongoflex.v2;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyRepositoryProxyHandler<T> implements InvocationHandler {

    private final MongoCollection<Document> collection;
    private final Class<T> entityClass;
    private final Pattern paramPattern = Pattern.compile("\\?(\\d+)"); // 用于匹配 ?0, ?1

    public MyRepositoryProxyHandler(MongoDatabase database, Class<T> entityClass) {
        this.collection = database.getCollection(entityClass.getSimpleName().toLowerCase());
        this.entityClass = entityClass;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.isAnnotationPresent(Mql.class)) {
            Mql myQuery = method.getAnnotation(Mql.class);
            String queryStr = myQuery.value();

            // 1. 解析并替换查询字符串中的参数
            Matcher matcher = paramPattern.matcher(queryStr);
            while (matcher.find()) {
                int paramIndex = Integer.parseInt(matcher.group(1));
                if (paramIndex < args.length) {
                    // 这里只是简单的字符串替换，实际生产中需要更复杂的JSON处理
                    // 例如，处理字符串、数字、布尔等不同类型
                    queryStr = queryStr.replace(matcher.group(), formatParam(args[paramIndex]));
                }
            }

            // 2. 将JSON字符串转换为MongoDB的Document
            Document queryDoc = Document.parse(queryStr);

            // 3. 执行MongoDB查询
            List<T> results = new ArrayList<>();
            collection.find(queryDoc).forEach(doc -> {
                // 4. 将Document映射回实体对象
                T entity = mapDocumentToEntity(doc, entityClass);
                results.add(entity);
            });

            // 5. 根据返回类型处理结果（例如，返回单个对象或列表）
            if (method.getReturnType().equals(List.class)) {
                return results;
            } else if (!results.isEmpty()) {
                return results.get(0);
            }
            return null;
        }

        // 如果没有@MyQuery注解，则执行默认行为（例如，交给默认方法）
        // 也可以抛出异常表示不支持此方法
        throw new UnsupportedOperationException("Method " + method.getName() + " is not annotated with @MyQuery");
    }

    // 辅助方法：将参数格式化为JSON可接受的格式
    private String formatParam(Object arg) {
        if (arg instanceof String) {
            return "\"" + arg + "\"";
        }
        return arg.toString();
    }

    // 辅助方法：将Document映射回Java对象
    // 这部分需要你自己的反射或序列化逻辑
    private T mapDocumentToEntity(Document doc, Class<T> clazz) {
        // 简单的示例：你可以使用Jackson或Gson等库来自动映射
        // 或者手动使用反射
        try {
            T instance = clazz.getDeclaredConstructor().newInstance();
            // 这里可以循环遍历doc的key-value，通过反射设置给instance的字段
            // ...
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Error mapping document to entity", e);
        }
    }
}
