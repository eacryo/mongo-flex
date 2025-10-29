package com.github.eacryo.mongoflex.strategy;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
public class CountExecutor implements  CommandExecutor {

    @Autowired
    private ExecutorProxy executorProxy;

    @Override
    public Object execute(String command, MongoCollection<Document> collection,Document queryContent,Method method, Object[] args) throws Exception {
        // 新增 count 逻辑
        Long count = collection.countDocuments(queryContent);
        // 返回类型可以是 long 或 Long
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
