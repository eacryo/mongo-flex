package com.github.eacryo.mongoflex.v2;

import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.github.eacryo.mongoflex.annotation.CollectionField;

public class FieldMappingIntrospector extends JacksonAnnotationIntrospector {

    // 覆盖这个方法，用于在反序列化时（从 JSON 读取）获取字段名
    @Override
    public PropertyName findNameForDeserialization(Annotated a) {
        // 查找字段上是否有 @CollectionField 注解
        CollectionField ann = a.getAnnotation(CollectionField.class);
        if (ann != null) {
            // 如果找到，使用注解的值作为 JSON 字段名
            return PropertyName.construct(ann.value());
        }
        // 否则，使用默认的 Jackson 逻辑
        return super.findNameForDeserialization(a);
    }

    // 覆盖这个方法，用于在序列化时（写入 JSON）获取字段名
    @Override
    public PropertyName findNameForSerialization(Annotated a) {
        CollectionField ann = a.getAnnotation(CollectionField.class);
        if (ann != null) {
             // 如果找到，使用注解的值作为 JSON 字段名
            return PropertyName.construct(ann.value());
        }
        // 否则，使用默认的 Jackson 逻辑
        return super.findNameForSerialization(a);
    }
}
