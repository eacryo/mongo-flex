package com.github.eacryo.mongoflex.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bson.Document;

/**
 * MongoDB Shell command parser — <b>deprecated</b>, use {@link JsonTemplateParser} instead.
 * <p>
 * MongoDB Shell 命令解析器——<b>已废弃</b>，请使用 {@link JsonTemplateParser} 替代。
 *
 * @deprecated since 2.0 — replaced by {@link JsonTemplateParser} which works with pure JSON filter
 *             templates and {@link Find}/{@link Count}/{@link Delete} annotations.
 */
@Deprecated
public class QueryParser {
    //这里只解析单引号'不解析双引号"
    private final Pattern commandPattern = Pattern.compile("^db\\.getCollection\\('(.*?)'\\)\\." +
            "(find|findOne|insertOne|updateOne|updateMany|deleteOne|deleteMany|count|aggregate)\\((.*?)\\)(.*)$");

    private static final Pattern SKIP_PATTERN = Pattern.compile("\\.skip\\((\\d+)\\)");
    private static final Pattern LIMIT_PATTERN = Pattern.compile("\\.limit\\((\\d+)\\)");

    public QueryCommand parse(String shellCommand) {
        Matcher matcher = commandPattern.matcher(shellCommand);
        if (matcher.find()) {
            String collectionName = matcher.group(1);
            String command = matcher.group(2); // "find" or "findOne"
            String argsString = matcher.group(3); // "{}" or "{ 'name': 'value' }" or "{a:1},{b:2}"
            String chainSuffix = matcher.group(4); // optional: ".skip(20).limit(10)"

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

            // 解析链式调用中的 skip/limit
            Integer skip = null;
            Integer limit = null;
            if (chainSuffix != null && !chainSuffix.isEmpty()) {
                skip = parseChainInt(chainSuffix, SKIP_PATTERN);
                limit = parseChainInt(chainSuffix, LIMIT_PATTERN);
            }

            return new QueryCommand(collectionName, command, arguments, skip, limit);
        }
        throw new IllegalArgumentException("Invalid MongoDB shell command format: " + shellCommand +
                ", expected format: db.getCollection('collectionName').find({'filed':'value'})");
    }

    /**
     * 从链式后缀中提取数值，如 ".skip(20)" → 20，未匹配返回 null。
     */
    private static Integer parseChainInt(String chainSuffix, Pattern pattern) {
        Matcher m = pattern.matcher(chainSuffix);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return null;
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
        /** 分页跳过条数，null 表示未指定 */
        public final Integer skip;
        /** 分页限制条数，null 表示未指定 */
        public final Integer limit;

        public QueryCommand(String collectionName, String operation, List<Document> arguments,
                            Integer skip, Integer limit) {
            this.collectionName = collectionName;
            this.operation = operation;
            this.arguments = arguments;
            this.skip = skip;
            this.limit = limit;
        }
    }
}
