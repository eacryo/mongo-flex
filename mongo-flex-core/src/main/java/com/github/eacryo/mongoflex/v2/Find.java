package com.github.eacryo.mongoflex.v2;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MongoDB find query annotation with JSON filter template / MongoDB 查询注解，使用 JSON 过滤模板
 * <p>
 * The value is a MongoDB filter JSON string with {@code #{paramName}} placeholders
 * which are replaced by JSON-encoded parameter values at runtime.
 * <p>
 * value 是 MongoDB 过滤 JSON 字符串，支持 {@code #{paramName}} 占位符，
 * 运行时替换为 JSON 编码后的参数值。
 *
 * <pre>{@code
 * @Find("{name: #{name}, level: {$gte: #{minLevel}}}")
 * List<Character> findByNameAndMinLevel(@Param("name") String name, @Param("minLevel") int level);
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Find {
    /** MongoDB filter JSON with #{param} placeholders / MongoDB 过滤 JSON，支持 #{param} 占位符 */
    String value();
    /** Documents to skip, 0 = no skip / 跳过的文档数，0 表示不跳过 */
    long skip() default 0;
    /** Max documents to return, 0 = no limit / 返回的最大文档数，0 表示不限制 */
    long limit() default 0;
}
