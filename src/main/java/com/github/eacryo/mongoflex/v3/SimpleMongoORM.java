package com.github.eacryo.mongoflex.v3;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.data.util.Pair;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ORM核心执行器，提供安全的查询方法
 */
public class SimpleMongoORM {

    private final MongoDatabase database;
    private static final Pattern PARAM_PATTERN = Pattern.compile("#\\{([a-zA-Z0-9_]+)\\}");

    public SimpleMongoORM(MongoDatabase database) {
        this.database = database;
    }

    public <S,T> List<Document> findList(String command, Pair<S,T>... params) {
        // 1. 解析查询模板，找到集合和查询部分
        Matcher matcher = Pattern.compile("db\\.getCollection\\('(.+)'\\)\\.find\\((.+)\\)").matcher(command);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid query template format.");
        }
        String collectionName = matcher.group(1);
        String findTemplate = matcher.group(2);

        // 2. 将参数安全地替换到查询中，防止注入
        String safeFindTemplate = replaceParameters(findTemplate, params);
        Document queryDoc = Document.parse(safeFindTemplate);

        // 3. 执行查询
        MongoCollection<Document> collection = database.getCollection(collectionName);
        return collection.find(queryDoc).into(new java.util.ArrayList<>());
    }

    /**
     * 核心安全替换方法，防止注入
     * @param template 原始查询字符串
     * @param params 参数数组
     * @return 替换后的安全查询字符串
     */
    private String replaceParameters(String template, Object[] params) {
        Matcher matcher = PARAM_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        int paramIndex = 0;

        while (matcher.find()) {
            if (paramIndex >= params.length) {
                throw new IllegalArgumentException("Parameter count mismatch.");
            }
            Object param = params[paramIndex++];
            String replacement = escapeAndQuote(param);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        if (paramIndex != params.length) {
            throw new IllegalArgumentException("Parameter count mismatch.");
        }

        return sb.toString();
    }

    /**
     * 根据参数类型进行转义和引用，这是防止注入的关键！
     */
    private String escapeAndQuote(Object param) {
        if (param instanceof String) {
            // 对字符串进行转义，确保特殊字符不会破坏JSON结构
            return "'" + escapeString((String) param) + "'";
        } else if (param instanceof Number || param instanceof Boolean) {
            // 数字和布尔值不需要引号
            return param.toString();
        }
        // TODO: 添加更多数据类型支持，例如 Date
        throw new UnsupportedOperationException("Unsupported parameter type: " + param.getClass().getName());
    }

    /**
     * 简单的字符串转义，防止引号和反斜杠注入
     */
    private String escapeString(String s) {
        return s.replace("'", "\\'").replace("\\", "\\\\");
    }
}
