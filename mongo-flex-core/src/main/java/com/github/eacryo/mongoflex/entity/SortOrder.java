package com.github.eacryo.mongoflex.entity;

import com.github.eacryo.mongoflex.util.ReflectUtil;
import com.github.eacryo.mongoflex.util.SFunction;
import lombok.Data;

/**
 * Sort order entry: field name + sort direction / 排序条目：字段名 + 排序方向
 * <p>
 * Supports two construction paths:
 * <ul>
 *   <li><b>String-based</b> — {@code new SortOrder("address", true)} for frontend-passed field names,
 *       resolved against the repository's entity class</li>
 *   <li><b>Lambda-based</b> — {@code new SortOrder(Character::getAddress, true)} for type-safe references,
 *       correctly resolves {@code @CollectionField} mappings from the field's declaring class</li>
 * </ul>
 * Follows MyBatis-Plus {@code OrderItem} design pattern.
 * <p>
 * 支持两种构造方式：
 * <ul>
 *   <li><b>字符串方式</b> — {@code new SortOrder("address", true)} 用于前端传递的字段名</li>
 *   <li><b>方法引用方式</b> — {@code new SortOrder(Character::getAddress, true)} 类型安全，
 *       可正确解析字段声明类上的 {@code @CollectionField} 映射</li>
 * </ul>
 * 遵循 MyBatis-Plus {@code OrderItem} 设计模式。
 *
 * @param <T> entity type / 实体类型
 */
@Data
public class SortOrder<T> {
    /** MongoDB field name or Java field name (fallback) / MongoDB 字段名或 Java 字段名（回退） */
    private String field;
    /** true = ascending, false = descending / true=升序, false=降序 */
    private boolean ascending = true;
    /** Java field name extracted from lambda, for proper @CollectionField resolution / 从 lambda 提取的 Java 字段名 */
    private String javaFieldName;
    /** Declaring class extracted from lambda, for proper @CollectionField resolution / 从 lambda 提取的声明类 */
    private Class<?> implClass;

    public SortOrder() {
    }

    /**
     * String-based sort order / 基于字符串字段名的排序
     *
     * @param field     MongoDB field name or Java field name / MongoDB 字段名或 Java 字段名
     * @param ascending true=ascending, false=descending / true=升序, false=降序
     */
    public SortOrder(String field, boolean ascending) {
        this.field = field;
        this.ascending = ascending;
    }

    /**
     * Lambda-based sort order with type-safe field reference / 基于方法引用的类型安全排序
     * <p>
     * Extracts the Java field name and declaring class from the lambda at construction time
     * so that {@code @CollectionField} mappings can be correctly resolved during rendering.
     *
     * @param field     method reference, e.g. {@code Character::getAddress} / 方法引用，如 {@code Character::getAddress}
     * @param ascending true=ascending, false=descending / true=升序, false=降序
     */
    public SortOrder(SFunction<T, ?> field, boolean ascending) {
        this.javaFieldName = ReflectUtil.getFieldNameFromLambda(field);
        this.implClass = ReflectUtil.getImplClassFromLambda(field);
        this.field = this.javaFieldName; // fallback: use Java field name when mapping info unavailable
        this.ascending = ascending;
    }
}
