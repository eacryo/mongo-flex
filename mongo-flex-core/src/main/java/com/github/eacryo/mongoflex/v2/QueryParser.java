package com.github.eacryo.mongoflex.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bson.Document;

public class QueryParser {
    //这里只解析单引号'不解析双引号"
    private final Pattern commandPattern = Pattern.compile("^db\\.getCollection\\('(.*?)'\\)\\." +
            "(find|findOne|insertOne|updateOne|updateMany|deleteOne|deleteMany|count|aggregate)\\((.*?)\\)$");

    public QueryCommand parse(String shellCommand) {
        Matcher matcher = commandPattern.matcher(shellCommand);
        if (matcher.find()) {
            String collectionName = matcher.group(1);
            String command = matcher.group(2); // "find" or "findOne"
            String argsString = matcher.group(3); // "{}" or "{ 'name': 'value' }" or "{a:1},{b:2}"

            List<Document> arguments = new ArrayList<>();
            if (argsString == null || argsString.trim().isEmpty()) {
                arguments.add(new Document());
            } else {
                List<String> segments = splitTopLevelArguments(argsString);
                for (String segment : segments) {
                    String trimmed = segment.trim();
                    if (trimmed.isEmpty()) {
                        arguments.add(new Document());
                    } else {
                        arguments.add(Document.parse(trimmed));
                    }
                }
            }

            return new QueryCommand(collectionName, command, arguments);
        }
        throw new IllegalArgumentException("Invalid MongoDB shell command format: " + shellCommand +
                ", expected format: db.getCollection('collectionName').find({'filed':'value'})");
    }

    /**
     * 按顶层逗号分割参数字符串，正确处理嵌套的 {}、[] 和引号内的逗号。
     * 例如 "{a:1}, {$set: {b: 2}}, {upsert: true}" 分割为三个部分。
     */
    static List<String> splitTopLevelArguments(String argsString) {
        List<String> result = new ArrayList<>();
        if (argsString == null || argsString.isEmpty()) {
            return result;
        }

        int braceDepth = 0;
        int bracketDepth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        int lastSplit = 0;

        for (int i = 0; i < argsString.length(); i++) {
            char c = argsString.charAt(i);

            // 反斜杠转义：在引号内跳过下一个字符
            if (c == '\\' && (inSingleQuote || inDoubleQuote)) {
                i++;
                continue;
            }

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (!inSingleQuote && !inDoubleQuote) {
                if (c == '{') {
                    braceDepth++;
                } else if (c == '}') {
                    braceDepth--;
                } else if (c == '[') {
                    bracketDepth++;
                } else if (c == ']') {
                    bracketDepth--;
                } else if (c == ',' && braceDepth == 0 && bracketDepth == 0) {
                    result.add(argsString.substring(lastSplit, i));
                    lastSplit = i + 1;
                }
            }
        }

        // 添加最后一段
        result.add(argsString.substring(lastSplit));
        return result;
    }

    // 内部类用于封装解析结果
    public static class QueryCommand {
        public final String collectionName;
        public final String operation;
        public final List<Document> arguments;

        public QueryCommand(String collectionName, String operation, List<Document> arguments) {
            this.collectionName = collectionName;
            this.operation = operation;
            this.arguments = arguments;
        }
    }
}
