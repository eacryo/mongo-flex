package com.github.eacryo.mongoflex.v2;


import com.github.eacryo.mongoflex.entity.PageDTO;
import com.github.eacryo.mongoflex.lambda.LambdaQueryWrapper;
import com.github.eacryo.mongoflex.util.SFunction;

import java.util.List;

/**
 * Repository interface with LambdaQueryWrapper support.
 * <p>
 * <b>null 参数：</b>所有方法的参数都不接受 null，传入 null 会抛出 {@code IllegalArgumentException}。
 * <p>
 * <b>写操作（delete/update）：</b>传入空实体或空条件的 wrapper 会抛出异常，
 * 全量操作请使用对应的 {@code xxxAll()} 方法。
 * <p>
 * <b>读操作（find/count）：</b>空实体或空条件视为无条件查询，返回全量结果。
 */
public interface MongoRepository<T, ID> {

    /**
     * 插入一条文档，entity 不可为 null。
     */
    T insert(T entity);

    /**
     * 按 ID 查询，id 不可为 null。
     */
    T findById(ID id);

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
     */
    T findOne(LambdaQueryWrapper<T> wrapper);

    /**
     * 按 LambdaQueryWrapper 条件查询列表，wrapper 不可为 null。
     */
    List<T> findList(LambdaQueryWrapper<T> wrapper);

    /**
     * 查询全集合所有文档。
     */
    List<T> findAll();

    /**
     * 按 LambdaQueryWrapper 条件分页查询。
     * 回填 pageDTO 的 total/totalPage/records 后返回同一对象。
     * wrapper 不可为 null，空条件视为无条件分页。
     */
    PageDTO<T> findPage(LambdaQueryWrapper<T> wrapper, PageDTO<T> pageDTO);

    /**
     * 按实体中的非空字段作为条件分页查询。
     * null 字段会被忽略，entity 和 pageDTO 不可为 null。
     * 若所有字段均为 null 则视为无条件分页。
     */
    PageDTO<T> findPageByEntity(T entity, PageDTO<T> pageDTO);

    /**
     * 按 LambdaQueryWrapper 条件统计，wrapper 不可为 null。
     */
    long count(LambdaQueryWrapper<T> wrapper);

    /**
     * 统计集合总文档数。
     */
    long count();

    /**
     * 按实体中的非空字段作为条件统计，entity 不可为 null。
     * null 字段会被忽略。
     */
    long countByEntity(T entity);

    /**
     * 按单个字段的值统计，field 不可为 null。
     */
    <R> long count(SFunction<T, R> field, R value);

    /**
     * 按 ID 更新，entity 不可为 null，且必须包含非 null 的 _id。
     */
    long updateById(T entity);

    /**
     * 按单个字段的值作为条件更新，field 和 entity 不可为 null。
     */
    <R> long update(SFunction<T, R> field, R value, T entity);

    /**
     * 按 LambdaQueryWrapper 条件更新，wrapper 和 entity 不可为 null。
     * wrapper 条件不能为空，全量更新请使用 {@link #updateAll(Object)}。
     */
    long update(LambdaQueryWrapper<T> wrapper, T entity);

    /**
     * 更新全集合所有文档，entity 不可为 null。
     */
    long updateAll(T entity);

    /**
     * 按 ID upsert：如果 _id 匹配则更新，否则插入一条新文档。entity 不可为 null，且必须包含非 null 的 _id。
     */
    long upsertById(T entity);

    /**
     * 按单个字段的值作为条件 upsert，field 和 entity 不可为 null。
     */
    <R> long upsert(SFunction<T, R> field, R value, T entity);

    /**
     * 按 LambdaQueryWrapper 条件 upsert，wrapper 和 entity 不可为 null。
     * wrapper 条件不能为空，全量 upsert 请使用 {@link #upsertAll(Object)}（如果有的话）。
     */
    long upsert(LambdaQueryWrapper<T> wrapper, T entity);

    /**
     * 按 ID 删除，id 不可为 null。
     */
    long deleteById(ID id);

    /**
     * 按实体中的非空字段作为条件删除，entity 不可为 null。
     * null 字段会被忽略。如果所有字段都为 null 请使用 {@link #deleteAll()}。
     */
    long deleteByEntity(T entity);

    /**
     * 按单个字段的值删除，field 不可为 null。
     */
    <R> long delete(SFunction<T, R> field, R value);

    /**
     * 按 LambdaQueryWrapper 条件删除，wrapper 不可为 null。
     * wrapper 条件不能为空，全量删除请使用 {@link #deleteAll()}。
     */
    long delete(LambdaQueryWrapper<T> wrapper);

    /**
     * 删除全集合所有文档。
     */
    long deleteAll();
}

