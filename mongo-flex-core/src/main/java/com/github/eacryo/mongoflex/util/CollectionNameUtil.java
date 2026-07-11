package com.github.eacryo.mongoflex.util;


import com.github.eacryo.mongoflex.annotation.CollectionName;
import com.github.eacryo.mongoflex.constant.MongoFlexConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Objects;

//如果是同一个数据库但不同表的多租户实现,则需要根据tablePrefix和collectionName拼接出实际的collectionName
public class CollectionNameUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectionNameUtil.class);

    public <T> String getByObj(T obj) {
        // 获取注解实例
        CollectionName annotation = obj.getClass().getAnnotation(CollectionName.class);
        if (Objects.isNull(annotation)) {
            String errMsg = "在{"+ obj.getClass().getName() +"}上获取注解失败,请检查实体类是否添加了@CollectionName注解";
            throw new RuntimeException(errMsg);
        }
        String value = annotation.value();
        return value;
    }


    public <T> String getByClass(Class<T> clazz) {
        // 获取注解实例
        CollectionName annotation = clazz.getAnnotation(CollectionName.class);
        if (Objects.isNull(annotation)) {
            String errMsg = "在{" + clazz.getName() + "}上获取注解失败,请检查实体类是否添加了@CollectionName注解";
            throw new RuntimeException(errMsg);
        }
        String value = annotation.value();
        return value;
    }

    public String select(String collectionName) {
        String tenantId = MDC.get(MongoFlexConstant.TENANT);
        String[] tenantIdParts = tenantId.split("_");
        if (tenantIdParts.length > 1) {
            return tenantIdParts[1] + "_" + collectionName;
        }
        return tenantId;
    }

    public String select(String tenantId, String collectionName) {
        String[] tenantIdParts = tenantId.split("_");
        if (tenantIdParts.length > 1) {
            return tenantIdParts[1] + "_" + collectionName;
        }
        return collectionName;
    }


}
