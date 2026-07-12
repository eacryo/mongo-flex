package com.github.eacryo.mongoflex.v2;

import com.github.eacryo.mongoflex.annotation.CollectionId;
import com.github.eacryo.mongoflex.annotation.CreateDate;
import com.github.eacryo.mongoflex.annotation.UpdateDate;
import com.github.eacryo.mongoflex.config.IdGenerator;
import com.github.eacryo.mongoflex.constant.IdType;
import com.github.eacryo.mongoflex.convertor.MongoMappingConvertor;
import com.github.eacryo.mongoflex.entity.PageDTO;
import com.github.eacryo.mongoflex.lambda.LambdaQueryWrapper;
import com.github.eacryo.mongoflex.lambda.MongoBsonRenderer;
import com.github.eacryo.mongoflex.util.DateValueGenerator;
import com.github.eacryo.mongoflex.util.ReflectUtil;
import com.github.eacryo.mongoflex.util.SFunction;
import com.github.f4b6a3.ulid.UlidCreator;
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
    private final String collectionName;
    private final Class<T> entityClass;
    private final MongoMappingConvertor mongoMappingConvertor;
    private final IdGenerator<?> idGenerator;
    /** Cache for per-entity-class ID generator instances, populated from @CollectionId.generatorClass. / 按实体类缓存 ID 生成器实例，从 @CollectionId.generatorClass 中解析。 */
    private final Map<Class<? extends IdGenerator>, IdGenerator<?>> generatorCache = new ConcurrentHashMap<>();


    public SimpleMongoRepository(Supplier<MongoDatabase> databaseSupplier, String collectionName, Class<T> entityClass,
                                 MongoMappingConvertor mongoMappingConvertor, IdGenerator<?> idGenerator) {
        this.databaseSupplier = Objects.requireNonNull(databaseSupplier, "databaseSupplier must not be null");
        this.collectionName = Objects.requireNonNull(collectionName, "collectionName must not be null");
        this.entityClass = Objects.requireNonNull(entityClass, "entityClass must not be null");
        this.mongoMappingConvertor = Objects.requireNonNull(mongoMappingConvertor, "mongoMappingConvertor must not be null");
        this.idGenerator = idGenerator;
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
        long currentPage = pageDTO.getCurrentPage() != null ? pageDTO.getCurrentPage() : 1L;
        long pageSize = pageDTO.getPageSize() != null ? pageDTO.getPageSize() : 10L;

        // 1. 查总数
        long total = databaseSupplier.get().getCollection(collectionName).countDocuments(filter);
        pageDTO.setTotal(total);
        pageDTO.setTotalPage(total == 0 ? 0 : (total + pageSize - 1) / pageSize);

        // 2. skip + limit
        int skip = (int) ((currentPage - 1) * pageSize);
        int limit = (int) pageSize;
        FindIterable<Document> iterable = databaseSupplier.get().getCollection(collectionName)
                .find(filter).skip(skip).limit(limit);

        // 3. 投影
        applyProjection(wrapper, iterable);

        // 4. 排序（优先 Lambda 排序，其次 pageDTO.orderBy 字符串排序）
        Document sortDoc = buildSort(wrapper, pageDTO);
        if (sortDoc != null) {
            iterable.sort(sortDoc);
        }

        // 5. 读数据
        List<T> records = new ArrayList<>();
        for (Document doc : iterable) {
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
                String mongoField = mongoMappingConvertor.resolveMongoFieldName(resolveClass, ob.getJavaFieldName());
                sort.append(mongoField, ob.isAscending() ? 1 : -1);
            }
            return sort;
        }
        // Fallback：PageDTO 字符串排序
        if (pageDTO.getOrderBy() != null && !pageDTO.getOrderBy().isEmpty()) {
            Document sort = new Document();
            for (String field : pageDTO.getOrderBy()) {
                String mongoField = mongoMappingConvertor.resolveMongoFieldName(entityClass, field);
                sort.append(mongoField, pageDTO.isOrderByAsc() ? 1 : -1);
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

    // ---- update ----

    @Override
    public long updateById(T entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        fillDate(entity, false);
        Document doc = mongoMappingConvertor.write(entity);
        Object id = doc.remove("_id");
        if (id == null) {
            throw new IllegalArgumentException("Entity must have an non-null id for updateById");
        }
        Object queryId = (id instanceof String && ObjectId.isValid((String) id)) ? new ObjectId((String) id) : id;
        Document updateDoc = new Document("$set", doc);
        UpdateResult updateResult = databaseSupplier.get().getCollection(collectionName)
                .updateOne(Filters.eq("_id", queryId), updateDoc);
        return updateResult.getModifiedCount();
    }

    @Override
    public <R> long update(SFunction<T, R> field, R value, T entity) {
        Objects.requireNonNull(field, "field must not be null");
        Objects.requireNonNull(entity, "entity must not be null");
        Document filter = buildFilterFromLambda(field, value);
        fillDate(entity, false);
        Document doc = mongoMappingConvertor.write(entity);
        doc.remove("_id");
        Document updateDoc = new Document("$set", doc);
        UpdateResult updateResult = databaseSupplier.get().getCollection(collectionName)
                .updateMany(filter, updateDoc);
        return updateResult.getModifiedCount();
    }

    @Override
    public long update(LambdaQueryWrapper<T> wrapper, T entity) {
        Objects.requireNonNull(wrapper, "wrapper must not be null");
        Objects.requireNonNull(entity, "entity must not be null");
        requireNonEmptyWrapper(wrapper, "update");
        ensureEntityClass(wrapper);
        Bson filter = MongoBsonRenderer.render(wrapper, mongoMappingConvertor);
        fillDate(entity, false);
        Document doc = mongoMappingConvertor.write(entity);
        doc.remove("_id");
        Document updateDoc = new Document("$set", doc);
        UpdateResult updateResult = databaseSupplier.get().getCollection(collectionName)
                .updateMany(filter, updateDoc);
        return updateResult.getModifiedCount();
    }

    @Override
    public long updateAll(T entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        fillDate(entity, false);
        Document doc = mongoMappingConvertor.write(entity);
        doc.remove("_id");
        Document updateDoc = new Document("$set", doc);
        UpdateResult updateResult = databaseSupplier.get().getCollection(collectionName)
                .updateMany(new Document(), updateDoc);
        return updateResult.getModifiedCount();
    }

    // ---- upsert ----

    @Override
    public long upsertById(T entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        fillDate(entity, true);
        Document doc = mongoMappingConvertor.write(entity);
        Object id = doc.remove("_id");
        if (id == null) {
            throw new IllegalArgumentException("Entity must have an non-null id for upsertById");
        }
        Object queryId = (id instanceof String && ObjectId.isValid((String) id)) ? new ObjectId((String) id) : id;
        Document updateDoc = new Document("$set", doc);
        UpdateResult updateResult = databaseSupplier.get().getCollection(collectionName)
                .updateOne(Filters.eq("_id", queryId), updateDoc, new UpdateOptions().upsert(true));
        return updateResult.getModifiedCount();
    }

    @Override
    public <R> long upsert(SFunction<T, R> field, R value, T entity) {
        Objects.requireNonNull(field, "field must not be null");
        Objects.requireNonNull(entity, "entity must not be null");
        Document filter = buildFilterFromLambda(field, value);
        fillDate(entity, true);
        Document doc = mongoMappingConvertor.write(entity);
        doc.remove("_id");
        Document updateDoc = new Document("$set", doc);
        UpdateResult updateResult = databaseSupplier.get().getCollection(collectionName)
                .updateMany(filter, updateDoc, new UpdateOptions().upsert(true));
        return updateResult.getModifiedCount();
    }

    @Override
    public long upsert(LambdaQueryWrapper<T> wrapper, T entity) {
        Objects.requireNonNull(wrapper, "wrapper must not be null");
        Objects.requireNonNull(entity, "entity must not be null");
        requireNonEmptyWrapper(wrapper, "upsert");
        ensureEntityClass(wrapper);
        Bson filter = MongoBsonRenderer.render(wrapper, mongoMappingConvertor);
        fillDate(entity, true);
        Document doc = mongoMappingConvertor.write(entity);
        doc.remove("_id");
        Document updateDoc = new Document("$set", doc);
        UpdateResult updateResult = databaseSupplier.get().getCollection(collectionName)
                .updateMany(filter, updateDoc, new UpdateOptions().upsert(true));
        return updateResult.getModifiedCount();
    }

    // ---- delete ----

    @Override
    public long deleteById(ID id) {
        Objects.requireNonNull(id, "id must not be null");
        Object queryId = convertIdIfNecessary(id);
        DeleteResult result = databaseSupplier.get().getCollection(collectionName)
                .deleteOne(Filters.eq("_id", queryId));
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
    public <R> long delete(SFunction<T, R> field, R value) {
        Objects.requireNonNull(field, "field must not be null");
        Document filter = buildFilterFromLambda(field, value);
        DeleteResult result = databaseSupplier.get().getCollection(collectionName).deleteMany(filter);
        return result.getDeletedCount();
    }

    @Override
    public long delete(LambdaQueryWrapper<T> wrapper) {
        Objects.requireNonNull(wrapper, "wrapper must not be null");
        requireNonEmptyWrapper(wrapper, "delete");
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

    // ---- internal helpers ----

    private void requireNonEmptyWrapper(LambdaQueryWrapper<T> wrapper, String operation) {
        if (wrapper.getConditions().isEmpty()) {
            throw new IllegalArgumentException(
                    operation + " requires at least one condition. Use " + operation + "All() to operate on all documents.");
        }
    }

    private void requireNonEmptyQuery(Document query, String operation) {
        if (query.isEmpty()) {
            throw new IllegalArgumentException(
                    operation + " requires at least one non-null field. Use " + operation + "All() to operate on all documents.");
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
                    idField.set(entity, UlidCreator.getUlid().toString());
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

    private void fillDate(T entity, boolean isInsert){
        try{
            if (isInsert){
                Field createDateField = mongoMappingConvertor.getCreateDateField(entityClass);
                if (createDateField != null){
                    createDateField.setAccessible(true);
                    if (createDateField.get(entity) == null){
                        String pattern = createDateField.getAnnotation(CreateDate.class).pattern();
                        createDateField.set(entity, DateValueGenerator.generateCurrentDate(createDateField.getType(), pattern));

                    }
                }
            }
            Field updateDateField = mongoMappingConvertor.getUpdateDateField(entityClass);
            if (updateDateField != null){
                updateDateField.setAccessible(true);
                String pattern = updateDateField.getAnnotation(UpdateDate.class).pattern();
                updateDateField.set(entity, DateValueGenerator.generateCurrentDate(updateDateField.getType(), pattern));
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
            String mongoField = mongoMappingConvertor.resolveMongoFieldName(resolveClass, pf.getJavaFieldName());
            mongoFields.add(mongoField);
        }
        return mongoFields;
    }

    private <R> Document buildFilterFromLambda(SFunction<T, R> field, R value) {
        String javaFieldName = ReflectUtil.getFieldNameFromLambda(field);
        String mongoFieldName = mongoMappingConvertor.resolveMongoFieldName(entityClass, javaFieldName);
        Object queryValue = value;
        if ("_id".equals(mongoFieldName) && shouldConvertToObjectId()
                && value instanceof String && ObjectId.isValid((String) value)) {
            queryValue = new ObjectId((String) value);
        }
        return new Document(mongoFieldName, queryValue);
    }

 }

