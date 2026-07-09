package com.github.eacryo.mongoflex.util;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

public class ReflectUtil {

    /**
     * 使用 ClassValue 缓存 writeReplace Method，一个 lambda class 只反射一次。
     * ClassValue 随 Class 卸载自动清理，无内存泄漏风险。
     */
    private static final ClassValue<Method> WRITE_REPLACE_CACHE = new ClassValue<Method>() {
        @Override
        protected Method computeValue(Class<?> type) {
            try {
                Method method = type.getDeclaredMethod("writeReplace");
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Failed to resolve writeReplace on lambda class: " + type.getName(), e);
            }
        }
    };

    public static <T,R> String getFieldNameFromLambda(SFunction<T,R> func) {
        try {
            Method writeReplace = WRITE_REPLACE_CACHE.get(func.getClass());
            SerializedLambda serializedLambda = (SerializedLambda) writeReplace.invoke(func);
            String methodName = serializedLambda.getImplMethodName();
            String fieldName;
            if (methodName.startsWith("get") && methodName.length() > 3) {
                fieldName = methodName.substring(3);
            } else if (methodName.startsWith("is") && methodName.length() > 2) {
                fieldName = methodName.substring(2);
            } else {
                fieldName = methodName;
            }
            return Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve field name from lambda", e);
        }
    }

}

