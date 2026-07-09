package com.github.eacryo.mongoflex.lambda;

import java.util.Objects;

public final class Condition {

    private final String field;
    private final Operator operator;
    private final Object value;

    public Condition(String field, Operator operator, Object value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Condition)) return false;
        Condition condition = (Condition) o;
        return Objects.equals(field, condition.field)
                && operator == condition.operator
                && Objects.equals(value, condition.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, operator, value);
    }

    @Override
    public String toString() {
        return "Condition[" +
                "field=" + field + ", " +
                "operator=" + operator + ", " +
                "value=" + value + ']';
    }

}

