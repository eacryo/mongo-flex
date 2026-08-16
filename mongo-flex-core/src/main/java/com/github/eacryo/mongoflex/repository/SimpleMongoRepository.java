package com.github.eacryo.mongoflex.repository;

import com.github.eacryo.mongoflex.annotation.CollectionId;
import com.github.eacryo.mongoflex.annotation.CreateDate;
import com.github.eacryo.mongoflex.annotation.UpdateDate;
import com.github.eacryo.mongoflex.config.DateValueProvider;
import com.github.eacryo.mongoflex.config.IdGenerator;
import com.github.eacryo.mongoflex.constant.IdType;
import com.github.eacryo.mongoflex.convertor.MongoMappingConvertor;
import com.github.eacryo.mongoflex.entity.PageDTO;
import com.github.eacryo.mongoflex.entity.SortOrder;
import com.github.eacryo.mongoflex.query.LambdaQueryWrapper;
import com.github.eacryo.mongoflex.query.MongoBsonRenderer;
import com.github.eacryo.mongoflex.query.QueryExecutor;
import com.github.eacryo.mongoflex.util.DateValueGenerator;
import com.github.eacryo.mongoflex.util.ReflectUtil;
import com.github.eacryo.mongoflex.util.SFunction;
import com.github.eacryo.mongoflex.ulid.Ulid;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * This class should not be exposed to external use
 * The implementation of this class refers to spring-data-mongodb's SimpleMongoRepository
 *
 * @param <T>
 * @param <ID>
 */
public class SimpleMongoRepository<T, ID> implements MongoRepository<T, ID> {

    private final Supplier<MongoDatabase> databaseSupplier;
    final String collectionName;
    private final Class<T> entityClass;
    private final MongoMappingConvertor mongoMappingConvertor;
    private final IdGenerator<?> idGenerator;
    private final DateValueProvider dateValueProvider;
    /** Unified query execution engine / 统一查询执行引擎 */
    private final QueryExecutor<T> queryExecutor;
    /** Cache for per-entity-class ID generator instances, populated from @CollectionId.generatorClass. / 按实体类缓存 ID 生成器实例，从 @CollectionId.generatorClass 中解析。 */
    private final Map<Class<? extends IdGenerator>, IdGenerator<?>> generatorCache = new ConcurrentHashMap<>();
    /** Cache for per-class DateValueProvider instances, populated from @CreateDate/@UpdateDate.providerClass. / 按类缓存 DateValueProvider 实例，从 @CreateDate/@UpdateDate.providerClass 中解析。 */
    private final Map<Class<? extends DateValueProvider>, DateValueProvider> dateProviderCache = new ConcurrentHashMap<>();


    public SimpleMongoRepository(Supplier<MongoDatabase> databaseSupplier, String collectionName, Class<T> entityClass,
                                 MongoMappingConvertor mongoMappingConvertor, IdGenerator<?> idGenerator,
                                 DateValueProvider dateValueProvider) {
        this.databaseSupplier = Objects.requireNonNull(databaseSupplier, "databaseSupplier must not be null");
        this.collectionName = Objects.requireNonNull(collectionName, "collectionName must not be null");
        this.entityClass = Objects.requireNonNull(entityClass, "entityClass must not be null");
        this.mongoMappingConvertor = Objects.requireNonNull(mongoMappingConvertor, "mongoMappingConvertor must not be null");
        this.idGenerator = idGenerator;
        this.dateValueProvider = dateValueProvider;
        this.queryExecutor = new QueryExecutor<>(entityClass, databaseSupplier, collectionName, mongoMappingConvertor);
    }

    // ---- create ----

