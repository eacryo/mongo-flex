package com.github.eacryo.mongoflex.strategy;

import com.github.eacryo.mongoflex.v2.JacksonDocumentConverter;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@Component
public class FindExecutor implements CommandExecutor {

    @Autowired
    private ExecutorProxy executorProxy;
    @Autowired
    private JacksonDocumentConverter jacksonDocumentConverter;

    @Override
    public Object execute(String command, MongoCollection<Document> collection, Document queryContent, Method method, Object[] args) throws Exception {
        Type genericReturnType = method.getGenericReturnType();
        ParameterizedType pType = (ParameterizedType) genericReturnType;
        Type rawType = pType.getRawType();
        Type actualType = pType.getActualTypeArguments()[0]; // 获取泛型参数T
        Class<?> listElementClass = (Class<?>) actualType;
        List<Object> results = new ArrayList<>();
        collection.find(queryContent).forEach(doc -> {
            Object entity = jacksonDocumentConverter.convert(doc, listElementClass);
            results.add(entity);
        });

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
