package com.github.eacryo.mongoflex.convertor;

import com.github.eacryo.mongoflex.annotation.CollectionField;
import com.github.eacryo.mongoflex.annotation.CollectionId;
import com.github.eacryo.mongoflex.annotation.CreateDate;
import com.github.eacryo.mongoflex.annotation.UpdateDate;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 实体类的反射元数据缓存
 * 首次访问某个实体类的时候，通过反射收集字段信息并缓存
 */
public class ClassFieldMetaData {
    private static final String MONGO_ID_FIELD = "_id";
    private static final String JAVA_ID_FIELD = "id";

    private final Map<String, FieldMapping> fieldMappingByJavaName;
    private final Field collectionIdField;
    private final Field createDateField;
    private final Field updateDateField;


    //从当前类开始向上遍历，直到Object
    //遍历顺序：子类 → 父类。使用 putIfAbsent 确保子类字段优先级高于父类同名字段
    public ClassFieldMetaData(Class<?> clazz) {
        Map<String, FieldMapping> mappingByJavaName = new HashMap<>();
        Field foundCollectionIdField = null;
        Field foundCreateDateField = null;
        Field foundUpdateDateField = null;
        for(Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                field.setAccessible(true);
                //确定该字段在MongoDB当中对应的字段名
                //优先使用@CollectionField注解作为指定的名称，否则使用Java字段名
                String mongoFieldName;
                if (field.isAnnotationPresent(CollectionField.class)){
                    mongoFieldName = field.getAnnotation(CollectionField.class).value();
                } else {
                    mongoFieldName = field.getName();
                }

                //Java中的id字段自动映射为MongoDB中的 _id
                if (JAVA_ID_FIELD.equals(mongoFieldName)) {
                    mongoFieldName = MONGO_ID_FIELD;
                }
                FieldMapping mapping = new FieldMapping(field, mongoFieldName, field.getType(), field.getGenericType());
                mappingByJavaName.putIfAbsent(field.getName(), mapping);

                if (foundCollectionIdField == null && field.isAnnotationPresent(CollectionId.class)) {
                    foundCollectionIdField = field;
                }
                if (foundCreateDateField == null && field.isAnnotationPresent(CreateDate.class)) {
                    foundCreateDateField = field;
                }
                if (foundUpdateDateField == null && field.isAnnotationPresent(UpdateDate.class)) {
                    foundUpdateDateField = field;
                }
            }
        }
        this.fieldMappingByJavaName = Collections.unmodifiableMap(new HashMap<>(mappingByJavaName));
        this.collectionIdField = foundCollectionIdField;
        this.createDateField = foundCreateDateField;
        this.updateDateField = foundUpdateDateField;
    }



    public Map<String, FieldMapping> getFieldMappingByJavaName() {
        return fieldMappingByJavaName;
    }

    public Field getCollectionIdField() {
        return collectionIdField;
    }

    public Field getCreateDateField() {
        return createDateField;
    }

    public Field getUpdateDateField() {
        return updateDateField;
    }
}
