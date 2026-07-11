package com.github.eacryo.mongoflex.strategy;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.springframework.beans.factory.InitializingBean;

import java.lang.reflect.Method;
import java.util.List;

public interface CommandExecutor extends InitializingBean {
    Object execute(String command, MongoCollection<Document> collection, List<Document> arguments, Method method, Object[] args) throws Exception;

}
