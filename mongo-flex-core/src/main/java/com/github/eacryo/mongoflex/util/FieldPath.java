package com.github.eacryo.mongoflex.util;

import java.util.Objects;

/**
 * Type-safe nested field path built from chained lambda method references, inspired by
 * Spring Data's {@code TypedPropertyPath} and Kotlin's {@code KProperty} div operator.
 * / 由链式 lambda 方法引用构建的类型安全嵌套字段路径，设计参考 Spring Data 的
 * {@code TypedPropertyPath} 与 Kotlin 的 {@code KProperty} 除号运算符。
 * <p>
 * Usage / 使用示例:
 * <pre>{@code
 * // address.city — two-level nesting / 两级嵌套
 * FieldPath.of(User::getAddress, Address::getCity)
 * // or the fluent form / 或流式写法
 * FieldPath.of(User::getAddress).then(Address::getCity)
 *
 * // arbitrary depth via then() / 通过 then() 支持任意深度
 * FieldPath.of(Order::getCustomer).then(Customer::getAddress).then(Address::getCity)
 *
 * // used with LambdaQueryWrapper / 配合 LambdaQueryWrapper 使用
 * wrapper.eq(FieldPath.of(User::getAddress, Address::getCity), "NY");
 * // → {"address.city": "NY"} (each segment honors @CollectionField mapping
 * //   / 每一段均应用 @CollectionField 映射)
 * }</pre>
 * <p>
 * Instances are immutable — {@link #then(SFunction)} returns a new instance. Lambda
 * introspection happens eagerly at construction time and reuses the cached
 * {@code writeReplace} lookup in {@link ReflectUtil}. / 实例不可变——{@link #then(SFunction)}
 * 返回新实例。lambda 解析在构造时立即完成，并复用 {@link ReflectUtil} 中缓存的
 * {@code writeReplace} 查找。
 *
 * @param <T> root entity type the path starts from / 路径起始的根实体类型
 * @param <R> type of the property the path currently points to / 路径当前指向的属性类型
 */
public final class FieldPath<T, R> {

    /** Dot-separated Java field path, e.g. "address.city" / 点号分隔的 Java 字段路径，如 "address.city" */
    private final String javaPath;

    /** Declaring class of the first segment, used for field-name mapping / 首段的声明类，用于字段名映射 */
    private final Class<?> rootImplClass;

    private FieldPath(String javaPath, Class<?> rootImplClass) {
        this.javaPath = javaPath;
        this.rootImplClass = rootImplClass;
    }

    /**
     * Start a path from a single method reference. / 从单个方法引用开始构建路径。
     */
    public static <T, R> FieldPath<T, R> of(SFunction<T, R> first) {
        Objects.requireNonNull(first, "first must not be null");
        return new FieldPath<>(
                ReflectUtil.getFieldNameFromLambda(first),
                ReflectUtil.getImplClassFromLambda(first));
    }

    /**
     * Convenience factory for a two-level path. / 两级路径的便捷工厂方法。
     */
    public static <T, A, R> FieldPath<T, R> of(SFunction<T, A> first, SFunction<A, R> second) {
        return FieldPath.of(first).then(second);
    }

    /**
     * Convenience factory for a three-level path. / 三级路径的便捷工厂方法。
     */
    public static <T, A, B, R> FieldPath<T, R> of(SFunction<T, A> first, SFunction<A, B> second, SFunction<B, R> third) {
        return FieldPath.of(first).then(second).then(third);
    }

    /**
     * Append the next segment, navigating into the nested type. / 追加下一段路径，导航进入嵌套类型。
     */
    public <V> FieldPath<T, V> then(SFunction<R, V> next) {
        Objects.requireNonNull(next, "next must not be null");
        return new FieldPath<>(
                javaPath + "." + ReflectUtil.getFieldNameFromLambda(next),
                rootImplClass);
    }

    /**
     * The dot-separated Java field path, e.g. {@code "address.city"}. / 点号分隔的 Java 字段路径，如 {@code "address.city"}。
     */
    public String javaPath() {
        return javaPath;
    }

    /**
     * The declaring class of the first segment, used to look up field-name metadata. / 首段的声明类，用于查找字段名映射元数据。
     */
    public Class<?> rootImplClass() {
        return rootImplClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FieldPath)) return false;
        FieldPath<?, ?> other = (FieldPath<?, ?>) o;
        return Objects.equals(javaPath, other.javaPath)
                && Objects.equals(rootImplClass, other.rootImplClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(javaPath, rootImplClass);
    }

    @Override
    public String toString() {
        return "FieldPath[" + (rootImplClass != null ? rootImplClass.getSimpleName() : "?") + ": " + javaPath + ']';
    }
}
