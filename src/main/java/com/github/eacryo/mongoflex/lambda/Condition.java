package com.github.eacryo.mongoflex.lambda;


public record Condition(
        String field,
        Operator operator,
        Object value
) {
}