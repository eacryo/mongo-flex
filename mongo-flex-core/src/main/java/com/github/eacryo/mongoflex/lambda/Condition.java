package com.github.eacryo.mongoflex.lambda;

import java.util.Objects;

public final class Condition {

    private final String field;
    private final Operator operator;
    private final Object value;
    private final Class<?> implClass;

    /**
     * Create a normal condition (non-OR-separator) / 创建普通条件（非 OR 分割符）
     *
     * @param field     Java field name — null only allowed for NOT operator (logical negation of sub-query)
     *                  has no single-field context) / Java 字段名——仅 NOT 操作符允许为 null（逻辑取反子查询，无单字段上下文）
     * @param operator  query operator, must not be null / 查询操作符，不可为 null
     * @param value     condition value (may be null for IS_NULL/IS_NOT_NULL) / 条件值（IS_NULL/IS_NOT_NULL 可为 null）
     * @param implClass field declaring class, may be null for string-based queries / 字段声明类，字符串查询可为 null
     */
    public Condition(String field, Operator operator, Object value, Class<?> implClass) {
        this.operator = Objects.requireNonNull(operator, "operator must not be null");
        this.field = (field == null && operator != Operator.NOT)
                ? Objects.requireNonNull(field, "field must not be null")
                : field;
        this.value = value;
        this.implClass = implClass;
    }

    Condition() {
        this.field = null;
        this.operator = null;
        this.value = null;
        this.implClass = null;
    }

    public String field() {
        return field;
    }

    public Operator operator() {
        return operator;
    }

    public Object value() {
        return value;
    }

    public Class<?> implClass() {
        return implClass;
    }

    public boolean isOrSeparator() {
        return field == null && operator == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Condition)) return false;
        Condition condition = (Condition) o;
        return Objects.equals(field, condition.field)
                && operator == condition.operator
                && Objects.equals(value, condition.value)
                && Objects.equals(implClass, condition.implClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, operator, value, implClass);
    }

    @Override
    public String toString() {
        return "Condition[" +
                "field=" + field + ", " +
                "operator=" + operator + ", " +
                "value=" + value + ", " +
                "implClass=" + (implClass != null ? implClass.getSimpleName() : "null") + ", " +
                "isOrSeparator=" + isOrSeparator() + ']';
    }

}
