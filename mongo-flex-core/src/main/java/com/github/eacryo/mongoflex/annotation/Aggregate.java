package com.github.eacryo.mongoflex.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MongoDB aggregation pipeline annotation / MongoDB 聚合管道注解
 * <p>
 * Declares an aggregation pipeline on a repository method. The value is a JSON array
 * of pipeline stages with {@code #{paramName}} placeholders.
 * <p>
 * 在 Repository 方法上声明聚合管道，value 为 JSON 数组格式的 pipeline stages，
 * 支持 {@code #{paramName}} 占位符。
 *
 * <pre>{@code
 * @Aggregate("[{$match: {status: #{status}}}, {$lookup: {from: 'customers', localField: 'customerId', foreignField: '_id', as: 'customer'}}]")
 * List<OrderWithCustomer> findOrdersWithCustomer(@Param("status") String status);
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Aggregate {
    /** MongoDB aggregation pipeline JSON array with #{param} placeholders / MongoDB 聚合管道 JSON 数组，支持 #{param} 占位符 */
    String value();
}