    @Override
    public T insert(T entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        fillId(entity);
        fillDate(entity, true);
        Document document = mongoMappingConvertor.write(entity);
        InsertOneResult insertOneResult = databaseSupplier.get().getCollection(collectionName).insertOne(document);
        BsonValue insertedId = insertOneResult.getInsertedId();
        if (insertedId != null) {
            Field idField = mongoMappingConvertor.getCollectionIdField(entityClass);
            if (idField != null) {
                idField.setAccessible(true);
                try {
                    if (idField.get(entity) == null) {
                        idField.set(entity, insertedId.asObjectId().getValue().toHexString());
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to set ID field after insert: " , e);
                }
            }
        }
        return entity;
    }

    @Override
    public List<T> insertMany(List<T> entities) {
        Objects.requireNonNull(entities, "entities must not be null / entities 不能为 null");
        if (entities.isEmpty()) {
            throw new IllegalArgumentException("entities must not be empty / entities 不能为空列表");
        }
        // Fill ID and date for each entity / 为每个实体填充 ID 和日期
        for (T entity : entities) {
            Objects.requireNonNull(entity, "each entity in the list must not be null / 列表中的每个实体不能为 null");
            fillId(entity);
            fillDate(entity, true);
        }
        // Convert all entities to Documents / 将所有实体转换为 Document
        List<Document> documents = new ArrayList<>(entities.size());
        for (T entity : entities) {
            documents.add(mongoMappingConvertor.write(entity));
        }
        // Batch insert via MongoDB insertMany / 通过 MongoDB insertMany 批量插入
        InsertManyResult result = databaseSupplier.get().getCollection(collectionName).insertMany(documents);
        // Back-fill ObjectId for OBJECT_ID idType if generated by MongoDB / 如果 MongoDB 生成了 ObjectId，回填到实体
        Field idField = mongoMappingConvertor.getCollectionIdField(entityClass);
        if (idField != null) {
            idField.setAccessible(true);
            result.getInsertedIds().forEach((index, bsonValue) -> {
                T entity = entities.get(index);
                try {
                    if (idField.get(entity) == null && bsonValue != null) {
                        idField.set(entity, bsonValue.asObjectId().getValue().toHexString());
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to set ID field after insertMany: " + index, e);
                }
            });
        }
        return entities;
    }

    // ---- read ----

    @Override
    public T findById(ID id) {
        Objects.requireNonNull(id, "id must not be null");
        Object queryId = convertIdIfNecessary(id);
        Document document = databaseSupplier.get().getCollection(collectionName).find(Filters.eq("_id", queryId)).first();
        return mongoMappingConvertor.read(document, entityClass);
    }

    @Override
    public T findOneByEntity(T entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        Document query = convertQueryId(mongoMappingConvertor.write(entity));
        Document document = databaseSupplier.get().getCollection(collectionName).find(query).first();
        return mongoMappingConvertor.read(document, entityClass);
    }

    @Override
    public List<T> findListByEntity(T entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        Document query = convertQueryId(mongoMappingConvertor.write(entity));
        List<Document> docs = databaseSupplier.get().getCollection(collectionName).find(query).into(new ArrayList<>());
        List<T> result = new ArrayList<>();
        for (Document d : docs) {
            result.add(mongoMappingConvertor.read(d, entityClass));
        }
        return result;
    }

    @Override
    public <R> T findOne(SFunction<T, R> field, R value) {
        Objects.requireNonNull(field, "field must not be null");
        Document filter = buildFilterFromLambda(field, value);
        Document document = databaseSupplier.get().getCollection(collectionName).find(filter).first();
        return mongoMappingConvertor.read(document, entityClass);
    }

    @Override
    public T findOne(LambdaQueryWrapper<T> wrapper) {
        Objects.requireNonNull(wrapper, "wrapper must not be null");
        ensureEntityClass(wrapper);
        Bson filter = MongoBsonRenderer.render(wrapper, mongoMappingConvertor);
        FindIterable<Document> iter = databaseSupplier.get().getCollection(collectionName).find(filter);
        applyProjection(wrapper, iter);
        Document document = iter.first();
        return mongoMappingConvertor.read(document, entityClass);
    }

    @Override
    public List<T> findList(LambdaQueryWrapper<T> wrapper) {
        Objects.requireNonNull(wrapper, "wrapper must not be null");
        ensureEntityClass(wrapper);
        Bson filter = MongoBsonRenderer.render(wrapper, mongoMappingConvertor);
        FindIterable<Document> iter = databaseSupplier.get().getCollection(collectionName).find(filter);
        applyProjection(wrapper, iter);
        List<Document> docs = iter.into(new ArrayList<>());
        List<T> result = new ArrayList<>();
        for (Document d : docs) {
            result.add(mongoMappingConvertor.read(d, entityClass));
        }
        return result;
    }

    @Override
    public List<T> findAll() {
        List<Document> docs = databaseSupplier.get().getCollection(collectionName).find().into(new ArrayList<>());
        List<T> result = new ArrayList<>();
        for (Document d : docs) {
            result.add(mongoMappingConvertor.read(d, entityClass));
        }
        return result;
    }

    // ---- page ----

    @Override
    public PageDTO<T> findPage(LambdaQueryWrapper<T> wrapper, PageDTO<T> pageDTO) {
        Objects.requireNonNull(wrapper, "wrapper must not be null");
        Objects.requireNonNull(pageDTO, "pageDTO must not be null");
        ensureEntityClass(wrapper);
        Bson filter = MongoBsonRenderer.render(wrapper, mongoMappingConvertor);
        return doFindPage(filter, wrapper, pageDTO);
    }

    @Override
    public PageDTO<T> findPageByEntity(T entity, PageDTO<T> pageDTO) {
        Objects.requireNonNull(entity, "entity must not be null");
        Objects.requireNonNull(pageDTO, "pageDTO must not be null");
        Document query = convertQueryId(mongoMappingConvertor.write(entity));
        return doFindPage(query, null, pageDTO);
    }

    private PageDTO<T> doFindPage(Bson filter, LambdaQueryWrapper<T> wrapper, PageDTO<T> pageDTO) {
        long currentPage = pageDTO.getCurrentPage();
        long pageSize = pageDTO.getPageSize();

        // Parameter validation up front (Spring Data AbstractPageRequest validates in the
        // constructor — fail fast instead of silently returning an empty page). /
        // 参数前置校验（对标 Spring Data AbstractPageRequest 构造时校验——fail-fast，
        // 而不是静默返回空页）。
        if (currentPage < 1) {
            throw new IllegalArgumentException(
                    "currentPage must be >= 1 / currentPage 不能小于 1: " + currentPage);
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException(
                    "pageSize must be >= 1 / pageSize 不能小于 1: " + pageSize);
        }
        Long offset = pageDTO.getOffset();
        if (offset != null && offset < 0) {
            throw new IllegalArgumentException(
                    "offset must not be negative / offset 不能为负数: " + offset);
        }

        // skip + limit — offset mode (PageDTO.offset) wins over page-number mode /
        //    偏移量模式（PageDTO.offset）优先于页码模式
        int skip = offset != null ? offset.intValue() : (int) ((currentPage - 1) * pageSize);
        int limit = (int) pageSize;

        // 1. count total — skip it in lightweight mode (MyBatis-Plus searchCount=false /
        //    Spring Data Slice) / 查总数——轻量模式（countTotal=false）跳过（对标
        //    MyBatis-Plus searchCount=false / Spring Data Slice）
        boolean countTotal = pageDTO.isCountTotal();
        if (countTotal) {
            long total = databaseSupplier.get().getCollection(collectionName).countDocuments(filter);
            pageDTO.setTotal(total);
        } else {
            pageDTO.setTotal(0L);
        }

        // 2. fetch — lightweight mode fetches one extra document to derive hasNext /
        //    查询——轻量模式多取一条用于推导 hasNext（Slice 语义）
        int fetchLimit = countTotal ? limit : limit + 1;
        FindIterable<Document> iterable = databaseSupplier.get().getCollection(collectionName)
                .find(filter).skip(skip).limit(fetchLimit);

        // 3. 投影
        applyProjection(wrapper, iterable);

        // 4. 排序（优先 Lambda 排序，其次 pageDTO.orderBy 字符串排序）
        Document sortDoc = buildSort(wrapper, pageDTO);
        if (sortDoc != null) {
            iterable.sort(sortDoc);
        }

        // 5. 读数据
        List<Document> docs = iterable.into(new ArrayList<>());
        boolean hasNext = false;
        if (!countTotal && docs.size() > limit) {
            hasNext = true;
            docs = docs.subList(0, limit);
        }
        // Reset in full mode so a previous lightweight run cannot leak its hasNext. /
        // 完整模式重置，避免上一次轻量运行的 hasNext 泄漏。
        pageDTO.setHasNext(countTotal ? null : hasNext);

        List<T> records = new ArrayList<>();
        for (Document doc : docs) {
            records.add(mongoMappingConvertor.read(doc, entityClass));
        }
        pageDTO.setRecords(records);
        return pageDTO;
    }

    /**
     * 构建排序 Document。优先使用 LambdaQueryWrapper 的类型安全排序
     * （可正确解析 @CollectionField 映射），否则 fallback 到 PageDTO 的字符串排序。
     */
    private Document buildSort(LambdaQueryWrapper<T> wrapper, PageDTO<T> pageDTO) {
        // 优先：Lambda 类型安全排序
        if (wrapper != null && !wrapper.getOrderBys().isEmpty()) {
            Document sort = new Document();
            for (LambdaQueryWrapper.OrderBy ob : wrapper.getOrderBys()) {
                Class<?> resolveClass = ob.getImplClass() != null ? ob.getImplClass() : entityClass;
                String mongoField = mongoMappingConvertor.resolveMongoFieldPath(resolveClass, ob.getJavaFieldName());
                sort.append(mongoField, ob.isAscending() ? 1 : -1);
            }
            return sort;
        }
        // Fallback：PageDTO SortOrder 排序
        if (pageDTO.getOrderBy() != null && !pageDTO.getOrderBy().isEmpty()) {
            Document sort = new Document();
            for (SortOrder<T> so : pageDTO.getOrderBy()) {
                // Lambda-based SortOrder carries implClass for correct @CollectionField resolution;
                // string-based SortOrder falls back to entityClass / 基于 Lambda 的 SortOrder 携带 implClass 以正确解析 @CollectionField
                Class<?> resolveClass = so.getImplClass() != null ? so.getImplClass() : entityClass;
                String javaField = so.getJavaFieldName() != null ? so.getJavaFieldName() : so.getField();
                String mongoField = mongoMappingConvertor.resolveMongoFieldPath(resolveClass, javaField);
                sort.append(mongoField, so.isAscending() ? 1 : -1);
            }
            return sort;
        }
        return null;
    }

    // ---- count ----

    @Override
    public long count(LambdaQueryWrapper<T> wrapper) {
        Objects.requireNonNull(wrapper, "wrapper must not be null");
        ensureEntityClass(wrapper);
        Bson filter = MongoBsonRenderer.render(wrapper, mongoMappingConvertor);
        return databaseSupplier.get().getCollection(collectionName).countDocuments(filter);
    }

    @Override
    public long count() {
        return databaseSupplier.get().getCollection(collectionName).countDocuments();
    }

    @Override
    public long countByEntity(T entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        Document query = convertQueryId(mongoMappingConvertor.write(entity));
        return databaseSupplier.get().getCollection(collectionName).countDocuments(query);
    }

    @Override
    public <R> long count(SFunction<T, R> field, R value) {
        Objects.requireNonNull(field, "field must not be null");
        Document filter = buildFilterFromLambda(field, value);
        return databaseSupplier.get().getCollection(collectionName).countDocuments(filter);
    }

    // ---- update / upsert ----

    @Override
    public long updateOneById(T entity) {
        return doUpdateOneById(entity, false);
    }

    @Override
    public long updateOneById(T entity, boolean upsert) {
        return doUpdateOneById(entity, upsert);
    }

    private long doUpdateOneById(T entity, boolean upsert) {
        Objects.requireNonNull(entity, "entity must not be null");
        fillDate(entity, upsert);
        Document doc = mongoMappingConvertor.write(entity);
        Object id = doc.remove("_id");
        if (id == null) {
            throw new IllegalArgumentException("Entity must have a non-null id for updateOneById");
        }
        Object queryId = (id instanceof String && ObjectId.isValid((String) id)) ? new ObjectId((String) id) : id;
        Document updateDoc = new Document("$set", doc);
        UpdateOptions options = upsert ? new UpdateOptions().upsert(true) : new UpdateOptions();
        UpdateResult result = databaseSupplier.get().getCollection(collectionName)
                .updateOne(Filters.eq("_id", queryId), updateDoc, options);
        return result.getModifiedCount();
    }

    // ── updateOne by field ──

    @Override
    public <R> long updateOne(SFunction<T, R> field, R value, T entity) {
        return doExecuteUpdate(field, value, entity, false, false);
    }

    @Override
    public <R> long updateOne(SFunction<T, R> field, R value, T entity, boolean upsert) {
        return doExecuteUpdate(field, value, entity, false, upsert);
    }

    // ── updateOne by wrapper ──

    @Override
    public long updateOne(LambdaQueryWrapper<T> wrapper, T entity) {
        return doExecuteUpdate(wrapper, entity, false, false);
    }

    @Override
    public long updateOne(LambdaQueryWrapper<T> wrapper, T entity, boolean upsert) {
        return doExecuteUpdate(wrapper, entity, false, upsert);
    }

    // ── updateMany by field ──

    @Override
    public <R> long updateMany(SFunction<T, R> field, R value, T entity) {
        return doExecuteUpdate(field, value, entity, true, false);
    }

    @Override
    public <R> long updateMany(SFunction<T, R> field, R value, T entity, boolean upsert) {
        return doExecuteUpdate(field, value, entity, true, upsert);
    }

    // ── updateMany by wrapper ──

    @Override
    public long updateMany(LambdaQueryWrapper<T> wrapper, T entity) {
        return doExecuteUpdate(wrapper, entity, true, false);
    }

    @Override
    public long updateMany(LambdaQueryWrapper<T> wrapper, T entity, boolean upsert) {
        return doExecuteUpdate(wrapper, entity, true, upsert);
    }

    // ── internal helpers ──

    private <R> long doExecuteUpdate(SFunction<T, R> field, R value, T entity, boolean many, boolean upsert) {
        Objects.requireNonNull(field, "field must not be null");
        Objects.requireNonNull(entity, "entity must not be null");
        Document filter = buildFilterFromLambda(field, value);
        return executeUpdate(filter, entity, many, upsert);
    }

    private long doExecuteUpdate(LambdaQueryWrapper<T> wrapper, T entity, boolean many, boolean upsert) {
        Objects.requireNonNull(wrapper, "wrapper must not be null");
        Objects.requireNonNull(entity, "entity must not be null");
        requireNonEmptyWrapper(wrapper, many ? "updateMany" : "updateOne");
        ensureEntityClass(wrapper);
        Bson filter = MongoBsonRenderer.render(wrapper, mongoMappingConvertor);
        return executeUpdate(filter, entity, many, upsert);
    }

    private long executeUpdate(Bson filter, T entity, boolean many, boolean upsert) {
        fillDate(entity, upsert);
        Document doc = mongoMappingConvertor.write(entity);
        doc.remove("_id");
        Document updateDoc = new Document("$set", doc);
        UpdateOptions options = upsert ? new UpdateOptions().upsert(true) : new UpdateOptions();
        UpdateResult result;
        if (many) {
            result = databaseSupplier.get().getCollection(collectionName).updateMany(filter, updateDoc, options);
        } else {
            result = databaseSupplier.get().getCollection(collectionName).updateOne(filter, updateDoc, options);
        }
        return result.getModifiedCount();
    }

    // ---- delete ----

    @Override
    public long deleteOneById(ID id) {
        Objects.requireNonNull(id, "id must not be null");
        Object queryId = convertIdIfNecessary(id);
        DeleteResult result = databaseSupplier.get().getCollection(collectionName)
                .deleteOne(Filters.eq("_id", queryId));
        return result.getDeletedCount();
    }

    @Override
    public <R> long deleteOne(SFunction<T, R> field, R value) {
        Objects.requireNonNull(field, "field must not be null");
        Document filter = buildFilterFromLambda(field, value);
        DeleteResult result = databaseSupplier.get().getCollection(collectionName).deleteOne(filter);
        return result.getDeletedCount();
    }

    @Override
    public long deleteOne(LambdaQueryWrapper<T> wrapper) {
        Objects.requireNonNull(wrapper, "wrapper must not be null");
        requireNonEmptyWrapper(wrapper, "deleteOne");
        ensureEntityClass(wrapper);
        Bson filter = MongoBsonRenderer.render(wrapper, mongoMappingConvertor);
        DeleteResult result = databaseSupplier.get().getCollection(collectionName).deleteOne(filter);
        return result.getDeletedCount();
    }

    @Override
    public long deleteByEntity(T entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        Document query = convertQueryId(mongoMappingConvertor.write(entity));
        requireNonEmptyQuery(query, "deleteByEntity");
        DeleteResult result = databaseSupplier.get().getCollection(collectionName).deleteMany(query);
        return result.getDeletedCount();
    }

    @Override
    public <R> long deleteMany(SFunction<T, R> field, R value) {
        Objects.requireNonNull(field, "field must not be null");
        Document filter = buildFilterFromLambda(field, value);
        DeleteResult result = databaseSupplier.get().getCollection(collectionName).deleteMany(filter);
        return result.getDeletedCount();
    }

    @Override
    public long deleteMany(LambdaQueryWrapper<T> wrapper) {
        Objects.requireNonNull(wrapper, "wrapper must not be null");
        requireNonEmptyWrapper(wrapper, "deleteMany");
        ensureEntityClass(wrapper);
        Bson filter = MongoBsonRenderer.render(wrapper, mongoMappingConvertor);
        DeleteResult result = databaseSupplier.get().getCollection(collectionName).deleteMany(filter);
        return result.getDeletedCount();
    }

    @Override
    public long deleteAll() {
        DeleteResult result = databaseSupplier.get().getCollection(collectionName).deleteMany(new Document());
        return result.getDeletedCount();
    }

    // ---- annotation-driven query execution / 注解驱动的查询执行 ----
    // These are called by MyRepositoryProxyHandler for @Find/@Count/@Delete/@Update methods.
    // They bypass the Lambda query wrapper and work directly with Bson filters.
    // 以下方法供 MyRepositoryProxyHandler 调用，用于 @Find/@Count/@Delete/@Update 注解方法，
    // 绕过 Lambda 查询包装器，直接使用 Bson 过滤器。

    /**
     * Find list by raw Bson filter with optional pagination / 根据原始 Bson 过滤器查询列表，可选分页
     */
    public List<T> findListByFilter(Bson filter, int skip, int limit) {
        FindIterable<Document> iter = databaseSupplier.get().getCollection(collectionName).find(filter);
        if (skip > 0) {
            iter = iter.skip(skip);
        }
        if (limit > 0) {
            iter = iter.limit(limit);
        }
        List<Document> docs = iter.into(new ArrayList<>());
        List<T> result = new ArrayList<>();
        for (Document d : docs) {
            result.add(mongoMappingConvertor.read(d, entityClass));
        }
        return result;
    }

    /**
     * Find one by raw Bson filter / 根据原始 Bson 过滤器查询单条
     */
    public T findOneByFilter(Bson filter) {
        Document document = databaseSupplier.get().getCollection(collectionName).find(filter).first();
        return mongoMappingConvertor.read(document, entityClass);
    }

    /**
     * Count by raw Bson filter / 根据原始 Bson 过滤器统计数量
     */
    public long countByFilter(Bson filter) {
        return databaseSupplier.get().getCollection(collectionName).countDocuments(filter);
    }

    /**
     * Delete by raw Bson filter / 根据原始 Bson 过滤器删除
     */
    public long deleteByFilter(Bson filter) {
        DeleteResult result = databaseSupplier.get().getCollection(collectionName).deleteMany(filter);
        return result.getDeletedCount();
    }

    /**
     * Update by raw Bson filter and update document / 根据原始 Bson 过滤器和更新文档执行更新
     */
    public long updateByFilter(Bson filter, Document updateDoc, boolean multi, boolean upsert) {
        UpdateOptions options = new UpdateOptions().upsert(upsert);
        UpdateResult result;
        if (multi) {
            result = databaseSupplier.get().getCollection(collectionName).updateMany(filter, updateDoc, options);
        } else {
            result = databaseSupplier.get().getCollection(collectionName).updateOne(filter, updateDoc, options);
        }
        return result.getModifiedCount();
    }

    // ---- internal helpers ----

    private void requireNonEmptyWrapper(LambdaQueryWrapper<T> wrapper, String operation) {
        // Recursively checks nested AND/OR/NOT groups, so a wrapper whose only conditions live
        // inside empty nested groups is also rejected. / 递归检查嵌套的 AND/OR/NOT 分组，
        // 因此"仅包含空嵌套分组"的 wrapper 同样会被拒绝。
        if (!LambdaQueryWrapper.hasEffectiveConditions(wrapper)) {
            String hint = operation.startsWith("delete") ? "deleteAll" : "";
            throw new IllegalArgumentException(
                    operation + " requires at least one condition." + (hint.isEmpty() ? "" : " Use " + hint + "() to operate on all documents."));
        }
    }

    private void requireNonEmptyQuery(Document query, String operation) {
        if (query.isEmpty()) {
            throw new IllegalArgumentException(
                    operation + " requires at least one non-null field. Use deleteAll() to operate on all documents."
                    + " / " + operation + " 至少需要一个非空字段，全量操作请使用 deleteAll()。");
        }
    }

    private void fillId(T entity){
        Field idField = mongoMappingConvertor.getCollectionIdField(entityClass);
        if (idField == null){
            //no @CollectionId annotation found, nothing to do
            return;
        }
        CollectionId annotation = idField.getAnnotation(CollectionId.class);
        IdType idType = annotation.value();
        if (idType == IdType.OBJECT_ID){
            //OBJECT_ID:let mongodb generate
            return;
        }
        idField.setAccessible(true);
        try{
            //If the field already has a value, do not overwrite it
            if (idField.get(entity) != null){
                return;
            }
            switch (idType){
                case ULID :
                    idField.set(entity, Ulid.generate());
                    break;
                case UUID :
                    idField.set(entity, UUID.randomUUID().toString());
                    break;
                case INPUT :
                    idField.set(entity, resolvePerEntityGenerator(annotation).create());
                    break;
                default : break;
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to set ID field: " + idField.getName(), e);
        }
    }


    /**
     * Resolve the IdGenerator for the current entity: annotation-specified class first, fallback to global bean. / 解析当前实体的 IdGenerator：优先使用注解指定的类，回退到全局 bean。
     */
    @SuppressWarnings("unchecked")
    private IdGenerator<Object> resolvePerEntityGenerator(CollectionId annotation) {
        Class<? extends IdGenerator> generatorClass = annotation.generatorClass();
        // Per-entity generator specified in annotation / 注解中指定了按实体生成器
        if (generatorClass != IdGenerator.None.class) {
            return (IdGenerator<Object>) generatorCache.computeIfAbsent(generatorClass, this::instantiateGenerator);
        }
        // Fallback to global IdGenerator bean / 回退到全局 IdGenerator bean
        if (idGenerator != null) {
            return (IdGenerator<Object>) idGenerator;
        }
        throw new IllegalArgumentException(
                "IdType.INPUT requires either a generatorClass in @CollectionId or a global IdGenerator bean. "
                + "Neither was provided for entity class: " + entityClass.getName()
                + " / IdType.INPUT 需要在 @CollectionId 中指定 generatorClass 或提供全局 IdGenerator bean，"
                + "当前实体类两者均未提供: " + entityClass.getName());
    }

    /**
     * Instantiate a generator class via no-arg constructor. / 通过无参构造函数实例化生成器类。
     */
    private IdGenerator<?> instantiateGenerator(Class<? extends IdGenerator> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to instantiate IdGenerator: " + clazz.getName()
                    + ". Ensure it has a public no-arg constructor. / 无法实例化 IdGenerator: " + clazz.getName()
                    + "，请确保它有一个 public 的无参构造函数。", e);
        }
    }

    /**
     * Resolve the DateValueProvider for the current field: annotation-specified class first, fallback to global bean. / 解析当前字段的 DateValueProvider：优先使用注解指定的类，回退到全局 bean。
     */
    private DateValueProvider resolveDateProvider(Class<? extends DateValueProvider> providerClass) {
        // Per-field provider specified in annotation / 注解中指定了按字段提供器
        if (providerClass != DateValueProvider.None.class) {
            return dateProviderCache.computeIfAbsent(providerClass, clazz -> {
                try {
                    return clazz.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Failed to instantiate DateValueProvider: " + clazz.getName()
                            + ". Ensure it has a public no-arg constructor. / 无法实例化 DateValueProvider: " + clazz.getName()
                            + "，请确保它有一个 public 的无参构造函数。", e);
                }
            });
        }
        // Fallback to global DateValueProvider bean (may be null) / 回退到全局 DateValueProvider bean（可为 null）
        return dateValueProvider;
    }

