package com.github.eacryo.mongoflex.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MongoDB count query annotation with JSON filter template / MongoDB 统计注解，使用 JSON 过滤模板
 * <p>
 * Executes {@code countDocuments(filter)} against the collection. The return type
 * must be {@code long} or {@code int}.
 * <p>
 * 对集合执行 {@code countDocuments(filter)}，返回类型必须为 {@code long} 或 {@code int}。
 *
 * <pre>{@code
 * @Count("{'vision': '#{vision}'}")
 * long countByVision(@Param("vision") String vision);
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Count {
    /** MongoDB filter JSON with #{param} placeholders / MongoDB 过滤 JSON，支持 #{param} 占位符 */
    String value();
}
