package com.github.eacryo.mongoflex.util;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

public class ReflectUtil {

    private static final ConcurrentHashMap<SFunction<?,?>,String> LAMBDA_FIELD_NAME_CACHE = new ConcurrentHashMap<>();

    public static <T,R> String getFieldNameFromLambda(SFunction<T,R> func){
        return LAMBDA_FIELD_NAME_CACHE.computeIfAbsent(func, f -> {
            try{
                Method writeReplace = f.getClass().getDeclaredMethod("writeReplace");
                writeReplace.setAccessible(true);
                SerializedLambda serializedLambda = (SerializedLambda) writeReplace.invoke(f);
                String methodName =  serializedLambda.getImplMethodName();
                String fieldName;
                if (methodName.startsWith("get") && methodName.length() > 3) {
                    fieldName = methodName.substring(3);
                } else if (methodName.startsWith("is") && methodName.length() > 2) {
                    fieldName = methodName.substring(2);
                } else {
                    fieldName = methodName;
                }
                return Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);
            } catch (Exception e){
                throw new RuntimeException("Failed to resolve field name from lambda", e);
            }
        });
    }

}

