package com.github.eacryo.mongoflex.query;

import com.github.eacryo.mongoflex.convertor.MongoMappingConvertor;
import com.github.eacryo.mongoflex.entity.PageDTO;
import com.github.eacryo.mongoflex.entity.SortOrder;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Unified query execution engine — all CRUD operations flow through this class /
 * 统一的查询执行引擎——所有 CRUD 操作汇聚于此。
 * <p>
 * Replaces the scattered find/count/delete/update logic previously duplicated across
 * {@code SimpleMongoRepository} and the deprecated {@code strategy/} package /
 * 替代原先分散在 SimpleMongoRepository 和已废弃 strategy/ 包中的重复逻辑。
 *
 * @param <T> entity type / 实体类型
 */
public class QueryExecutor<T> {

    private final Class<T> entityClass;
    private final Supplier<MongoDatabase> databaseSupplier;
    private final String collectionName;
    private final MongoMappingConvertor convertor;

    public QueryExecutor(Class<T> entityClass,
                         Supplier<MongoDatabase> databaseSupplier,
                         String collectionName,
                         MongoMappingConvertor convertor) {
        this.entityClass = Objects.requireNonNull(entityClass, "entityClass must not be null");
        this.databaseSupplier = Objects.requireNonNull(databaseSupplier, "databaseSupplier must not be null");
        this.collectionName = Objects.requireNonNull(collectionName, "collectionName must not be null");
        this.convertor = Objects.requireNonNull(convertor, "convertor must not be null");
    }

    // ── find / 查询 ──

    /** Find one by QuerySpec / 按 QuerySpec 查询一条 */
    public T findOne(QuerySpec<T> query) {
        Objects.requireNonNull(query, "query must not be null");
        Bson filter = toFilter(query);
        Document document = databaseSupplier.get().getCollection(collectionName).find(filter).first();
        return document != null ? convertor.read(document, resolveEntityClass(query)) : null;
    }

    /** Find list by QuerySpec / 按 QuerySpec 查询列表 */
    public List<T> findList(QuerySpec<T> query, Integer skip, Integer limit) {
        Objects.requireNonNull(query, "query must not be null");
        Bson filter = toFilter(query);
        FindIterable<Document> iter = databaseSupplier.get().getCollection(collectionName).find(filter);
        if (skip != null && skip > 0) iter = iter.skip(skip);
        if (limit != null && limit > 0) iter = iter.limit(limit);
        List<Document> docs = iter.into(new ArrayList<>());
        Class<T> ec = resolveEntityClass(query);
        List<T> result = new ArrayList<>(docs.size());
        for (Document d : docs) {
            result.add(convertor.read(d, ec));
        }
        return result;
    }

    /** Find all documents / 查询全集合 */
    public List<T> findAll() {
        List<Document> docs = databaseSupplier.get().getCollection(collectionName).find().into(new ArrayList<>());
        List<T> result = new ArrayList<>(docs.size());
        for (Document d : docs) {
            result.add(convertor.read(d, entityClass));
        }
        return result;
    }

    /** Find one by raw Bson filter / 按原始 Bson 过滤器查询一条 */
    public T findOneByFilter(Bson filter) {
        Objects.requireNonNull(filter, "filter must not be null");
        Document document = databaseSupplier.get().getCollection(collectionName).find(filter).first();
        return document != null ? convertor.read(document, entityClass) : null;
    }

    /** Find list by raw Bson filter / 按原始 Bson 过滤器查询列表 */
    public List<T> findListByFilter(Bson filter, int skip, int limit) {
        Objects.requireNonNull(filter, "filter must not be null");
        FindIterable<Document> iter = databaseSupplier.get().getCollection(collectionName).find(filter);
        if (skip > 0) iter = iter.skip(skip);
        if (limit > 0) iter = iter.limit(limit);
        List<Document> docs = iter.into(new ArrayList<>());
        List<T> result = new ArrayList<>(docs.size());
        for (Document d : docs) {
            result.add(convertor.read(d, entityClass));
        }
        return result;
    }

    // ── count / 统计 ──

    /** Count by QuerySpec / 按 QuerySpec 统计 */
    public long count(QuerySpec<T> query) {
        Objects.requireNonNull(query, "query must not be null");
        Bson filter = toFilter(query);
        return databaseSupplier.get().getCollection(collectionName).countDocuments(filter);
    }

    /** Count by raw Bson filter / 按原始 Bson 过滤器统计 */
    public long countByFilter(Bson filter) {
        Objects.requireNonNull(filter, "filter must not be null");
        return databaseSupplier.get().getCollection(collectionName).countDocuments(filter);
    }

