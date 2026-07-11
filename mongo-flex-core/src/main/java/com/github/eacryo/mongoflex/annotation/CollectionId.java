package com.github.eacryo.mongoflex.annotation;

import com.github.eacryo.mongoflex.constant.IdType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记实体中的 ID 字段，对应 MongoDB 的 {@code _id}。
 * <p>
 * 默认使用 {@link IdType#NONE}（MongoDB 原生 ObjectId）。
 * 标注此注解的字段会强制映射到 {@code _id}，字段名不必是 {@code id}。
 *
 * <h3>示例</h3>
 * <pre>{@code
 * // 使用 MongoDB 原生 ObjectId（默认）
 * @CollectionId
 * private String id;
 *
 * // 使用 ULID
 * @CollectionId(IdType.ULID)
 * private String id;
 *
 * // 字段名可以不是 "id"
 * @CollectionId(IdType.ULID)
 * private String userId;
 * }</pre>
 *
 * @see IdType
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CollectionId {
    /** ID 生成策略，默认为 {@link IdType#NONE}。 */
    IdType value() default IdType.NONE;
}
