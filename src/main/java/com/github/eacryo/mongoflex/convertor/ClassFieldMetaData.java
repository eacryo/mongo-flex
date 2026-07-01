package com.github.eacryo.mongoflex.convertor;

import com.github.eacryo.mongoflex.annotation.CollectionField;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 实体类的反射元数据缓存
 * 首次访问某个实体类的时候，通过反射收集字段信息并缓存
 */
public class ClassFieldMetaData {
    private static final String MONGO_ID_FIELD = "_id";
    private static final String JAVA_ID_FIELD = "id";

    private final List<FieldMapping> fieldMappingList;


    //从当前类开始向上遍历，直到Object
    public ClassFieldMetaData(Class<?> clazz) {
        List<FieldMapping> mappingList = new ArrayList<>();
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
                mappingList.add(new FieldMapping(field, mongoFieldName , field.getType(),field.getGenericType()));
            }
        }
        //不可变列表保证线程安全
        this.fieldMappingList = List.copyOf(mappingList);
    }



    public List<FieldMapping> getFieldMappingList() {
        return fieldMappingList;
    }
}
