package com.github.eacryo.mongoflex.strategy;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Component
public class ExecutorProxy {
    private final Map<String, CommandExecutor> executors = new HashMap<>();

    public Object execute(String command, MongoCollection<Document> collection, Document queryContent, Method method, Object[] args) throws Exception {
        CommandExecutor executor = executors.get(command);
        if (executor == null) {
            throw new UnsupportedOperationException("Unsupported command: " + command);
        } else {
            return executor.execute(command, collection, queryContent, method, args);
        }
    }

    public void registerExecutor(String operation, CommandExecutor executor) {
        executors.put(operation, executor);
    }
}
