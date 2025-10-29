package com.github.eacryo.mongoflex.convertor;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import org.bson.Document;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import org.bson.Document;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class MongoMappingConvertor {

    private static final String MONGO_ID_FIELD = "_id";
    private static final String JAVA_ID_FIELD = "id";

    // --- 写入 (POJO -> BSON Document) ---

    public <T> Document write(T entity) {
        if (entity == null) {
            return null;
        }
        return new Document(convertToMap(entity));
    }

    private Map<String, Object> convertToMap(Object entity) {
        if (entity == null) {
            return null;
        }

        Class<?> clazz = entity.getClass();
        // 确保不是系统类型，否则返回原始对象
        if (isPrimitiveOrSystemType(clazz)) {
            return (Map<String, Object>) entity;
        }

        Map<String, Object> map = new HashMap<>();

        for (Field field : getAllFields(clazz)) {
            field.setAccessible(true);
            try {
                Object value = field.get(entity);

                if (value == null) {
                    continue;
                }

                String fieldName = field.getName();

                // 1. 映射处理：Java id -> MongoDB _id
                if (JAVA_ID_FIELD.equals(fieldName)) {
                    fieldName = MONGO_ID_FIELD;
                }

                // 2. 递归处理值（Date, 嵌套对象等）
                Object convertedValue = processFieldValue(value);

                map.put(fieldName, convertedValue);

            } catch (IllegalAccessException e) {
                throw new RuntimeException("Error accessing field '" + field.getName() + "' via reflection.", e);
            }
        }
        return map;
    }

    private Object processFieldValue(Object value) {
        if (value == null) {
            return null;
        }

        Class<?> valueClass = value.getClass();

        // 1. 基本类型和 BSON 兼容类型（包括 Date, ObjectId）
        if (value instanceof Date ||
                value instanceof Number ||
                value instanceof String ||
                value instanceof Boolean ||
                value instanceof ObjectId ||
                valueClass.isEnum()) {
            return value;
        }

        // 2. 集合/列表：递归处理内部元素
        if (value instanceof List) {
            List<?> originalList = (List<?>) value;
            return originalList.stream()
                    .map(this::processFieldValue)
                    .collect(Collectors.toList());
        }

        // 3. 嵌套 POJO：递归调用 convertToMap
        if (!isPrimitiveOrSystemType(valueClass)) {
            return convertToMap(value);
        }

        return value;
    }

    // --- 读取 (BSON Document -> POJO) ---

    public <T> T read(Document doc, Class<T> targetClass) {
        if (doc == null) {
            return null;
        }

        try {
            T entity = targetClass.getDeclaredConstructor().newInstance();

            for (Field field : getAllFields(targetClass)) {
                field.setAccessible(true);
                String fieldName = field.getName();
                String docKey = fieldName;

                // 1. 映射处理：MongoDB _id -> Java id
                if (JAVA_ID_FIELD.equals(fieldName) && doc.containsKey(MONGO_ID_FIELD)) {
                    docKey = MONGO_ID_FIELD;
                } else if (!doc.containsKey(fieldName)) {
                    continue; // 文档中没有这个键，跳过
                }

                Object bsonValue = doc.get(docKey);

                // 2. 递归将 BSON 值转换为目标 Java 类型
                Object javaValue = convertBsonValueToJavaType(bsonValue, field.getType(), field.getGenericType());

                field.set(entity, javaValue);

            }
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Error reading Document to POJO: " + targetClass.getName(), e);
        }
    }

    private Object convertBsonValueToJavaType(Object bsonValue, Class<?> targetClass, java.lang.reflect.Type genericType) {
        if (bsonValue == null) {
            return null;
        }

        // 1. 基本兼容类型：直接返回（包括 ISODate -> java.util.Date）
        if (targetClass.isInstance(bsonValue) || targetClass.isPrimitive()) {
            return bsonValue;
        }

        // 2. 嵌套 Document：递归调用 read
        if (bsonValue instanceof Document && !isPrimitiveOrSystemType(targetClass)) {
            return read((Document) bsonValue, targetClass);
        }

        // 3. List/Collection：处理嵌套列表元素
        if (bsonValue instanceof List && targetClass.isAssignableFrom(List.class)) {
            List<Object> bsonList = (List<Object>) bsonValue;

            // 尝试获取列表元素的泛型类型
            Class<?> elementType = getListElementType(genericType);

            return bsonList.stream()
                    .map(item -> convertBsonValueToJavaType(item, elementType, elementType))
                    .collect(Collectors.toList());
        }

        // 4. 枚举
        if (targetClass.isEnum() && bsonValue instanceof String) {
            return Enum.valueOf((Class<Enum>) targetClass, (String) bsonValue);
        }

        // 默认返回 BSON 值
        return bsonValue;
    }

    // --- 辅助方法 ---

    // 获取类及其所有父类的字段
    private List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            fields.addAll(Arrays.asList(c.getDeclaredFields()));
        }
        return fields;
    }

    // 判断是否是基础的或系统类型，避免对系统类进行递归处理
    private boolean isPrimitiveOrSystemType(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz.isEnum() ||
                clazz.getName().startsWith("java.lang.") ||
                clazz.getName().startsWith("java.util.") ||
                clazz.getName().startsWith("java.time.") ||
                clazz.getName().startsWith("org.bson.");
    }

    // 简单的泛型类型获取
    private Class<?> getListElementType(java.lang.reflect.Type genericType) {
        if (genericType instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.ParameterizedType pt = (java.lang.reflect.ParameterizedType) genericType;
            java.lang.reflect.Type[] args = pt.getActualTypeArguments();
            if (args.length > 0 && args[0] instanceof Class) {
                return (Class<?>) args[0];
            }
        }
        return Object.class;
    }
}