package com.github.eacryo.mongoflex.util;


import com.github.eacryo.mongoflex.annotation.CollectionName;
import com.github.eacryo.mongoflex.constant.MongoFlexConstant;
import org.slf4j.MDC;

import java.util.Objects;

/**
 * Collection name utility — resolves @CollectionName annotation and handles
 * multi-tenant collection prefixing (same DB, different collections per tenant).
 * <p>
 * 集合名工具类——解析 @CollectionName 注解，处理多租户集合前缀
 * （同一数据库、不同集合的多租户实现）。
 */
public class CollectionNameUtil {

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

    /**
     * Resolve the actual collection name with tenant prefix from MDC context /
     * 根据 MDC 上下文中的租户前缀解析实际的集合名
     * <p>
     * Tenant ID format: {@code <namespace>_<tenantName>}, e.g. {@code "myapp_tenant001"}.
     * Splits on the first {@code "_"} and uses the suffix as the collection prefix.
     * When no tenant is set in MDC (non-multi-tenant mode), returns the original
     * collection name unchanged.
     * <p>
     * 租户 ID 格式：{@code <命名空间>_<租户名>}，如 {@code "myapp_tenant001"}。
     * 按第一个 {@code "_"} 分割，后缀作为集合前缀。MDC 中无租户时（非多租户模式）返回原始集合名。
     *
     * @param collectionName base collection name / 基础集合名
     * @return tenant-prefixed collection name, or original if no tenant context / 带租户前缀的集合名，无租户上下文时返回原始值
     */
    public String select(String collectionName) {
        String tenantId = MDC.get(MongoFlexConstant.TENANT);
        if (tenantId == null || tenantId.isEmpty()) {
            return collectionName;
        }
        String[] tenantIdParts = tenantId.split("_");
        if (tenantIdParts.length > 1) {
            return tenantIdParts[1] + "_" + collectionName;
        }
        return collectionName;
    }

    /**
     * Resolve the actual collection name with explicit tenant ID /
     * 根据显式传入的租户 ID 解析实际的集合名
     * <p>
     * Same prefix logic as {@link #select(String)} but accepts tenantId as a parameter
     * instead of reading from MDC. When tenantId is null or empty, returns the original
     * collection name unchanged.
     * <p>
     * 前缀逻辑与 {@link #select(String)} 相同，但租户 ID 通过参数显式传入。
     * 当 tenantId 为 null 或空时返回原始集合名。
     *
     * @param tenantId       tenant identifier, format {@code <namespace>_<tenantName>} /
     *                       租户标识，格式为 {@code <命名空间>_<租户名>}
     * @param collectionName base collection name / 基础集合名
     * @return tenant-prefixed collection name, or original if tenantId is null/empty /
     *         带租户前缀的集合名，tenantId 为空时返回原始值
     */
    public String select(String tenantId, String collectionName) {
        if (tenantId == null || tenantId.isEmpty()) {
            return collectionName;
        }
        String[] tenantIdParts = tenantId.split("_");
        if (tenantIdParts.length > 1) {
            return tenantIdParts[1] + "_" + collectionName;
        }
        return collectionName;
    }


}
