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
            SerializedLambda serializedLambda = getSerializedLambda(func);
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

    /**
     * 从 lambda 方法引用中提取实际声明该方法的类。
     * 例如 LiyueCharacter::getIsAdeptus 返回 LiyueCharacter.class，
     * 用于字段名映射时使用正确的 ClassFieldMetaData。
     */
    public static <T, R> Class<?> getImplClassFromLambda(SFunction<T, R> func) {
        try {
            SerializedLambda sl = getSerializedLambda(func);
            String implClass = sl.getImplClass().replace('/', '.');
            return Class.forName(implClass);
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve impl class from lambda", e);
        }
    }

    private static <T, R> SerializedLambda getSerializedLambda(SFunction<T, R> func) throws Exception {
        Method writeReplace = WRITE_REPLACE_CACHE.get(func.getClass());
        return (SerializedLambda) writeReplace.invoke(func);
    }

}