    /** Count all documents / 统计全集合 */
    public long countAll() {
        return databaseSupplier.get().getCollection(collectionName).countDocuments();
    }

    // ── delete / 删除 ──

    /** Delete one by QuerySpec / 按 QuerySpec 删除一条 */
    public long deleteOne(QuerySpec<T> query) {
        Objects.requireNonNull(query, "query must not be null");
        Bson filter = toFilter(query);
        DeleteResult result = databaseSupplier.get().getCollection(collectionName).deleteOne(filter);
        return result.getDeletedCount();
    }

    /** Delete many by QuerySpec / 按 QuerySpec 删除多条 */
    public long deleteMany(QuerySpec<T> query) {
        Objects.requireNonNull(query, "query must not be null");
        Bson filter = toFilter(query);
        DeleteResult result = databaseSupplier.get().getCollection(collectionName).deleteMany(filter);
        return result.getDeletedCount();
    }

    /** Delete many by raw Bson filter / 按原始 Bson 过滤器删除多条 */
    public long deleteByFilter(Bson filter) {
        Objects.requireNonNull(filter, "filter must not be null");
        DeleteResult result = databaseSupplier.get().getCollection(collectionName).deleteMany(filter);
        return result.getDeletedCount();
    }

    // ── update / 更新 ──

    /** Update one by QuerySpec / 按 QuerySpec 更新一条 */
    public long updateOne(QuerySpec<T> query, Document updateDoc, boolean upsert) {
        Objects.requireNonNull(query, "query must not be null");
        Bson filter = toFilter(query);
        UpdateOptions options = upsert ? new UpdateOptions().upsert(true) : new UpdateOptions();
        UpdateResult result = databaseSupplier.get().getCollection(collectionName)
                .updateOne(filter, updateDoc, options);
        return result.getModifiedCount();
    }

    /** Update many by QuerySpec / 按 QuerySpec 更新多条 */
    public long updateMany(QuerySpec<T> query, Document updateDoc, boolean upsert) {
        Objects.requireNonNull(query, "query must not be null");
        Bson filter = toFilter(query);
        UpdateOptions options = upsert ? new UpdateOptions().upsert(true) : new UpdateOptions();
        UpdateResult result = databaseSupplier.get().getCollection(collectionName)
                .updateMany(filter, updateDoc, options);
        return result.getModifiedCount();
    }

    // ── pagination / 分页 ──

    /** Paginated find by QuerySpec / 按 QuerySpec 分页查询 */
    public PageDTO<T> findPage(QuerySpec<T> query, PageDTO<T> pageDTO) {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(pageDTO, "pageDTO must not be null");
        Bson filter = toFilter(query);
        long currentPage = pageDTO.getCurrentPage();
        long pageSize = pageDTO.getPageSize();
        long total = databaseSupplier.get().getCollection(collectionName).countDocuments(filter);
        pageDTO.setTotal(total);
        int skip = (int) ((currentPage - 1) * pageSize);
        int limit = (int) pageSize;
        FindIterable<Document> iterable = databaseSupplier.get().getCollection(collectionName)
                .find(filter).skip(skip).limit(limit);
        applyPageSort(pageDTO, iterable);
        List<Document> docs = iterable.into(new ArrayList<>());
        List<T> records = new ArrayList<>(docs.size());
        Class<T> ec = resolveEntityClass(query);
        for (Document d : docs) {
            records.add(convertor.read(d, ec));
        }
        pageDTO.setRecords(records);
        return pageDTO;
    }

    // ── helpers / 辅助方法 ──

    private Bson toFilter(QuerySpec<T> query) {
        return query.toBson(convertor);
    }

    private Class<T> resolveEntityClass(QuerySpec<T> query) {
        Class<T> cls = query.getEntityClass();
        return cls != null ? cls : entityClass;
    }

    private void applyPageSort(PageDTO<T> pageDTO, FindIterable<Document> iter) {
        List<SortOrder<T>> orders = pageDTO.getOrderBy();
        if (orders == null || orders.isEmpty()) {
            return;
        }
        Document sortDoc = new Document();
        for (SortOrder<T> order : orders) {
            sortDoc.append(order.getField(), order.isAscending() ? 1 : -1);
        }
        iter.sort(sortDoc);
    }

    // ── accessors for SimpleMongoRepository / 供 SimpleMongoRepository 使用的访问器 ──

    public Class<T> getEntityClass() {
        return entityClass;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public MongoMappingConvertor getConvertor() {
        return convertor;
    }
}
