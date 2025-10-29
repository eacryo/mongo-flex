package com.github.eacryo.mongoflex.v2;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.InsertOneResult;
import org.bson.BsonValue;
import org.bson.Document;

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
    private final JacksonDocumentConverter jacksonDocumentConverter;
    //private final MongoCollection<Document> collection;

    public SimpleMongoRepository(Supplier<MongoDatabase> databaseSupplier, String collectionName, Class<T> entityClass,
                                 JacksonDocumentConverter jacksonDocumentConverter) {
        //this.mongoDatabase = mongoDatabase;
        this.databaseSupplier = databaseSupplier;
        this.collectionName = collectionName;
        this.entityClass = entityClass;
        this.jacksonDocumentConverter = jacksonDocumentConverter;
        //this.collection = mongoDatabase.getCollection(collectionName);
        //this.collection = databaseSupplier.get().getCollection(collectionName);

    }

    @Override
    public T insert(T entity) {
        Document document = jacksonDocumentConverter.convert(entity);
        InsertOneResult insertOneResult = databaseSupplier.get().getCollection(collectionName).insertOne(document);
        BsonValue insertedId = insertOneResult.getInsertedId();
        return null;
    }

    @Override
    public T findById(ID id) {
        Document document = databaseSupplier.get().getCollection(collectionName).find(Filters.eq("_id", id)).first();
        return jacksonDocumentConverter.convert(document, entityClass);
    }

    @Override
    public T findByEntity(T entity) {
        Document document = databaseSupplier.get().getCollection(collectionName).find(jacksonDocumentConverter.convert(entity)).first();
        return jacksonDocumentConverter.convert(document, entityClass);
    }

    @Override
    public long count() {
        return databaseSupplier.get().getCollection(collectionName).countDocuments();
    }

    @Override
    public long count(T entity) {
        return databaseSupplier.get().getCollection(collectionName).countDocuments(jacksonDocumentConverter.convert(entity));
    }
}
