package com.github.eacryo.mongoflex.v2;

import com.github.eacryo.mongoflex.annotation.CollectionId;
import com.github.eacryo.mongoflex.config.IdGenerator;
import com.github.eacryo.mongoflex.constant.IdType;
import com.github.eacryo.mongoflex.convertor.MongoMappingConvertor;
import com.github.eacryo.mongoflex.util.ReflectUtil;
import com.github.f4b6a3.ulid.UlidCreator;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.InsertOneResult;
import org.bson.BsonValue;
import org.bson.Document;

import java.lang.reflect.Field;
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

    //private final MongoDatabase mongoDatabase;
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

    @Override
    public T insert(T entity) {
        fillId(entity);
        Document document = mongoMappingConvertor.write(entity);
        InsertOneResult insertOneResult = databaseSupplier.get().getCollection(collectionName).insertOne(document);
        BsonValue insertedId = insertOneResult.getInsertedId();
        return null;
    }

    private void fillId(T entity){
        Field idField = ReflectUtil.getCachedIdField(entityClass.getClass());
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

    @Override
    public T findById(ID id) {
        Document document = databaseSupplier.get().getCollection(collectionName).find(Filters.eq("_id", id)).first();
        return mongoMappingConvertor.read(document, entityClass);
    }

    @Override
    public T findByEntity(T entity) {
        Document document = databaseSupplier.get().getCollection(collectionName).find(mongoMappingConvertor.write(entity)).first();
        return mongoMappingConvertor.read(document, entityClass);
    }

    @Override
    public long count() {
        return databaseSupplier.get().getCollection(collectionName).countDocuments();
    }

    @Override
    public long count(T entity) {
        return databaseSupplier.get().getCollection(collectionName).countDocuments(mongoMappingConvertor.write(entity));
    }
}
