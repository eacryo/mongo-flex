package com.github.eacryo.mongoflex.strategy;

import com.github.eacryo.mongoflex.convertor.MongoMappingConvertor;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class FindExecutor implements CommandExecutor {

    @Autowired
    private ExecutorProxy executorProxy;
    @Autowired
    private MongoMappingConvertor mongoMappingConvertor;

    @Override
    public Object execute(String command, MongoCollection<Document> collection, Document queryContent, Method method, Object[] args) throws Exception {
        Type genericReturnType = method.getGenericReturnType();
        Class<?> listElementClass;
        if (genericReturnType instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) genericReturnType;
            listElementClass = (Class<?>) pType.getActualTypeArguments()[0];
        } else {
            listElementClass = Object.class;
        }
        List<Object> results = new ArrayList<>();
        if (listElementClass == Object.class) {
            collection.find(queryContent).forEach(doc ->
                    results.add(mongoMappingConvertor.documentToMap(doc)));
        } else {
            collection.find(queryContent).forEach(doc -> {
                Object entity = mongoMappingConvertor.read(doc, listElementClass);
                results.add(entity);
            });
        }

        // 处理返回类型
        if (method.getReturnType().equals(List.class)) {
            return results;
        } else if (!results.isEmpty()) {
            return results.get(0);
        }
        return null;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        executorProxy.registerExecutor("find", this);
    }
}
