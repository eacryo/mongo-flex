package com.github.eacryo.mongoflex.query;

import com.github.eacryo.mongoflex.convertor.MongoMappingConvertor;
import org.bson.conversions.Bson;

/**
 * Unified query abstraction — all query paths (Lambda, MQL, Entity Example)
 * produce a MongoDB {@link Bson} filter through this interface /
 * 统一的查询条件抽象——所有查询路径（Lambda、MQL、Entity Example）
 * 最终通过此接口产出 MongoDB {@link Bson} 过滤器。
 *
 * @param <T> entity type / 实体类型
 */
public interface QuerySpec<T> {

    /**
     * Render this query specification into a MongoDB Bson filter /
     * 将此查询规范渲染为 MongoDB Bson 过滤器
     *
     * @param convertor field-name mapping converter / 字段名映射转换器
     * @return MongoDB Bson filter / MongoDB Bson 过滤器
     */
    Bson toBson(MongoMappingConvertor convertor);

    /**
     * Return the target entity class for field-name resolution and result mapping /
     * 返回目标实体类型，用于字段名解析和结果映射
     *
     * @return entity class, or {@code null} if not specified / 实体类型，未指定时返回 null
     */
    Class<T> getEntityClass();
}
