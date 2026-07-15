package com.github.eacryo.mongoflex.repository;

import org.bson.Document;
import com.github.eacryo.mongoflex.annotation.Param;

import java.lang.reflect.Method;
import com.github.eacryo.mongoflex.annotation.Param;
import java.lang.reflect.Parameter;
import com.github.eacryo.mongoflex.annotation.Param;

/**
 * JSON template parser — replaces {@code #{paramName}} placeholders with JSON-encoded
 * parameter values and parses the result as a MongoDB {@link Document} filter.
 * <p>
 * JSON 模板解析器——将 {@code #{paramName}} 占位符替换为 JSON 编码后的参数值，
 * 并将结果解析为 MongoDB {@link Document} 过滤器。
 * <p>
 * <b>Template syntax:</b> MongoDB shell-style JSON with bare {@code #{param}} placeholders
 * (no surrounding quotes — the parser adds type-appropriate JSON encoding).
 * <pre>{@code
 * @Find("{name: #{name}, level: #{level}}")
 * // with name="Hu Tao", level=90 → {name: "Hu Tao", level: 90}
 * }</pre>
 * <p>
 * Replaces the old shell-command-based QueryParser (removed).
 */
public class JsonTemplateParser {

    /**
     * Parse a JSON template with method parameters and return a Document filter /
     * 使用方法参数解析 JSON 模板并返回 Document 过滤器
     *
     * @param template JSON string with #{param} placeholders / 带 #{param} 占位符的 JSON 字符串
     * @param method   the annotated method / 被注解的方法
     * @param args     method parameter values / 方法参数值
     * @return parsed MongoDB Document filter / 解析后的 MongoDB Document 过滤器
     */
    public Document parse(String template, Method method, Object[] args) {
        String json = template;
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Param paramAnnotation = parameters[i].getAnnotation(Param.class);
            if (paramAnnotation != null) {
                String paramName = paramAnnotation.value();
                String placeholder = "#{" + paramName + "}";
                json = json.replace(placeholder, toJsonValue(args[i]));
            }
        }
        return Document.parse(json);
    }

    /**
     * Convert a Java value to its JSON representation / 将 Java 值转换为 JSON 表示
     * <p>
     * Returns standard JSON literals: {@code null}, numbers/booleans as-is,
     * strings double-quoted with proper escaping.
     *
     * @param value the Java value / Java 值
     * @return JSON literal string / JSON 字面量字符串
     */
    static String toJsonValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        // String or fallback: double-quote with escaping / 字符串或兜底：双引号包围并转义
        String s = value instanceof String ? (String) value : value.toString();
        return "\"" + escapeJson(s) + "\"";
    }

    /**
     * Escape special characters for a JSON double-quoted string /
     * 转义 JSON 双引号字符串中的特殊字符
     */
    private static String escapeJson(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b");  break;
                case '\f': sb.append("\\f");  break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:   sb.append(c);      break;
            }
        }
        return sb.toString();
    }
}