    private void fillDate(T entity, boolean isInsert){
        try{
            if (isInsert){
                Field createDateField = mongoMappingConvertor.getCreateDateField(entityClass);
                if (createDateField != null){
                    createDateField.setAccessible(true);
                    if (createDateField.get(entity) == null){
                        CreateDate ann = createDateField.getAnnotation(CreateDate.class);
                        DateValueProvider provider = resolveDateProvider(ann.providerClass());
                        createDateField.set(entity, DateValueGenerator.generateCurrentDate(createDateField.getType(), ann.pattern(), provider));
                    }
                }
            }
            Field updateDateField = mongoMappingConvertor.getUpdateDateField(entityClass);
            if (updateDateField != null){
                updateDateField.setAccessible(true);
                UpdateDate ann = updateDateField.getAnnotation(UpdateDate.class);
                DateValueProvider provider = resolveDateProvider(ann.providerClass());
                updateDateField.set(entity, DateValueGenerator.generateCurrentDate(updateDateField.getType(), ann.pattern(), provider));
            }
        } catch (IllegalAccessException e){
            throw new RuntimeException("Failed to set date fields", e);
        }

    }

    private Object convertIdIfNecessary(ID id){
        if (shouldConvertToObjectId() && id instanceof String && ObjectId.isValid((String) id)){
            return new ObjectId((String) id);
        }
        return id;
    }

