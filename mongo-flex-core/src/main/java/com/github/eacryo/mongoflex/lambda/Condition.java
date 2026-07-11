package com.github.eacryo.mongoflex.lambda;

import java.util.Objects;

public final class Condition {

    private final String field;
    private final Operator operator;
    private final Object value;
    private final Class<?> implClass;

    public Condition(String field, Operator operator, Object value, Class<?> implClass) {
        this.field = field;
        this.operator = operator;
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
