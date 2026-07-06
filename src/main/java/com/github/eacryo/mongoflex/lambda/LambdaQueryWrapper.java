package com.github.eacryo.mongoflex.lambda;

import com.github.eacryo.mongoflex.util.SFunction;
import com.github.eacryo.mongoflex.util.ReflectUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A Lambda-based query wrapper similar to MyBatis-Plus's LambdaQueryWrapper.
 * Example usage:
 * LambdaQueryWrapper<User> w = new LambdaQueryWrapper<>(User.class);
 * w.eq(User::getUserName, "Tom");
 */
public class LambdaQueryWrapper<T> {

    private final List<Condition> conditions = new ArrayList<>();
    private Class<T> entityClass;

    public LambdaQueryWrapper() {
    }

    public LambdaQueryWrapper(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    public Class<T> getEntityClass() {
        return entityClass;
    }

    public void setEntityClass(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    public <R> LambdaQueryWrapper<T> eq(SFunction<T, R> field, R value) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.EQ, value));
        return this;
    }

    public <R> LambdaQueryWrapper<T> ne(SFunction<T, R> field, R value) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.NE, value));
        return this;
    }

    public <R> LambdaQueryWrapper<T> gt(SFunction<T, R> field, R value) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.GT, value));
        return this;
    }

    public <R> LambdaQueryWrapper<T> lt(SFunction<T, R> field, R value) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.LT, value));
        return this;
    }

    public <R> LambdaQueryWrapper<T> gte(SFunction<T, R> field, R value) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.GTE, value));
        return this;
    }

    public <R> LambdaQueryWrapper<T> lte(SFunction<T, R> field, R value) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.LTE, value));
        return this;
    }

    public <R> LambdaQueryWrapper<T> regex(SFunction<T, R> field, String pattern) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.REGEX, pattern));
        return this;
    }

    public <R> LambdaQueryWrapper<T> in(SFunction<T, R> field, Collection<?> values) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.IN, values));
        return this;
    }

    public <R> LambdaQueryWrapper<T> nin(SFunction<T, R> field, Collection<?> values) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.NIN, values));
        return this;
    }

    public LambdaQueryWrapper<T> exists(SFunction<T, ?> field, boolean value) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.EXISTS, value));
        return this;
    }

    public <R> LambdaQueryWrapper<T> all(SFunction<T, R> field, Collection<?> values) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.ALL, values));
        return this;
    }

    public LambdaQueryWrapper<T> size(SFunction<T, ?> field, int value) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.SIZE, value));
        return this;
    }

    public LambdaQueryWrapper<T> elemMatch(SFunction<T, ?> field, LambdaQueryWrapper<?> subWrapper) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.ELEM_MATCH, subWrapper));
        return this;
    }

    public List<Condition> getConditions() {
        return conditions;
    }
}
