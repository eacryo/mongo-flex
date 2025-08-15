package com.github.eacryo.mongoflex.util;

import com.github.eacryo.mongoflex.annotation.CollectionId;
import com.github.eacryo.mongoflex.config.SFunction;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class ReflectUtil {
    /**
     * 获取对象的所有字段（包括继承的字段）
     *
     * @param clazz 目标类
     * @return 字段列表
     */
    public static List<Field> getAllFieldsIncludingInherited(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();

        // 遍历整个继承层次
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            //这样就不会有重复添加的问题
            ReflectionUtils.doWithLocalFields(currentClass, fields::add);
            currentClass = currentClass.getSuperclass();
        }

        return fields;
    }

    public static Field getIdField(Class<?> clazz) {
        return getIdField(getAllFieldsIncludingInherited(clazz));
    }

    public static Field getIdField(List<Field> fields) {
        for (Field field : fields) {
            if (field.isAnnotationPresent(CollectionId.class)) {
                return field;
            }
        }
        throw new IllegalArgumentException("未找到id对应字段");
    }

    // 获取方法引用对应的字段名
    public static <T, R> String getFieldName(SFunction<T, R> func) {
        try {
            Method method = func.getClass().getDeclaredMethod("writeReplace");
            method.setAccessible(true);
            SerializedLambda lambda = (SerializedLambda) method.invoke(func);
            String methodName = lambda.getImplMethodName();

            // 解析getter方法名
            String fieldName;
            if (methodName.startsWith("get")) {
                fieldName = StringUtils.uncapitalize(methodName.substring(3));
            } else if (methodName.startsWith("is")) {
                fieldName = StringUtils.uncapitalize(methodName.substring(2));
            } else {
                fieldName = methodName;
            }
            
            // 特殊处理：id字段映射到MongoDB的_id字段
            if ("id".equals(fieldName)) {
                return "_id";
            }
            
            return fieldName;
        } catch (Exception e) {
            throw new RuntimeException("解析字段名失败: " + e.getMessage(), e);
        }
    }

}
