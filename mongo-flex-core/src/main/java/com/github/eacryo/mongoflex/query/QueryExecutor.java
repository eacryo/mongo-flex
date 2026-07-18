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

    /** Find one by LambdaQueryWrapper / 按 LambdaQueryWrapper 查询一条 */
    public T findOne(LambdaQueryWrapper<T> query) {
        Objects.requireNonNull(query, "query must not be null");
        Bson filter = MongoBsonRenderer.render(query, convertor);
        Document document = databaseSupplier.get().getCollection(collectionName).find(filter).first();
        return document != null ? convertor.read(document, resolveEntityClass(query)) : null;
    }

    /** Find list by LambdaQueryWrapper / 按 LambdaQueryWrapper 查询列表 */
    public List<T> findList(LambdaQueryWrapper<T> query, Integer skip, Integer limit) {
        Objects.requireNonNull(query, "query must not be null");
        Bson filter = MongoBsonRenderer.render(query, convertor);
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

    /** Count by LambdaQueryWrapper / 按 LambdaQueryWrapper 统计 */
    public long count(LambdaQueryWrapper<T> query) {
        Objects.requireNonNull(query, "query must not be null");
        Bson filter = MongoBsonRenderer.render(query, convertor);
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

    /** Delete one by LambdaQueryWrapper / 按 LambdaQueryWrapper 删除一条 */
    public long deleteOne(LambdaQueryWrapper<T> query) {
        Objects.requireNonNull(query, "query must not be null");
        Bson filter = MongoBsonRenderer.render(query, convertor);
        DeleteResult result = databaseSupplier.get().getCollection(collectionName).deleteOne(filter);
        return result.getDeletedCount();
    }

    /** Delete many by LambdaQueryWrapper / 按 LambdaQueryWrapper 删除多条 */
    public long deleteMany(LambdaQueryWrapper<T> query) {
        Objects.requireNonNull(query, "query must not be null");
        Bson filter = MongoBsonRenderer.render(query, convertor);
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

    /** Update one by LambdaQueryWrapper / 按 LambdaQueryWrapper 更新一条 */
    public long updateOne(LambdaQueryWrapper<T> query, Document updateDoc, boolean upsert) {
        Objects.requireNonNull(query, "query must not be null");
        Bson filter = MongoBsonRenderer.render(query, convertor);
        UpdateOptions options = upsert ? new UpdateOptions().upsert(true) : new UpdateOptions();
        UpdateResult result = databaseSupplier.get().getCollection(collectionName)
                .updateOne(filter, updateDoc, options);
        return result.getModifiedCount();
    }

    /** Update many by LambdaQueryWrapper / 按 LambdaQueryWrapper 更新多条 */
    public long updateMany(LambdaQueryWrapper<T> query, Document updateDoc, boolean upsert) {
        Objects.requireNonNull(query, "query must not be null");
        Bson filter = MongoBsonRenderer.render(query, convertor);
        UpdateOptions options = upsert ? new UpdateOptions().upsert(true) : new UpdateOptions();
        UpdateResult result = databaseSupplier.get().getCollection(collectionName)
                .updateMany(filter, updateDoc, options);
        return result.getModifiedCount();
    }

    // ── pagination / 分页 ──

    /** Paginated find by LambdaQueryWrapper / 按 LambdaQueryWrapper 分页查询 */
    public PageDTO<T> findPage(LambdaQueryWrapper<T> query, PageDTO<T> pageDTO) {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(pageDTO, "pageDTO must not be null");
        Bson filter = MongoBsonRenderer.render(query, convertor);
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

    private Class<T> resolveEntityClass(LambdaQueryWrapper<T> query) {
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
