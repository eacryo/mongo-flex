package com.github.eacryo.mongoflex.v2;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.InsertOneResult;
import org.bson.BsonValue;
import org.bson.Document;

public class BaseRepositoryV2<T,ID> implements IBaseRepositoryV2<T,ID> {

    private final MongoDatabase mongoDatabase;
    private final String collectionName;
    private final Class<T> entityClass;
    private final JacksonDocumentConverter jacksonDocumentConverter;

    public BaseRepositoryV2(MongoDatabase mongoDatabase, String collectionName, Class<T> entityClass,
                            JacksonDocumentConverter jacksonDocumentConverter) {
        this.mongoDatabase = mongoDatabase;
        this.collectionName = collectionName;
        this.entityClass = entityClass;
        this.jacksonDocumentConverter = jacksonDocumentConverter;
    }

    @Override
    public T insert(T entity) {
        MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);
        Document document = jacksonDocumentConverter.convert(entity);
        InsertOneResult insertOneResult = collection.insertOne(document);
        BsonValue insertedId = insertOneResult.getInsertedId();
        return null;
    }

    @Override
    public T findById(ID id) {
        MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);
        Document document = collection.find(Filters.eq("_id",id)).first();
        return jacksonDocumentConverter.convert(document,entityClass);
    }

    @Override
    public T findByEntity(T entity) {
        MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);
        Document document = collection.find(Filters.eq("name","Ganyu")).first();
        return jacksonDocumentConverter.convert(document,entityClass);
    }
}