    /**
     * 判断是否需要将 String ID 转为 ObjectId。
     * 仅在 {@link IdType#OBJECT_ID} 模式下才需要转换，因为此时 MongoDB 生成的 ObjectId
     * 被映射为 hex String 存储在 Java 字段中，查询时必须转回 ObjectId。
     */
    private boolean shouldConvertToObjectId() {
        Field idField = mongoMappingConvertor.getCollectionIdField(entityClass);
        if (idField == null) {
            return false;
        }
        CollectionId annotation = idField.getAnnotation(CollectionId.class);
        return annotation.value() == IdType.OBJECT_ID;
    }

    private Document convertQueryId(Document query){
        Object id = query.get("_id");
        if (id != null){
            query.put("_id", convertIdIfNecessary((ID) id));
        }
        return query;
    }


    private void ensureEntityClass(LambdaQueryWrapper<T> wrapper) {
        if (wrapper.getEntityClass() == null) {
            wrapper.setEntityClass(entityClass);
        }
    }

    /**
     * 从 LambdaQueryWrapper 中提取投影字段并应用到 FindIterable。
     * 支持 include（{@link Projections#include(String...)}）、exclude
     * （{@link Projections#exclude(String...)}）以及 include + {@code _id} 排除的混合模式。
     *
     * <p>MongoDB 不允许同时使用 include 和 exclude（{@code _id} 除外）。
     * 若同时指定了 includes 和 excludes 且存在非 {@code _id} 的 exclude 字段，将抛出
     * {@link IllegalArgumentException}。</p>
     */
    private void applyProjection(LambdaQueryWrapper<T> wrapper, FindIterable<Document> iter) {
        if (wrapper == null) {
            return;
        }
        List<LambdaQueryWrapper.ProjectionField> includeFields = wrapper.getProjections();
        List<LambdaQueryWrapper.ProjectionField> excludeFields = wrapper.getExcludes();

        if (includeFields.isEmpty() && excludeFields.isEmpty()) {
            return;
        }

        // 将 Java 字段名解析为 MongoDB 字段名
        List<String> includeMongoFields = resolveProjectionFields(includeFields);
        List<String> excludeMongoFields = resolveProjectionFields(excludeFields);

        if (!includeMongoFields.isEmpty() && excludeMongoFields.isEmpty()) {
            // 纯 include 模式
            iter.projection(Projections.include(includeMongoFields));
        } else if (includeMongoFields.isEmpty() && !excludeMongoFields.isEmpty()) {
            // 纯 exclude 模式
            iter.projection(Projections.exclude(excludeMongoFields));
        } else {
            // 混合模式：只允许 exclude _id
            for (String f : excludeMongoFields) {
                if (!"_id".equals(f)) {
                    throw new IllegalArgumentException(
                            "MongoDB does not allow mixing include and exclude projections. "
                                    + "Found non-_id exclude field: '" + f + "'. "
                                    + "Use either include() or exclude(), or exclude only the _id field with include().");
                }
            }
            iter.projection(Projections.fields(
                    Projections.include(includeMongoFields),
                    Projections.excludeId()));
        }
    }

    private List<String> resolveProjectionFields(List<LambdaQueryWrapper.ProjectionField> fields) {
        List<String> mongoFields = new ArrayList<>();
        for (LambdaQueryWrapper.ProjectionField pf : fields) {
            Class<?> resolveClass = pf.getImplClass() != null ? pf.getImplClass() : entityClass;
            String mongoField = mongoMappingConvertor.resolveMongoFieldPath(resolveClass, pf.getJavaFieldName());
            mongoFields.add(mongoField);
        }
        return mongoFields;
    }

    private <R> Document buildFilterFromLambda(SFunction<T, R> field, R value) {
        String javaFieldName = ReflectUtil.getFieldNameFromLambda(field);
        String mongoFieldName = mongoMappingConvertor.resolveMongoFieldPath(entityClass, javaFieldName);
        Object queryValue = value;
        if ("_id".equals(mongoFieldName) && shouldConvertToObjectId()
                && value instanceof String && ObjectId.isValid((String) value)) {
            queryValue = new ObjectId((String) value);
        }
        return new Document(mongoFieldName, queryValue);
    }

 }

