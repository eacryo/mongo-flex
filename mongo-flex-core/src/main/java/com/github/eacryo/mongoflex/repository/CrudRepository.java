package com.github.eacryo.mongoflex.repository;

import java.util.List;

/**
 * Minimal CRUD repository interface — insert, find by ID, count, delete /
 * 最精简的 CRUD Repository 接口——插入、按 ID 查询、统计、删除。
 * <p>
 * Extend this interface when you only need basic operations /
 * 仅需基础操作时继承此接口。
 *
 * @param <T>  entity type / 实体类型
 * @param <ID> id type / ID 类型
 */
public interface CrudRepository<T, ID> {

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
}
