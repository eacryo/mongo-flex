package com.github.eacryo.mongoflex.naming;

import com.github.eacryo.mongoflex.annotation.CollectionField;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.util.StringUtils;

public class CustomNamingStrategy implements FieldNamingStrategy {
    @Override
    public String getFieldName(PersistentProperty<?> property) {
        // 尝试获取我们自定义的 @CollectionField 注解
        CollectionField fieldAnnotation = property.findAnnotation(CollectionField.class);

        // 如果找到了自定义注解，并且注解的值不为空，则使用该值作为字段名
        if (fieldAnnotation != null && StringUtils.hasText(fieldAnnotation.value())) {
            return fieldAnnotation.value();
        }

        // 如果没有自定义注解，或者注解值为空，则执行默认的命名策略。
        // 在这里，我们可以将小驼峰转换为下划线。
        return property.getName();
    }

    // 辅助方法：将小驼峰转换为下划线
//    private String camelCaseToUnderscores(String name) {
//        return name.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
//    }
}
