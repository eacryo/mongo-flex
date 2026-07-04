package com.github.eacryo.mongoflex.lambda;

import com.github.eacryo.mongoflex.util.SFunction;
import com.github.eacryo.mongoflex.util.ReflectUtil;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.EQ, value));
        return this;
    }

    public <R> LambdaQueryWrapper<T> ne(SFunction<T, R> field, R value) {
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.NE, value));
        return this;
    }

    public <R> LambdaQueryWrapper<T> gt(SFunction<T, R> field, R value) {
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.GT, value));
        return this;
    }

    public <R> LambdaQueryWrapper<T> lt(SFunction<T, R> field, R value) {
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.LT, value));
        return this;
    }

    public <R> LambdaQueryWrapper<T> gte(SFunction<T, R> field, R value) {
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.GTE, value));
        return this;
    }

    public <R> LambdaQueryWrapper<T> lte(SFunction<T, R> field, R value) {
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.LTE, value));
        return this;
    }

    public <R> LambdaQueryWrapper<T> regex(SFunction<T, R> field, String pattern) {
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.REGEX, pattern));
        return this;
    }

    public <R> LambdaQueryWrapper<T> in(SFunction<T, R> field, Collection<?> values) {
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.IN, values));
        return this;
    }

    public <R> LambdaQueryWrapper<T> nin(SFunction<T, R> field, Collection<?> values) {
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.NIN, values));
        return this;
    }

    public LambdaQueryWrapper<T> exists(SFunction<T, ?> field, boolean value) {
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.EXISTS, value));
        return this;
    }

    public <R> LambdaQueryWrapper<T> all(SFunction<T, R> field, Collection<?> values) {
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.ALL, values));
        return this;
    }

    public LambdaQueryWrapper<T> size(SFunction<T, ?> field, int value) {
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.SIZE, value));
        return this;
    }

    public LambdaQueryWrapper<T> elemMatch(SFunction<T, ?> field, LambdaQueryWrapper<?> subWrapper) {
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        if (entityClass != null && subWrapper.getEntityClass() == null) {
            Class<?> subEntityClass = resolveElementTypeFromField(entityClass, javaField);
            if (subEntityClass != null) {
                subWrapper.setEntityClass((Class) subEntityClass);
            }
        }
        conditions.add(new Condition(javaField, Operator.ELEM_MATCH, subWrapper));
        return this;
    }

    private Class<?> resolveElementTypeFromField(Class<?> clazz, String fieldName) {
        try {
            java.lang.reflect.Field field = ReflectUtil.getFiled(clazz, fieldName);
            if (field != null) {
                Type genericType = field.getGenericType();
                if (genericType instanceof ParameterizedType pt) {
                    Type[] args = pt.getActualTypeArguments();
                    if (args.length > 0 && args[0] instanceof Class) {
                        return (Class<?>) args[0];
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public List<Condition> getConditions() {
        return conditions;
    }
}
