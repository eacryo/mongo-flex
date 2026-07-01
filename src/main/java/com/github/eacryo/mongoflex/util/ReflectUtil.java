package com.github.eacryo.mongoflex.util;

import com.github.eacryo.mongoflex.annotation.CollectionId;
import com.github.eacryo.mongoflex.annotation.CreateDate;
import com.github.eacryo.mongoflex.annotation.UpdateDate;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ReflectUtil {

    private static final ConcurrentHashMap<Class<?>,Optional<Field>> ID_FIELD_CACHE = new ConcurrentHashMap<>();

    public static Field getCachedIdField(Class<?> clazz){
        return ID_FIELD_CACHE.computeIfAbsent(clazz, c -> {
            List<Field> fields = getAllFieldsIncludingInherited(c);
            Field idField = getIdFieldNotThrow(fields);
            if (idField != null){
               idField.setAccessible(true);
            }
            return Optional.ofNullable(idField);
        }).orElse(null);
    }

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

    public static Field getIdFieldNotThrow(List<Field> fields) {
        for (Field field : fields) {
            if (field.isAnnotationPresent(CollectionId.class)) {
                return field;
            }
        }
        return null;
    }

    public static Field getCreateDateField(List<Field> fields) {
        for (Field field : fields) {
            if (field.isAnnotationPresent(CreateDate.class)) {
                return field;
            }
        }
        return null;
    }

    public static Field getUpdateDateField(List<Field> fields) {
        for (Field field : fields) {
            if (field.isAnnotationPresent(UpdateDate.class)) {
                return field;
            }
        }
        return null;
    }

    public static Field getFiled(Class<?> clazz, String fieldName) {
        List<Field> fields = getAllFieldsIncludingInherited(clazz);
        return fields.stream().filter(f -> f.getName().equals(fieldName)).findFirst().orElse(null);
    }


}
