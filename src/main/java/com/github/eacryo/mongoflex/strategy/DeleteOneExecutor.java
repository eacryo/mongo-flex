package com.github.eacryo.mongoflex.strategy;


import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.internal.bulk.DeleteRequest;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
public class DeleteOneExecutor implements CommandExecutor{

    @Autowired
    private ExecutorProxy executorProxy;

    @Override
    public Object execute(String command, MongoCollection<Document> collection,
                          Document queryContent, Method method, Object[] args) throws Exception {
        DeleteResult result = collection.deleteOne(queryContent);
        Class<?> returnType = method.getReturnType();
        if (returnType == boolean.class || returnType == Boolean.class) {
            return result.getDeletedCount() > 0;
        }
        return result.getDeletedCount();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        executorProxy.registerExecutor("deleteOne", this);
    }
}
