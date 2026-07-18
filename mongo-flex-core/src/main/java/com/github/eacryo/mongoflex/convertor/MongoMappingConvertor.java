package com.github.eacryo.mongoflex.convertor;


import org.bson.Document;
import org.bson.types.ObjectId;

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
public class MongoMappingConvertor {

    private static final String MONGO_ID_FIELD = "_id";
    private static final String JAVA_ID_FIELD = "id";

    private static final ConcurrentHashMap<Class<?>,ClassFieldMetaData> META_DATA_CACHE = new ConcurrentHashMap<>();

    private ClassFieldMetaData getMetaData(Class<?> clazz){
        return META_DATA_CACHE.computeIfAbsent(clazz,ClassFieldMetaData::new);
    }


    /**
     * Resolve a Java field path (single field name or dot-separated nested path) to the
     * corresponding MongoDB field path. / 将 Java 字段路径（单段字段名或点号分隔的嵌套路径）解析为 MongoDB 中对应的字段路径。
     * <p>
     * Each segment is mapped independently against the metadata of its declaring class
     * ({@code @CollectionId} / {@code @CollectionField} / implicit {@code id -> _id}),
     * then the segments are joined back with dots. / 每一段都基于其声明类的元数据独立映射
     * （@CollectionId / @CollectionField / 隐式 id→_id），再用点号重新拼接。
     * <p>
     * Examples / 示例：
     * <ul>
     *   <li>{@code resolveMongoFieldPath(User.class, "id")} → {@code "_id"}</li>
     *   <li>{@code resolveMongoFieldPath(User.class, "address.city")} → {@code "addr.city"}
     *       (when the address field is annotated with {@code @CollectionField("addr")}
     *       / 当 address 字段标注了 {@code @CollectionField("addr")} 时)</li>
     * </ul>
     * Collection segments are traversed transparently into their generic element type,
     * matching MongoDB's dot-notation semantics for arrays. / 集合段会透明穿透到其泛型元素类型，
     * 与 MongoDB 数组的点号语义一致。
     * <p>
     * If a segment has no mapping in the metadata, that segment and all following
     * segments are kept as-is. / 若某段在元数据中找不到映射，该段及其后所有段原样保留。
     *
     * @param clazz         root entity class the path starts from / 路径起始的根实体类
     * @param javaFieldPath Java field name or dot-separated field path / Java 字段名或点号分隔的字段路径
     * @return the corresponding MongoDB field path / MongoDB 中对应的字段路径
     */
    public String resolveMongoFieldPath(Class<?> clazz, String javaFieldPath){
        if (javaFieldPath == null || javaFieldPath.indexOf('.') < 0) {
            // Single segment — fast path, identical to the legacy single-name behavior
            // 单段——快速路径，与旧的单字段名行为完全一致
            FieldMapping mapping = getMetaData(clazz).getFieldMappingByJavaName().get(javaFieldPath);
            if (mapping != null) {
                return mapping.getMongoFieldName();
            }
            return javaFieldPath;
        }

        String[] segments = javaFieldPath.split("\\.");
        StringBuilder result = new StringBuilder();
        Class<?> currentClass = clazz;
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                result.append('.');
            }
            String segment = segments[i];
            FieldMapping mapping = currentClass != null
                    ? getMetaData(currentClass).getFieldMappingByJavaName().get(segment)
                    : null;
            if (mapping == null) {
                // Unknown segment — keep this and all remaining segments as-is
                // 未知段——该段及其后所有段原样保留
                result.append(segment);
                currentClass = null;
                continue;
            }
            result.append(mapping.getMongoFieldName());
            currentClass = nextSegmentClass(mapping);
        }
        return result.toString();
    }

    /**
     * Determine the class used to resolve the next path segment: collection fields are
     * traversed into their generic element type, plain fields use their declared type.
     * Primitive/system types are not traversable and yield null. / 确定用于解析下一段路径的类：
     * 集合字段穿透到其泛型元素类型，普通字段使用其声明类型。基础/系统类型不可继续导航，返回 null。
     */
    private Class<?> nextSegmentClass(FieldMapping mapping) {
        Class<?> fieldType = mapping.getFieldType();
        if (fieldType == null) {
            return null;
        }
        if (Collection.class.isAssignableFrom(fieldType)) {
            Type genericType = mapping.getGenericType();
            if (genericType instanceof ParameterizedType) {
                Type[] args = ((ParameterizedType) genericType).getActualTypeArguments();
                if (args.length > 0 && args[0] instanceof Class) {
                    Class<?> elementClass = (Class<?>) args[0];
                    // Element type may itself be a system type (e.g. List<String>) / 元素类型本身也可能是系统类型（如 List<String>）
                    return isPrimitiveOrSystemType(elementClass) ? null : elementClass;
                }
            }
            return null;
        }
        return isPrimitiveOrSystemType(fieldType) ? null : fieldType;
    }

    /**
     * 获取指定Java字段名对应的字段元数据（含 Field、字段类型、泛型类型等）
     */
    public FieldMapping getFieldMapping(Class<?> clazz, String javaFieldName) {
        return getMetaData(clazz).getFieldMappingByJavaName().get(javaFieldName);
    }

    /**
     * Get the field metadata of the last segment of a Java field path, walking nested
     * types segment by segment. / 获取 Java 字段路径末段的字段元数据，逐段穿透嵌套类型。
     * Returns null if any segment cannot be resolved. / 任一段无法解析时返回 null。
     */
    private FieldMapping getFieldMappingByPath(Class<?> clazz, String javaFieldPath) {
        if (javaFieldPath == null || javaFieldPath.indexOf('.') < 0) {
            return getFieldMapping(clazz, javaFieldPath);
        }
        String[] segments = javaFieldPath.split("\\.");
        Class<?> currentClass = clazz;
        FieldMapping mapping = null;
        for (String segment : segments) {
            if (currentClass == null) {
                return null;
            }
            mapping = getFieldMapping(currentClass, segment);
            if (mapping == null) {
                return null;
            }
            currentClass = nextSegmentClass(mapping);
        }
        return mapping;
    }

    /**
     * 获取字段的泛型元素类型（如 List&lt;Item&gt; → Item.class）。
     * Supports dot-separated nested paths, e.g. {@code "team.members"}. / 支持点号分隔的嵌套路径，如 {@code "team.members"}。
     */
    public Class<?> getFieldGenericElementType(Class<?> clazz, String javaFieldPath) {
        FieldMapping mapping = getFieldMappingByPath(clazz, javaFieldPath);
        if (mapping == null) return null;
        Type genericType = mapping.getGenericType();
        if (genericType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) genericType;
            Type[] args = pt.getActualTypeArguments();
            if (args.length > 0 && args[0] instanceof Class) {
                return (Class<?>) args[0];
            }
        }
        return null;
    }

    public Field getCollectionIdField(Class<?> clazz) {
        return getMetaData(clazz).getCollectionIdField();
    }

    public Field getCreateDateField(Class<?> clazz) {
        return getMetaData(clazz).getCreateDateField();
    }

    public Field getUpdateDateField(Class<?> clazz) {
        return getMetaData(clazz).getUpdateDateField();
    }

    // --- 写入 (POJO -> BSON Document) ---

    /**
     * 将 Java POJO 对象转换为 MongoDB Document。
     * @param entity 要转换的 Java 对象。
     * @return 转换后的 Document。
     */
    public <T> Document write(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity must not be null");
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

        for (FieldMapping mapping : getMetaData(clazz).getFieldMappingByJavaName().values()) {
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

            for (FieldMapping mapping : getMetaData(targetClass).getFieldMappingByJavaName().values()) {
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

    // --- Document → Map 转换（用于 Object.class 等无实体映射场景） ---

    /**
     * 将 BSON Document 递归转换为普通的 {@link Map}&lt;String, Object&gt;，
     * 嵌套的 Document 和 List 中的 Document 也会被递归转换。
     * 当 {@code @Mql} 方法返回 {@code List<Object>} 或 {@code Object} 时使用，
     * 用户可自行对 Map 做序列化/反序列化处理。
     */
    public Map<String, Object> documentToMap(Document doc) {
        if (doc == null) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : doc.entrySet()) {
            result.put(entry.getKey(), convertDocumentValue(entry.getValue()));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object convertDocumentValue(Object value) {
        if (value instanceof Document) {
            return documentToMap((Document) value);
        } else if (value instanceof List) {
            List<Object> list = new ArrayList<>();
            for (Object item : (List<?>) value) {
                list.add(convertDocumentValue(item));
            }
            return list;
        }
        return value;
    }

    // --- 辅助方法 ---

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
