package com.github.eacryo.mongoflex.repository;


import com.github.eacryo.mongoflex.entity.PageDTO;
import com.github.eacryo.mongoflex.query.LambdaQueryWrapper;
import com.github.eacryo.mongoflex.query.QuerySpec;
import com.github.eacryo.mongoflex.util.SFunction;

import java.util.List;

/**
 * Full-featured repository interface — extends {@link QueryRepository} with
 * convenience methods for entity-based, field-based, and LambdaQueryWrapper queries /
 * 完整功能 Repository 接口——继承 {@link QueryRepository}，
 * 额外提供基于实体、字段和 LambdaQueryWrapper 的便捷查询方法。
 * <p>
 * Interface hierarchy / 接口层级:
 * <pre>{@code
 * CrudRepository<T,ID>     — insert, findById, findAll, count, deleteOneById, deleteAll
 *   └─ QueryRepository<T,ID>  — findOne/findList/findPage/count/update/delete by QuerySpec
 *        └─ MongoRepository<T,ID> — convenience methods (SFunction, entity, LambdaQueryWrapper)
 * }</pre>
 * <p>
 * <b>null 参数：</b>所有方法的参数都不接受 null，传入 null 会抛出 {@code IllegalArgumentException}。
 * <p>
 * <b>写操作（delete/update）：</b>传入空实体或空条件的 wrapper 会抛出异常，
 * 全量操作请使用对应的 {@code xxxAll()} 方法。
 * <p>
 * <b>读操作（find/count）：</b>空实体或空条件视为无条件查询，返回全量结果。
 */
public interface MongoRepository<T, ID> extends QueryRepository<T, ID> {

    /**
     * 按实体中的非空字段作为条件查询，返回第一条匹配记录。
     * null 字段会被忽略，entity 不可为 null。
     */
    T findOneByEntity(T entity);

    /**
     * 按实体中的非空字段作为条件查询，返回所有匹配记录的列表。
     * null 字段会被忽略，entity 不可为 null。若所有字段均为 null 则返回全集合。
     */
    List<T> findListByEntity(T entity);

    /**
     * 按单个字段的值查询，field 不可为 null。
     */
    <R> T findOne(SFunction<T, R> field, R value);

    /**
     * 按 LambdaQueryWrapper 条件查询，wrapper 不可为 null。
     * @deprecated use {@link #findOne(QuerySpec)} instead / 请使用 {@link #findOne(QuerySpec)}
     */
    @Deprecated
    T findOne(LambdaQueryWrapper<T> wrapper);

    /**
     * 按 LambdaQueryWrapper 条件查询列表，wrapper 不可为 null。
     * @deprecated use {@link #findList(QuerySpec)} instead / 请使用 {@link #findList(QuerySpec)}
     */
    @Deprecated
    List<T> findList(LambdaQueryWrapper<T> wrapper);

    /**
     * 按 LambdaQueryWrapper 条件分页查询。
     * 回填 pageDTO 的 total/records（totalPage 为计算属性，自动同步）后返回同一对象。
     * wrapper 不可为 null，空条件视为无条件分页。
     * @deprecated use {@link #findPage(QuerySpec, PageDTO)} instead / 请使用 {@link #findPage(QuerySpec, PageDTO)}
     */
    @Deprecated
    PageDTO<T> findPage(LambdaQueryWrapper<T> wrapper, PageDTO<T> pageDTO);

    /**
     * 按实体中的非空字段作为条件分页查询。
     * null 字段会被忽略，entity 和 pageDTO 不可为 null。
     * 若所有字段均为 null 则视为无条件分页。
     */
    PageDTO<T> findPageByEntity(T entity, PageDTO<T> pageDTO);

    /**
     * 按 LambdaQueryWrapper 条件统计，wrapper 不可为 null。
     * @deprecated use {@link #count(QuerySpec)} instead / 请使用 {@link #count(QuerySpec)}
     */
    @Deprecated
    long count(LambdaQueryWrapper<T> wrapper);

    /**
     * 按实体中的非空字段作为条件统计，entity 不可为 null。
     * null 字段会被忽略。
     */
    long countByEntity(T entity);

    /**
     * 按单个字段的值统计，field 不可为 null。
     */
    <R> long count(SFunction<T, R> field, R value);

    // ──── updateOne / 更新单条 ────

    /** 按 ID 更新单条，entity 不可为 null 且必须包含非 null 的 _id。 */
    long updateOneById(T entity);
    /** 按 ID 更新单条，可选 upsert（匹配不到时插入）。 */
    long updateOneById(T entity, boolean upsert);

    /** 按字段值更新第一条匹配文档，field 和 entity 不可为 null。 */
    <R> long updateOne(SFunction<T, R> field, R value, T entity);
    /** 按字段值更新第一条匹配文档，可选 upsert。 */
    <R> long updateOne(SFunction<T, R> field, R value, T entity, boolean upsert);

    /** 按 LambdaQueryWrapper 条件更新第一条匹配文档，wrapper 和 entity 不可为 null。 */
    @Deprecated
    long updateOne(LambdaQueryWrapper<T> wrapper, T entity);
    /** 按 LambdaQueryWrapper 条件更新第一条匹配文档，可选 upsert。 */
    @Deprecated
    long updateOne(LambdaQueryWrapper<T> wrapper, T entity, boolean upsert);

    // ──── updateMany / 更新多条 ────

    /** 按字段值更新所有匹配文档，field 和 entity 不可为 null。 */
    <R> long updateMany(SFunction<T, R> field, R value, T entity);
    /** 按字段值更新所有匹配文档，可选 upsert。 */
    <R> long updateMany(SFunction<T, R> field, R value, T entity, boolean upsert);

    /** 按 LambdaQueryWrapper 条件更新所有匹配文档，wrapper 和 entity 不可为 null。 */
    @Deprecated
    long updateMany(LambdaQueryWrapper<T> wrapper, T entity);
    /** 按 LambdaQueryWrapper 条件更新所有匹配文档，可选 upsert。 */
    @Deprecated
    long updateMany(LambdaQueryWrapper<T> wrapper, T entity, boolean upsert);

    // ──── deleteOne / 删除单条 ────

    /** 按字段值删除第一条匹配文档，field 不可为 null。 */
    <R> long deleteOne(SFunction<T, R> field, R value);
    /** 按 LambdaQueryWrapper 条件删除第一条匹配文档，wrapper 不可为 null。 */
    @Deprecated
    long deleteOne(LambdaQueryWrapper<T> wrapper);

    // ──── deleteMany / 删除多条 ────

    /** 按实体中的非空字段作为条件删除，entity 不可为 null。null 字段会被忽略。 */
    long deleteByEntity(T entity);
    /** 按字段值删除所有匹配文档，field 不可为 null。 */
    <R> long deleteMany(SFunction<T, R> field, R value);
    /** 按 LambdaQueryWrapper 条件删除所有匹配文档，wrapper 不可为 null。 */
    @Deprecated
    long deleteMany(LambdaQueryWrapper<T> wrapper);
}

