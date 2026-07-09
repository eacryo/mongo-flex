package com.github.eacryo.mongoflex.v2;

import com.github.eacryo.mongoflex.annotation.CollectionId;
import com.github.eacryo.mongoflex.annotation.CreateDate;
import com.github.eacryo.mongoflex.annotation.UpdateDate;
import com.github.eacryo.mongoflex.config.IdGenerator;
import com.github.eacryo.mongoflex.constant.IdType;
import com.github.eacryo.mongoflex.convertor.MongoMappingConvertor;
import com.github.eacryo.mongoflex.lambda.LambdaQueryWrapper;
import com.github.eacryo.mongoflex.lambda.MongoBsonRenderer;
import com.github.eacryo.mongoflex.util.DateValueGenerator;
import com.github.eacryo.mongoflex.util.ReflectUtil;
import com.github.eacryo.mongoflex.util.SFunction;
import com.github.f4b6a3.ulid.UlidCreator;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
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


    public SimpleMongoRepository(Supplier<MongoDatabase> databaseSupplier, String collectionName, Class<T> entityClass,
                                 MongoMappingConvertor mongoMappingConvertor, IdGenerator<?> idGenerator) {
        this.databaseSupplier = databaseSupplier;
        this.collectionName = collectionName;
        this.entityClass = entityClass;
        this.mongoMappingConvertor = mongoMappingConvertor;
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
        Document document = databaseSupplier.get().getCollection(collectionName).find(filter).first();
        return mongoMappingConvertor.read(document, entityClass);
    }

    @Override
    public List<T> findList(LambdaQueryWrapper<T> wrapper) {
        Objects.requireNonNull(wrapper, "wrapper must not be null");
        ensureEntityClass(wrapper);
        Bson filter = MongoBsonRenderer.render(wrapper, mongoMappingConvertor);
        List<Document> docs = databaseSupplier.get().getCollection(collectionName).find(filter).into(new ArrayList<>());
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
        if (idType == IdType.NONE){
            //NONE:let mongodb generate
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
                    if (idGenerator == null) {
                        throw new IllegalArgumentException("Id generator is required for INPUT id type");
                    }
                    idField.set(entity, idGenerator.create());
                    break;
                default : break;
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to set ID field: " + idField.getName(), e);
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
        } catch (Exception e){
            throw new RuntimeException("Failed to set date fields", e);
        }

    }

    private Object convertIdIfNecessary(ID id){
        if (id instanceof String && ObjectId.isValid((String) id)){
            return new ObjectId((String) id);
        }
        return id;
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

    private <R> Document buildFilterFromLambda(SFunction<T, R> field, R value) {
        String javaFieldName = ReflectUtil.getFieldNameFromLambda(field);
        String mongoFieldName = mongoMappingConvertor.resolveMongoFieldName(entityClass, javaFieldName);
        Object queryValue = value;
        if ("_id".equals(mongoFieldName) && value instanceof String && ObjectId.isValid((String) value)) {
            queryValue = new ObjectId((String) value);
        }
        return new Document(mongoFieldName, queryValue);
    }

 }

