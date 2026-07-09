package com.github.eacryo.mongoflex.strategy;


import com.github.eacryo.mongoflex.convertor.MongoMappingConvertor;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
public class FindOneExecutor implements CommandExecutor{

    @Autowired
    private ExecutorProxy executorProxy;
    @Autowired
    private MongoMappingConvertor mongoMappingConvertor;

    @Override
    public Object execute(String command, MongoCollection<Document> collection,
                          Document queryContent, Method method, Object[] args) throws Exception {
                Document doc = collection.find(queryContent).first();
                if (doc != null) {
                    return mongoMappingConvertor.read(doc,method.getReturnType());
                }
                return doc;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        executorProxy.registerExecutor("findOne", this);
    }
}
