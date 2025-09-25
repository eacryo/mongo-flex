package com.github.eacryo.mongoflex.v2;

import com.github.eacryo.mongoflex.annotation.CollectionField;
import com.github.eacryo.mongoflex.annotation.CollectionId;
import org.bson.Document;
import java.lang.reflect.Field;

public class SimpleMongoConverter {

    /**
     * 将 MongoDB Document 转换为指定的 Java 对象。
     *
     * @param document 要转换的 Document 对象
     * @param targetClass 目标 Java 类的 Class 对象
     * @param <T> 目标 Java 类的泛型
     * @return 转换后的 Java 对象实例
     */
    public <T> T convert(Document document, Class<T> targetClass) {
        if (document == null || targetClass == null) {
            return null;
        }
        if (targetClass.equals(Object.class)) {
            return (T) document;
        }

        try {
            // 1. 创建目标类的实例
            T instance = targetClass.getDeclaredConstructor().newInstance();

            // 2. 遍历目标类的所有字段
            for (Field field : targetClass.getDeclaredFields()) {
                // 设置字段为可访问，即使它是私有的
                field.setAccessible(true);

                // 3. 确定MongoDB文档中的键名
                String mongoKey = field.getName();

                // 检查是否有@Field注解
                if (field.isAnnotationPresent(CollectionField.class)) {
                    mongoKey = field.getAnnotation(CollectionField.class).value();
                }
                // 检查是否有@Id注解，_id是MongoDB的特殊键
                else if (field.isAnnotationPresent(CollectionId.class)) {
                    mongoKey = "_id";
                }

                // 4. 从Document中获取对应的值
                Object value = document.get(mongoKey);

                // 如果值不为空，则设置到Java对象的字段上
                if (value != null) {
                    // 这里可以添加更复杂的类型转换逻辑，例如：
                    // value = convertValueToFieldType(value, field.getType());
                    field.set(instance, value);
                }
            }

            return instance;

        } catch (Exception e) {
            // 在实际项目中，需要更详细地处理各种异常
            System.err.println("转换失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
