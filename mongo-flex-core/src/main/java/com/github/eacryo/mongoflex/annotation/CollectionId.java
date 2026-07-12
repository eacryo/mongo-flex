package com.github.eacryo.mongoflex.annotation;

import com.github.eacryo.mongoflex.config.IdGenerator;
import com.github.eacryo.mongoflex.constant.IdType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark the ID field of an entity, mapped to MongoDB {@code _id}. / 标记实体中的 ID 字段，对应 MongoDB 的 {@code _id}。
 * <p>
 * Default uses {@link IdType#OBJECT_ID} (MongoDB native ObjectId). / 默认使用 {@link IdType#OBJECT_ID}（MongoDB 原生 ObjectId）。
 * Fields annotated with this annotation are forced to map to {@code _id}, the field name does not need to be {@code id}. / 标注此注解的字段会强制映射到 {@code _id}，字段名不必是 {@code id}。
 *
 * <h3>Examples / 示例</h3>
 * <pre>{@code
 * // MongoDB native ObjectId (default) / MongoDB 原生 ObjectId（默认）
 * @CollectionId
 * private String id;
 *
 * // ULID auto-generation / ULID 自动生成
 * @CollectionId(IdType.ULID)
 * private String id;
 *
 * // Custom ID generator / 自定义 ID 生成器
 * @CollectionId(value = IdType.INPUT, generatorClass = SnowflakeGenerator.class)
 * private String id;
 *
 * // Field name can be anything / 字段名可以是任意名称
 * @CollectionId(IdType.ULID)
 * private String userId;
 * }</pre>
 *
 * @see IdType
 * @see IdGenerator
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CollectionId {
    /** ID generation strategy, default {@link IdType#OBJECT_ID}. / ID 生成策略，默认为 {@link IdType#OBJECT_ID}。 */
    IdType value() default IdType.OBJECT_ID;

    /**
     * Custom ID generator class, only effective when {@link #value()} is {@link IdType#INPUT}. / 自定义 ID 生成器类，仅在 {@link #value()} 为 {@link IdType#INPUT} 时生效。
     * <p>
     * The class must implement {@link IdGenerator} and have a no-arg constructor. / 该类必须实现 {@link IdGenerator} 并具有无参构造函数。
     * If not specified (defaults to {@link IdGenerator.None}), falls back to the globally injected {@code IdGenerator} bean. / 如果未指定（默认为 {@link IdGenerator.None}），则回退到全局注入的 {@code IdGenerator} bean。
     */
    Class<? extends IdGenerator> generatorClass() default IdGenerator.None.class;
}
