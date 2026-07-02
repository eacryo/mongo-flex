package com.github.eacryo.mongoflex.convertor;


import com.github.eacryo.mongoflex.annotation.CollectionField;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MongoMappingConverter: 基于反射的 POJO <-> BSON Document 转换器。
 * 实现了嵌套对象处理、Date <-> ISODate 转换以及 id <-> _id 映射。
 */
@Component
public class MongoMappingConvertor {

    private static final String MONGO_ID_FIELD = "_id";
    private static final String JAVA_ID_FIELD = "id";

    private static final ConcurrentHashMap<Class<?>,ClassFieldMetaData> META_DATA_CACHE = new ConcurrentHashMap<>();

    private ClassFieldMetaData getMetaData(Class<?> clazz){
        return META_DATA_CACHE.computeIfAbsent(clazz,ClassFieldMetaData::new);
    }


    /**
     * 将Java字段名解析为MongoDB中对应的字段名
     * 例如将id -> _id, 或者使用 @CollectionField中注解指定的名称
     * @return
     */
    public String resolveMongoFieldName(Class<?> clazz, String javaFieldName){
        for(FieldMapping mapping : getMetaData(clazz).getFieldMappingList()){
            if (mapping.getField().getName().equals(javaFieldName)){
                return mapping.getMongoFieldName();
            }
        }
        return javaFieldName; // 如果没有找到对应的映射，返回原始字段名
    }

    // --- 写入 (POJO -> BSON Document) ---

    /**
     * 将 Java POJO 对象转换为 MongoDB Document。
     * @param entity 要转换的 Java 对象。
     * @return 转换后的 Document。
     */
    public <T> Document write(T entity) {
        if (entity == null) {
            return null;
        }
        // 使用 Document 构造函数，利用其对 java.util.Date 的原生支持
        return new Document(convertToMap(entity));
    }

    // 递归将 POJO 转换为 Map (Document)
    private Map<String, Object> convertToMap(Object entity) {
        if (entity == null) {
            return null;
        }

        Class<?> clazz = entity.getClass();

        // 如果是系统类型（如 Date, String, Number），直接返回，不进行反射遍历
        if (isPrimitiveOrSystemType(clazz)) {
            // 注意：这里是一个类型转换警告，但在逻辑上是安全的，因为调用方会确保类型匹配。
            return (Map<String, Object>) entity;
        }

        Map<String, Object> map = new HashMap<>();

        for (FieldMapping mapping : getMetaData(clazz).getFieldMappingList()) {
            try{
                Object value = mapping.field.get(entity);
                if (value == null){
                    continue;
                }
                String fieldName = mapping.mongoFieldName;
                Object convertedValue = processFieldValue(value);

                map.put(fieldName,convertedValue);
            } catch (IllegalAccessException e){
                throw new RuntimeException("Error accessing field: " + mapping.field.getName() + " in class: " + clazz.getName(), e);
            }
        }
        return map;
    }

    // 递归处理字段值，将嵌套 POJO 转换为 Map，List 转换为 List<Map>
    private Object processFieldValue(Object value) {
        if (value == null) {
            return null;
        }

        Class<?> valueClass = value.getClass();

        // 1. BSON 兼容类型：直接返回 (Date, ObjectId, Number, String, Boolean)
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
                .map(this::processFieldValue) // 递归处理列表中的每个元素
                .collect(Collectors.toList());
        }

        // 3. 嵌套 POJO：递归调用 convertToMap
        if (!isPrimitiveOrSystemType(valueClass)) {
            return convertToMap(value);
        }

        return value;
    }

    // --- 读取 (BSON Document -> POJO) ---

    /**
     * 将 MongoDB Document 转换为指定的 Java POJO 对象。
     * @param doc 待转换的 Document。
     * @param targetClass 目标 POJO 类的 Class 对象。
     * @return 转换后的 POJO 实例。
     */
    public <T> T read(Document doc, Class<T> targetClass) {
        if (doc == null) {
            return null;
        }

        try {
            // 实例化目标 POJO (要求有无参构造函数)
            T entity = targetClass.getDeclaredConstructor().newInstance();

            for (FieldMapping mapping : getMetaData(targetClass).getFieldMappingList()) {
                String mongoFieldName = mapping.getMongoFieldName();
                String docKey = mongoFieldName;
                if (MONGO_ID_FIELD.equals(mongoFieldName) && doc.containsKey(MONGO_ID_FIELD)) {
                    docKey = MONGO_ID_FIELD;
                } else if (!doc.containsKey(mongoFieldName)){
                    continue;
                }
                Object bsonValue = doc.get(docKey);
                if (bsonValue!=null){
                    Object javaValue = convertBsonValueToJavaType(bsonValue,mapping.getFieldType(),mapping.getGenericType());
                    mapping.field.set(entity, javaValue);
                }
            }
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Error reading Document to POJO: " + targetClass.getName(), e);
        }
    }

    // 递归将 BSON 值转换为目标 Java 类型
    private Object convertBsonValueToJavaType(Object bsonValue, Class<?> targetClass, Type genericType) {
        if (bsonValue == null) {
            return null;
        }

        // 1. 基本兼容类型：直接返回（例如 ISODate -> java.util.Date，ObjectId -> ObjectId）
        if (targetClass.isInstance(bsonValue) || targetClass.isPrimitive()) {
            return bsonValue;
        }

        // 2. 当Java类型为String类型，而Bson类型为ObjectId时，转换为十六进制字符串
        if (targetClass == String.class && bsonValue instanceof ObjectId) {
            return ((ObjectId) bsonValue).toHexString();
        }

        // 3. 嵌套 Document：递归调用 read
        if (bsonValue instanceof Document && !isPrimitiveOrSystemType(targetClass)) {
            // TargetClass 必须有无参构造函数
            return read((Document) bsonValue, targetClass);
        }

        // 4. List/Collection：处理嵌套列表元素
        if (bsonValue instanceof List && targetClass.isAssignableFrom(List.class)) {
            List<Object> bsonList = (List<Object>) bsonValue;

            // 尝试获取列表元素的泛型类型
            Class<?> elementType = getListElementType(genericType);

            return bsonList.stream()
                .map(item -> convertBsonValueToJavaType(item, elementType, elementType))
                .collect(Collectors.toList());
        }

        // 5. 枚举：从字符串转换
        if (targetClass.isEnum() && bsonValue instanceof String) {
            return Enum.valueOf((Class<Enum>) targetClass, (String) bsonValue);
        }

        // 默认返回 BSON 值
        return bsonValue;
    }

    // --- 辅助方法 ---

    /**
     * 获取类及其所有父类的所有字段。
     */
    private List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            fields.addAll(Arrays.asList(c.getDeclaredFields()));
        }
        return fields;
    }

    /**
     * 判断是否是基础的或系统类型，用于决定是否需要递归处理（避免对 JDK 类进行反射）。
     */
    private boolean isPrimitiveOrSystemType(Class<?> clazz) {
        return clazz.isPrimitive() ||
               clazz.isEnum() ||
               clazz.getName().startsWith("java.lang.") ||
               clazz.getName().startsWith("java.util.") ||
               clazz.getName().startsWith("java.time.") ||
               clazz.getName().startsWith("org.bson.");
    }

    /**
     * 简单的泛型类型获取（针对 List<T> 场景）。
     */
    private Class<?> getListElementType(Type genericType) {
        if (genericType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) genericType;
            Type[] args = pt.getActualTypeArguments();
            if (args.length > 0 && args[0] instanceof Class) {
                return (Class<?>) args[0];
            }
        }
        return Object.class;
    }
}
