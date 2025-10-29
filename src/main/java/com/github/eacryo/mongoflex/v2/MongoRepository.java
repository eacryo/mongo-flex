package com.github.eacryo.mongoflex.v2;

public interface MongoRepository<T,ID> {
    T insert(T entity);

    T findById(ID id);

    T findByEntity(T entity);

    long count();

    long count(T entity);
}
