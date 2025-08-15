package com.github.eacryo.mongoflex.config;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.util.StringUtils;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

public class LambdaCriteria {
    public static <T,R> Criteria where(SFunction<T, R> func) {
        return new Criteria(getFieldName(func));
    }


    // 获取方法引用对应的字段名
    private static <T,R> String getFieldName(SFunction<T, R> func) {
        try {
            Method method = func.getClass().getDeclaredMethod("writeReplace");
            method.setAccessible(true);
            SerializedLambda lambda = (SerializedLambda) method.invoke(func);
            String methodName = lambda.getImplMethodName();

            // 解析getter方法名
            if (methodName.startsWith("get")) {
                return StringUtils.uncapitalize(methodName.substring(3));
            } else if (methodName.startsWith("is")) {
                return StringUtils.uncapitalize(methodName.substring(2));
            }
            return methodName;
        } catch (Exception e) {
            throw new RuntimeException("解析字段名失败: " + e.getMessage(), e);
        }
    }
}
