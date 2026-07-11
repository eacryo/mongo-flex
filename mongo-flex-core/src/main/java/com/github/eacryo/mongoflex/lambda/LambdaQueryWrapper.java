package com.github.eacryo.mongoflex.lambda;

import com.github.eacryo.mongoflex.util.SFunction;
import com.github.eacryo.mongoflex.util.ReflectUtil;

import java.util.ArrayList;
import java.util.Arrays;
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
        conditions.add(new Condition(javaField, Operator.EQ, value, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    public <R> LambdaQueryWrapper<T> ne(SFunction<T, R> field, R value) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.NE, value, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    public <R> LambdaQueryWrapper<T> gt(SFunction<T, R> field, R value) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.GT, value, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    public <R> LambdaQueryWrapper<T> lt(SFunction<T, R> field, R value) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.LT, value, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    public <R> LambdaQueryWrapper<T> gte(SFunction<T, R> field, R value) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.GTE, value, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    public <R> LambdaQueryWrapper<T> lte(SFunction<T, R> field, R value) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.LTE, value, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    public <R> LambdaQueryWrapper<T> regex(SFunction<T, R> field, String pattern) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.REGEX, pattern, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    public <R> LambdaQueryWrapper<T> in(SFunction<T, R> field, Collection<?> values) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.IN, values, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    public <R> LambdaQueryWrapper<T> nin(SFunction<T, R> field, Collection<?> values) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.NIN, values, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    public LambdaQueryWrapper<T> exists(SFunction<T, ?> field, boolean value) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.EXISTS, value, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    public <R> LambdaQueryWrapper<T> all(SFunction<T, R> field, Collection<?> values) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.ALL, values, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    public LambdaQueryWrapper<T> size(SFunction<T, ?> field, int value) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.SIZE, value, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    public LambdaQueryWrapper<T> elemMatch(SFunction<T, ?> field, LambdaQueryWrapper<?> subWrapper) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.ELEM_MATCH, subWrapper, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    /**
     * SQL-style LIKE: supports * and % as wildcards, automatically converted to regex.
     * Example: like(User::getName, "*Tom*") → { name: { $regex: ".*Tom.*" } }
     */
    public <R> LambdaQueryWrapper<T> like(SFunction<T, R> field, String pattern) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.LIKE, pattern, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    /**
     * SQL-style NOT LIKE: negated LIKE, same wildcard conversion.
     */
    public <R> LambdaQueryWrapper<T> notLike(SFunction<T, R> field, String pattern) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.NOT_LIKE, pattern, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    /**
     * Range query: field BETWEEN start AND end → { field: { $gte: start, $lte: end } }
     */
    public <R> LambdaQueryWrapper<T> between(SFunction<T, R> field, R start, R end) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.BETWEEN, Arrays.asList(start, end), ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    /**
     * IS NULL → { field: { $exists: false } }
     */
    public LambdaQueryWrapper<T> isNull(SFunction<T, ?> field) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.IS_NULL, null, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    /**
     * IS NOT NULL → { field: { $exists: true } }
     */
    public LambdaQueryWrapper<T> isNotNull(SFunction<T, ?> field) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.IS_NOT_NULL, null, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    /**
     * Logical NOT: negates a sub-query.
     * No field parameter — implClass is null.
     * Example: not(w -> w.eq(User::getName, "Tom")) → { $nor: [...] }
     */
    public LambdaQueryWrapper<T> not(LambdaQueryWrapper<T> subWrapper) {
        Objects.requireNonNull(subWrapper, "subWrapper must not be null");
        conditions.add(new Condition(null, Operator.NOT, subWrapper, null));
        return this;
    }

    /**
     * Modulo: { field: { $mod: [divisor, remainder] } }
     */
    public <R> LambdaQueryWrapper<T> mod(SFunction<T, R> field, int divisor, int remainder) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.MOD, Arrays.asList(divisor, remainder), ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    /**
     * BSON type check: { field: { $type: "string" } } or { field: { $type: "int" } }
     * Common type names: "string", "int", "double", "array", "objectId", "bool", "date"
     */
    public LambdaQueryWrapper<T> type(SFunction<T, ?> field, String bsonTypeName) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.TYPE, bsonTypeName, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public LambdaQueryWrapper<T> or() {
        conditions.add(new Condition());
        return this;
    }

    public LambdaQueryWrapper<T> or(LambdaQueryWrapper<T> orWrapper) {
        Objects.requireNonNull(orWrapper, "orWrapper must not be null");
        conditions.add(new Condition());
        conditions.addAll(orWrapper.getConditions());
        return this;
    }
}
