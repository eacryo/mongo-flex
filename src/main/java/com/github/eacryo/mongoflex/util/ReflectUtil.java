package com.github.eacryo.mongoflex.util;

import com.github.eacryo.mongoflex.annotation.CollectionId;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

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
        //TODO:优化报错信息；
        //这里应该用RuntimeException吗？
        throw new RuntimeException("未找到id对应字段");
    }
}
