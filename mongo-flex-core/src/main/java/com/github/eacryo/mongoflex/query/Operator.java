package com.github.eacryo.mongoflex.query;

public enum Operator {

    EQ,

    NE,

    GT,

    LT,

    GTE,

    LTE,

    REGEX,

    LIKE,

    NOT_LIKE,

    IN,

    NIN,

    EXISTS,

    ALL,

    SIZE,

    ELEM_MATCH,

    NOT,

    BETWEEN,

    IS_NULL,

    IS_NOT_NULL,

    MOD,

    TYPE,

    /** Nested AND group: the condition value is a sub-wrapper ANDed with sibling conditions. / 嵌套 AND 分组：条件值为子 wrapper，与同级条件按 AND 组合 */
    AND,

    /** Nested OR group: the condition value is a sub-wrapper ORed with sibling conditions. / 嵌套 OR 分组：条件值为子 wrapper，与同级条件按 OR 组合 */
    OR

}