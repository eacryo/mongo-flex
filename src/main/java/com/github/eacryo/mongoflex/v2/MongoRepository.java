package com.github.eacryo.mongoflex.v2;


import com.github.eacryo.mongoflex.util.SFunction;

public interface MongoRepository<T,ID> {
    T insert(T entity);

    T findById(ID id);

    T findOneByEntity(T entity);

    <R> T findOne(SFunction<T, R> field, R value);

    long count();

    long count(T entity);

    <R> long count(SFunction<T, R> field, R value);

    long updateById(T entity);

    <R> long update(SFunction<T, R> field, R value, T entity);

    long deleteById(ID id);

    long deleteByEntity(T entity);

    <R> long delete(SFunction<T, R> field, R value);
}
