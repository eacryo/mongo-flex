package com.github.eacryo.mongoflex.strategy;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExecutorProxy {
    private final Map<String, CommandExecutor> executors = new HashMap<>();

    public Object execute(String command, MongoCollection<Document> collection, List<Document> arguments,
                           Integer skip, Integer limit, Method method, Object[] args) throws Exception {
        CommandExecutor executor = executors.get(command);
        if (executor == null) {
            throw new UnsupportedOperationException("Unsupported command: " + command);
        } else {
            return executor.execute(command, collection, arguments, skip, limit, method, args);
        }
    }

    public void registerExecutor(String operation, CommandExecutor executor) {
        executors.put(operation, executor);
    }
}
