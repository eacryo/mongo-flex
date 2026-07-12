package com.github.eacryo.mongoflex.util;

import com.github.eacryo.mongoflex.config.DateValueProvider;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Date/time value generator for {@code @CreateDate} / {@code @UpdateDate} fields. / 日期/时间值生成器，用于 {@code @CreateDate} / {@code @UpdateDate} 字段。
 * <p>
 * Resolution order / 解析优先级：
 * <ol>
 *   <li>User-provided {@link DateValueProvider} (if configured) / 用户提供的 {@link DateValueProvider}（如有配置）</li>
 *   <li>Built-in type table / 内置类型表</li>
 *   <li>Throw {@link IllegalArgumentException} / 抛出 {@link IllegalArgumentException}</li>
 * </ol>
 * <p>
 * Built-in supported types / 内置支持的类型：
 * <table>
 *   <tr><th>Type / 类型</th><th>Generated Value / 生成值</th></tr>
 *   <tr><td>{@code java.util.Date}</td><td>{@code new Date()}</td></tr>
 *   <tr><td>{@code String}</td><td>{@code LocalDateTime.now().format(pattern)}</td></tr>
 *   <tr><td>{@code LocalDateTime}</td><td>{@code LocalDateTime.now()}</td></tr>
 *   <tr><td>{@code LocalDate}</td><td>{@code LocalDate.now()}</td></tr>
 *   <tr><td>{@code Instant}</td><td>{@code Instant.now()}</td></tr>
 *   <tr><td>{@code Long} / {@code long}</td><td>{@code System.currentTimeMillis()}</td></tr>
 * </table>
 *
 * @see DateValueProvider
 * @see com.github.eacryo.mongoflex.annotation.CreateDate
 * @see com.github.eacryo.mongoflex.annotation.UpdateDate
 */
public class DateValueGenerator {
    private static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final ConcurrentHashMap<String, DateTimeFormatter> FORMATTER_CACHE = new ConcurrentHashMap<>();

    private static DateTimeFormatter getFormatter(String pattern) {
        return FORMATTER_CACHE.computeIfAbsent(pattern, DateTimeFormatter::ofPattern);
    }

    /**
     * Generate a current date/time value using only the built-in type table. / 仅使用内置类型表生成当前日期/时间值。
     */
    public static Object generateCurrentDate(Class<?> fieldType) {
        return generateCurrentDate(fieldType, DEFAULT_PATTERN, null);
    }

    /**
     * Generate a current date/time value using only the built-in type table with a pattern. / 仅使用内置类型表生成当前日期/时间值（带格式）。
     */
    public static Object generateCurrentDate(Class<?> fieldType, String pattern) {
        return generateCurrentDate(fieldType, pattern, null);
    }

    /**
     * Generate a current date/time value with optional user provider. / 生成当前日期/时间值，支持可选的用户提供器。
     * <p>
     * Resolution order / 解析优先级：
     * <ol>
     *   <li>If {@code provider} is non-null, try {@code provider.generateCurrentDate(fieldType, pattern)} — return if non-null / 如果 provider 非空，先尝试 provider.generateCurrentDate()，非空则返回</li>
     *   <li>Fall through to built-in type table / 回退到内置类型表</li>
     *   <li>Throw {@link IllegalArgumentException} if type is unsupported / 类型不支持时抛出异常</li>
     * </ol>
     *
     * @param fieldType the Java type of the annotated field / 被注解字段的 Java 类型
     * @param pattern   the date format pattern (used by String type and custom providers) / 日期格式（String 类型和自定义 provider 使用）
     * @param provider  optional user-provided date value provider, may be null / 可选的用户日期值提供器，可为 null
     * @return the generated date/time value / 生成的日期/时间值
     * @throws IllegalArgumentException if the type is not supported by either the provider or the built-in table / 如果提供器和内置表都不支持该类型
     */
    public static Object generateCurrentDate(Class<?> fieldType, String pattern, DateValueProvider provider) {
        // 1. Try user provider first / 先尝试用户提供器
        if (provider != null) {
            Object result = provider.generateCurrentDate(fieldType, pattern);
            if (result != null) {
                return result;
            }
        }

        // 2. Built-in type table / 内置类型表
        if (fieldType == Date.class) {
            return new Date();
        }
        if (fieldType == String.class) {
            DateTimeFormatter formatter = getFormatter(pattern);
            return LocalDateTime.now().format(formatter);
        }
        if (fieldType == LocalDateTime.class) {
            return LocalDateTime.now();
        }
        if (fieldType == LocalDate.class) {
            return LocalDate.now();
        }
        if (fieldType == Instant.class) {
            return Instant.now();
        }
        if (fieldType == Long.class || fieldType == long.class) {
            return System.currentTimeMillis();
        }

        // 3. Unsupported type / 不支持的类型
        throw new IllegalArgumentException(
                "@CreateDate/@UpdateDate unsupported type: " + fieldType.getName()
                + ". Supported types: Date, String, LocalDateTime, LocalDate, Instant, Long. "
                + "Or implement DateValueProvider for custom types. / "
                + "不支持的字段类型: " + fieldType.getName()
                + "。支持的类型: Date, String, LocalDateTime, LocalDate, Instant, Long。"
                + "也可以实现 DateValueProvider 接口来支持自定义类型。");
    }
}
