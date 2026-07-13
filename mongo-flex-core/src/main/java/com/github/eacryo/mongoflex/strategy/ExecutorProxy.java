package com.github.eacryo.mongoflex.strategy;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command executor registry — <b>deprecated</b>, use annotation-driven dispatch instead.
 * <p>
 * 命令执行器注册表——<b>已废弃</b>，请使用注解驱动调度替代。
 *
 * @deprecated since 2.0 — replaced by annotation-driven dispatch in
 *             {@link com.github.eacryo.mongoflex.v2.MyRepositoryProxyHandler}
 *             using {@link com.github.eacryo.mongoflex.v2.Find}/{@link com.github.eacryo.mongoflex.v2.Count}/{@link com.github.eacryo.mongoflex.v2.Delete}.
 */
@Deprecated
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
