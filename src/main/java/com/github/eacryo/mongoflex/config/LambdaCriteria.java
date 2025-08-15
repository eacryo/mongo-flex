package com.github.eacryo.mongoflex.config;

import com.github.eacryo.mongoflex.util.ReflectUtil;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.util.StringUtils;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

public class LambdaCriteria {
    public static <T,R> Criteria where(SFunction<T, R> func) {
        return new Criteria(ReflectUtil.getFieldName(func));
    }
}
