package com.github.eacryo.mongoflex.util;


import com.github.eacryo.mongoflex.annotation.CollectionName;

import java.util.Objects;

/**
 * Collection name utility — resolves @CollectionName annotation /
 * 集合名工具类——解析 @CollectionName 注解
 */
public class CollectionNameUtil {

    public <T> String getByObj(T obj) {
        CollectionName annotation = obj.getClass().getAnnotation(CollectionName.class);
        if (Objects.isNull(annotation)) {
            String errMsg = "在{"+ obj.getClass().getName() +"}上获取注解失败,请检查实体类是否添加了@CollectionName注解";
            throw new RuntimeException(errMsg);
        }
        return annotation.value();
    }


    public <T> String getByClass(Class<T> clazz) {
        CollectionName annotation = clazz.getAnnotation(CollectionName.class);
        if (Objects.isNull(annotation)) {
            String errMsg = "在{" + clazz.getName() + "}上获取注解失败,请检查实体类是否添加了@CollectionName注解";
            throw new RuntimeException(errMsg);
        }
        return annotation.value();
    }
}
