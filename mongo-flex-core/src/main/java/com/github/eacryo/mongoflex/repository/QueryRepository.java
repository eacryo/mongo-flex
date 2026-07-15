package com.github.eacryo.mongoflex.repository;

import com.github.eacryo.mongoflex.entity.PageDTO;
import com.github.eacryo.mongoflex.query.QuerySpec;

import java.util.List;

/**
 * Query-capable repository — extends {@link CrudRepository} with
 * {@link QuerySpec}-based find/count/update/delete operations /
 * 查询扩展 Repository——在 {@link CrudRepository} 基础上增加基于
 * {@link QuerySpec} 的查询、统计、更新、删除操作。
 * <p>
 * All query methods accept the unified {@link QuerySpec} abstraction,
 * so they work with {@code LambdaQueryWrapper}, MQL criteria, or any
 * custom implementation /
 * 所有查询方法接受统一的 {@link QuerySpec} 抽象，
 * 适用于 LambdaQueryWrapper、MQL 条件或任何自定义实现。
 *
 * @param <T>  entity type / 实体类型
 * @param <ID> id type / ID 类型
 */
public interface QueryRepository<T, ID> extends CrudRepository<T, ID> {

    /** Find one by QuerySpec / 按 QuerySpec 查询一条 */
    T findOne(QuerySpec<T> query);

    /** Find list by QuerySpec / 按 QuerySpec 查询列表 */
    List<T> findList(QuerySpec<T> query);

    /** Paginated query by QuerySpec / 按 QuerySpec 分页查询 */
    PageDTO<T> findPage(QuerySpec<T> query, PageDTO<T> pageDTO);

    /** Count by QuerySpec / 按 QuerySpec 统计 */
    long count(QuerySpec<T> query);

    /** Update one by QuerySpec / 按 QuerySpec 更新一条 */
    long updateOne(QuerySpec<T> query, T entity);

    /** Update one by QuerySpec, optional upsert / 按 QuerySpec 更新一条，可选 upsert */
    long updateOne(QuerySpec<T> query, T entity, boolean upsert);

    /** Update many by QuerySpec / 按 QuerySpec 更新多条 */
    long updateMany(QuerySpec<T> query, T entity);

    /** Update many by QuerySpec, optional upsert / 按 QuerySpec 更新多条，可选 upsert */
    long updateMany(QuerySpec<T> query, T entity, boolean upsert);

    /** Delete one by QuerySpec / 按 QuerySpec 删除一条 */
    long deleteOne(QuerySpec<T> query);

    /** Delete many by QuerySpec / 按 QuerySpec 删除多条 */
    long deleteMany(QuerySpec<T> query);
}
