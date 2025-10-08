package com.github.eacryo.mongoflex.v2;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.InsertOneResult;
import org.bson.BsonValue;
import org.bson.Document;

/**
 * This class should not be exposed to external use
 *
 * @param <T>
 * @param <ID>
 */
public class BaseRepositoryV2<T, ID> implements IBaseRepositoryV2<T, ID> {

    private final MongoDatabase mongoDatabase;
    private final String collectionName;
    private final Class<T> entityClass;
    private final JacksonDocumentConverter jacksonDocumentConverter;
    private final MongoCollection<Document> collection;

    public BaseRepositoryV2(MongoDatabase mongoDatabase, String collectionName, Class<T> entityClass,
                            JacksonDocumentConverter jacksonDocumentConverter) {
        this.mongoDatabase = mongoDatabase;
        this.collectionName = collectionName;
        this.entityClass = entityClass;
        this.jacksonDocumentConverter = jacksonDocumentConverter;
        this.collection = mongoDatabase.getCollection(collectionName);
    }

    @Override
    public T insert(T entity) {
        Document document = jacksonDocumentConverter.convert(entity);
        InsertOneResult insertOneResult = collection.insertOne(document);
        BsonValue insertedId = insertOneResult.getInsertedId();
        return null;
    }

    @Override
    public T findById(ID id) {
        Document document = collection.find(Filters.eq("_id", id)).first();
        return jacksonDocumentConverter.convert(document, entityClass);
    }

    @Override
    public T findByEntity(T entity) {
        Document document = collection.find(jacksonDocumentConverter.convert(entity)).first();
        return jacksonDocumentConverter.convert(document, entityClass);
    }

    @Override
    public long count() {
        return collection.countDocuments();
    }

    @Override
    public long count(T entity) {
        return collection.countDocuments(jacksonDocumentConverter.convert(entity));
    }
}
