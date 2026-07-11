package com.github.eacryo.mongoflex.strategy;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;
import java.util.List;

public class CountExecutor implements  CommandExecutor {

    @Autowired
    private ExecutorProxy executorProxy;

    @Override
    public Object execute(String command, MongoCollection<Document> collection, List<Document> arguments,
                           Integer skip, Integer limit, Method method, Object[] args) throws Exception {
        Document queryContent = arguments.get(0);
        Long count = collection.countDocuments(queryContent);
        // 支持 long / Long / int / Integer 四种返回类型
        if (method.getReturnType().equals(Long.class) || method.getReturnType().equals(long.class)) {
            return count;
        }
        if (method.getReturnType().equals(Integer.class) || method.getReturnType().equals(int.class)) {
            return count.intValue();
        }
        // 其他类型可根据需要扩展
        throw new IllegalArgumentException("Count return type must be long or Long or int or Integer");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        executorProxy.registerExecutor("count", this);
    }
}
