package com.github.eacryo.mongoflex.v2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bson.Document;

public class QueryParser {
    //这里只解析单引号'不解析双引号"
    private final Pattern commandPattern = Pattern.compile("^db\\.getCollection\\('(.*?)'\\)\\.(find|findOne)\\((.*?)\\)$");

    public QueryCommand parse(String shellCommand) {
        Matcher matcher = commandPattern.matcher(shellCommand);
        if (matcher.find()) {
            String collectionName = matcher.group(1);
            String command = matcher.group(2); // "find" or "findOne"
            String queryJson = matcher.group(3); // "{}" or "{ 'name': 'value' }"

            // 处理空查询文档，例如 "db.getCollection('x').find()"
            if (queryJson == null || queryJson.trim().isEmpty()) {
                queryJson = "{}";
            }

            // 这里只是简单的字符串处理，如果查询参数是 ?0 等占位符，需要在这里处理
            Document queryDoc = Document.parse(queryJson);

            return new QueryCommand(collectionName, command, queryDoc);
        }
        throw new IllegalArgumentException("Invalid MongoDB shell command format: " + shellCommand +
                ", expected format: db.getCollection('collectionName').find({'filed':'value'})");
    }

    // 内部类用于封装解析结果
    public static class QueryCommand {
        public final String collectionName;
        public final String command;
        public final Document queryDoc;

        public QueryCommand(String collectionName, String command, Document queryDoc) {
            this.collectionName = collectionName;
            this.command = command;
            this.queryDoc = queryDoc;
        }
    }
}
