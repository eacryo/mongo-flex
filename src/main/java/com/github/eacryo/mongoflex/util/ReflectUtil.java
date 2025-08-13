package com.github.eacryo.mongoflex.util;

import com.github.eacryo.mongoflex.annotation.CollectionId;
import com.github.eacryo.mongoflex.annotation.CollectionName;
import com.github.eacryo.mongoflex.annotation.FieldName;
import com.github.eacryo.mongoflex.annotation.ToSnakeCase;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
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
            if (field.isAnnotationPresent(CollectionId.class)){
                return field;
            }
        }
        throw new IllegalArgumentException("未找到id对应字段");
    }

    public static Map<String,String> getFieldMapping(Class<?> clazz){
        Map<String,String> mapping = new HashMap<>();
        ToSnakeCase toSnakeCase = clazz.getAnnotation(ToSnakeCase.class);
        List<Field> fields = getAllFieldsIncludingInherited(clazz);
        if (Objects.nonNull(toSnakeCase)) {
            for(Field field : fields){
                //TODO:小驼峰转下划线
                mapping.put(field.getName(), lowerCamelCaseToSnakeCase(field.getName()));
            }
            return mapping;
        }
        for (Field field : fields) {
            //TODO:
            if (field.isAnnotationPresent(ToSnakeCase.class)) {
                mapping.put(field.getName(), lowerCamelCaseToSnakeCase(field.getName()));
                continue;
            };
            String tableField = Optional.ofNullable(field.getAnnotation(FieldName.class))
                    .map(FieldName::value).orElse(null);
            if (Objects.nonNull(tableField)) {
                mapping.put(field.getName(), tableField);
            }
        }
        return mapping;
    }

    private static String lowerCamelCaseToSnakeCase(String cameCase) {
        return StringUtil.camelToUnderscore(cameCase);
    }
}
