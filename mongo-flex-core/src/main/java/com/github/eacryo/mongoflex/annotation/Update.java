package com.github.eacryo.mongoflex.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MongoDB update annotation with JSON filter and update templates / MongoDB 更新注解，使用 JSON 过滤模板和更新模板
 * <p>
 * Executes {@code updateOne(filter, update)} or {@code updateMany(filter, update)} against the collection.
 * The return type must be {@code long} or {@code void}.
 * <p>
 * 对集合执行 {@code updateOne(filter, update)} 或 {@code updateMany(filter, update)}，
 * 返回类型必须为 {@code long} 或 {@code void}。
 *
 * <pre>{@code
 * @Update(value = "{'name': '#{name}'}", update = "{$set: {level: #{level}}}")
 * long updateLevelByName(@Param("name") String name, @Param("level") int level);
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Update {
    /** MongoDB filter JSON with #{param} placeholders / MongoDB 过滤 JSON，支持 #{param} 占位符 */
    String value();
    /** MongoDB update document JSON with #{param} placeholders / MongoDB 更新文档 JSON，支持 #{param} 占位符 */
    String update();
    /** If true, insert a new document when no document matches the filter / 如果为 true，当没有文档匹配过滤条件时插入新文档 */
    boolean upsert() default false;
    /** If true, update all matching documents; if false, update only the first / 如果为 true，更新所有匹配的文档；如果为 false，仅更新第一个 */
    boolean multi() default false;
}
