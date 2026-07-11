package com.github.eacryo.mongoflex.strategy;


import com.github.eacryo.mongoflex.convertor.MongoMappingConvertor;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;
import java.util.List;

public class FindOneExecutor implements CommandExecutor{

    @Autowired
    private ExecutorProxy executorProxy;
    @Autowired
    private MongoMappingConvertor mongoMappingConvertor;

    @Override
    public Object execute(String command, MongoCollection<Document> collection,
                          List<Document> arguments, Integer skip, Integer limit,
                          Method method, Object[] args) throws Exception {
                Document queryContent = arguments.get(0);
                Document doc = collection.find(queryContent).first();
                if (doc != null) {
                    if (method.getReturnType() == Object.class) {
                        return mongoMappingConvertor.documentToMap(doc);
                    }
                    return mongoMappingConvertor.read(doc,method.getReturnType());
                }
                return doc;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        executorProxy.registerExecutor("findOne", this);
    }
}
