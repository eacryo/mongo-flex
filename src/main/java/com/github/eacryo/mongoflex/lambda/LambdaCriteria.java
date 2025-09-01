package com.github.eacryo.mongoflex.lambda;

import com.github.eacryo.mongoflex.util.ReflectUtil;
import org.springframework.data.mongodb.core.query.Criteria;

public class LambdaCriteria {
    public static <T,R> Criteria where(SFunction<T, R> func) {
        return new Criteria(ReflectUtil.getFieldName(func));
    }
}
