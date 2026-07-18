package com.github.eacryo.mongoflex.repository;


import com.github.eacryo.mongoflex.entity.PageDTO;
import com.github.eacryo.mongoflex.query.LambdaQueryWrapper;
import com.github.eacryo.mongoflex.util.SFunction;

import java.util.List;

/**
 * MongoDB repository interface — the single entry point for all CRUD and query operations /
 * MongoDB Repository 接口——所有增删改查操作的唯一入口。
 * <p>
 * Query methods are organized by condition style:
 * <ul>
 *   <li><b>by entity</b> — conditions from non-null entity fields</li>
 *   <li><b>by lambda reference</b> — conditions from type-safe {@link SFunction} field + value</li>
 *   <li><b>by {@link LambdaQueryWrapper}</b> — dynamic conditions with type-safe chainable operators</li>
 * </ul>
 * <p>
 * <b>null 参数：</b>所有方法的参数都不接受 null，传入 null 会抛出 {@code IllegalArgumentException}。
 * <p>
 * <b>写操作（delete/update）：</b>传入空实体或空条件的 wrapper 会抛出异常，
 * 全量操作请使用对应的 {@code xxxAll()} 方法。
 * <p>
 * <b>读操作（find/count）：</b>空实体或空条件视为无条件查询，返回全量结果。
 */
public interface MongoRepository<T, ID> {

    // ──── Basic / 基础操作 ────

    /** Insert a single document / 插入一条文档 */
    T insert(T entity);

    /** Batch insert / 批量插入 */
    List<T> insertMany(List<T> entities);

    /** Find by ID / 按 ID 查询 */
    T findById(ID id);

    /** Find all documents / 查询全集合 */
    List<T> findAll();

    /** Count all documents / 统计全集合 */
    long count();

    /** Delete by ID / 按 ID 删除 */
    long deleteOneById(ID id);

    /** Delete all documents / 删除全集合 */
    long deleteAll();

    /** Update by ID / 按 ID 更新 */
    long updateOneById(T entity);

    /** Update by ID, optional upsert / 按 ID 更新，可选 upsert */
    long updateOneById(T entity, boolean upsert);

    // ──── by entity / 按实体条件 ────

    /** Find one by entity fields / 按实体字段查一条 */
    T findOneByEntity(T entity);

    /** Find list by entity fields / 按实体字段查列表 */
    List<T> findListByEntity(T entity);

    /** Paginated query by entity fields / 按实体字段分页查 */
    PageDTO<T> findPageByEntity(T entity, PageDTO<T> pageDTO);

    /** Count by entity fields / 按实体字段统计 */
    long countByEntity(T entity);

    /** Delete by entity fields / 按实体字段删除 */
    long deleteByEntity(T entity);

    // ──── by lambda reference / 按 Lambda 字段引用 ────

    /** Find one by field value / 按字段值查一条 */
    <R> T findOne(SFunction<T, R> field, R value);

    /** Count by field value / 按字段值统计 */
    <R> long count(SFunction<T, R> field, R value);

    /** Update one by field value / 按字段值更新一条 */
    <R> long updateOne(SFunction<T, R> field, R value, T entity);

    /** Update one by field value, optional upsert / 按字段值更新一条，可选 upsert */
    <R> long updateOne(SFunction<T, R> field, R value, T entity, boolean upsert);

    /** Update many by field value / 按字段值更新多条 */
    <R> long updateMany(SFunction<T, R> field, R value, T entity);

    /** Update many by field value, optional upsert / 按字段值更新多条，可选 upsert */
    <R> long updateMany(SFunction<T, R> field, R value, T entity, boolean upsert);

    /** Delete one by field value / 按字段值删一条 */
    <R> long deleteOne(SFunction<T, R> field, R value);

    /** Delete many by field value / 按字段值删多条 */
    <R> long deleteMany(SFunction<T, R> field, R value);

    // ──── by LambdaQueryWrapper / 按 LambdaQueryWrapper 查询 ────

    /** Find one by LambdaQueryWrapper / 按 LambdaQueryWrapper 查一条 */
    T findOne(LambdaQueryWrapper<T> wrapper);

    /** Find list by LambdaQueryWrapper / 按 LambdaQueryWrapper 查列表 */
    List<T> findList(LambdaQueryWrapper<T> wrapper);

    /** Paginated query by LambdaQueryWrapper / 按 LambdaQueryWrapper 分页查 */
    PageDTO<T> findPage(LambdaQueryWrapper<T> wrapper, PageDTO<T> pageDTO);

    /** Count by LambdaQueryWrapper / 按 LambdaQueryWrapper 统计 */
    long count(LambdaQueryWrapper<T> wrapper);

    /** Update one by LambdaQueryWrapper / 按 LambdaQueryWrapper 更新一条 */
    long updateOne(LambdaQueryWrapper<T> wrapper, T entity);

    /** Update one by LambdaQueryWrapper, optional upsert / 按 LambdaQueryWrapper 更新一条，可选 upsert */
    long updateOne(LambdaQueryWrapper<T> wrapper, T entity, boolean upsert);

    /** Update many by LambdaQueryWrapper / 按 LambdaQueryWrapper 更新多条 */
    long updateMany(LambdaQueryWrapper<T> wrapper, T entity);

    /** Update many by LambdaQueryWrapper, optional upsert / 按 LambdaQueryWrapper 更新多条，可选 upsert */
    long updateMany(LambdaQueryWrapper<T> wrapper, T entity, boolean upsert);

    /** Delete one by LambdaQueryWrapper / 按 LambdaQueryWrapper 删一条 */
    long deleteOne(LambdaQueryWrapper<T> wrapper);

    /** Delete many by LambdaQueryWrapper / 按 LambdaQueryWrapper 删多条 */
    long deleteMany(LambdaQueryWrapper<T> wrapper);
}
