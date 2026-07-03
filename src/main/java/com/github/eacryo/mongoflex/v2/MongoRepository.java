package com.github.eacryo.mongoflex.v2;


import com.github.eacryo.mongoflex.lambda.LambdaQueryWrapper;
import com.github.eacryo.mongoflex.util.SFunction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extended repository interface with LambdaQueryWrapper support.
 */
public interface MongoRepository<T,ID> {
    T insert(T entity);

    T findById(ID id);

    T findOneByEntity(T entity);

    <R> T findOne(SFunction<T, R> field, R value);

    // LambdaQueryWrapper convenience methods
    T findOne(LambdaQueryWrapper<T> wrapper);

    List<T> findList(LambdaQueryWrapper<T> wrapper);

    long count(LambdaQueryWrapper<T> wrapper);

    long update(LambdaQueryWrapper<T> wrapper, T entity);

    long delete(LambdaQueryWrapper<T> wrapper);

    long count();

    long count(T entity);

    <R> long count(SFunction<T, R> field, R value);

    long updateById(T entity);

    <R> long update(SFunction<T, R> field, R value, T entity);

    long deleteById(ID id);

    long deleteByEntity(T entity);

    <R> long delete(SFunction<T, R> field, R value);
}
