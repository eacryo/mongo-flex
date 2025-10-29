package com.github.eacryo.mongoflex.strategy;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.springframework.beans.factory.InitializingBean;

import java.lang.reflect.Method;

public interface CommandExecutor extends InitializingBean {
    Object execute(String command, MongoCollection<Document> collection,Document queryContent, Method method, Object[] args) throws Exception;

}
