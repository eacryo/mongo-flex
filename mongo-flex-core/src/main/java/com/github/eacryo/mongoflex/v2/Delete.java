package com.github.eacryo.mongoflex.v2;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MongoDB delete query annotation with JSON filter template / MongoDB 删除注解，使用 JSON 过滤模板
 * <p>
 * Executes {@code deleteMany(filter)} against the collection. The return type
 * must be {@code long} or {@code void}.
 * <p>
 * 对集合执行 {@code deleteMany(filter)}，返回类型必须为 {@code long} 或 {@code void}。
 *
 * <pre>{@code
 * @Delete("{'name': '#{name}'}")
 * long deleteByName(@Param("name") String name);
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Delete {
    /** MongoDB filter JSON with #{param} placeholders / MongoDB 过滤 JSON，支持 #{param} 占位符 */
    String value();
}
