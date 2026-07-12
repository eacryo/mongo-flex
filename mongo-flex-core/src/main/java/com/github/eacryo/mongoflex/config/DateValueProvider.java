package com.github.eacryo.mongoflex.config;

/**
 * Custom date/time value provider for {@code @CreateDate} / {@code @UpdateDate} fields. / 自定义日期/时间值提供器，用于 {@code @CreateDate} / {@code @UpdateDate} 字段。
 * <p>
 * Implement this interface and reference it via {@code @CreateDate(providerClass = ...)} or
 * {@code @UpdateDate(providerClass = ...)} to handle custom field types, or register a Spring bean
 * to serve as the global fallback. / 实现此接口并通过 {@code @CreateDate(providerClass = ...)} 或 {@code @UpdateDate(providerClass = ...)} 引用来处理自定义字段类型，或注册 Spring bean 作为全局回退。
 * <p>
 * Return {@code null} to fall through to the built-in type table or global bean. / 返回 {@code null} 则回退到内置类型表或全局 bean。
 *
 * <h3>Per-field usage / 按字段使用</h3>
 * <pre>{@code
 * @CreateDate(providerClass = MyZonedDateTimeProvider.class)
 * private ZonedDateTime createAt;
 * }</pre>
 *
 * <h3>Global bean usage / 全局 bean 使用</h3>
 * <pre>{@code
 * @Component
 * public class MyDateProvider implements DateValueProvider {
 *     @Override
 *     public Object generateCurrentDate(Class<?> fieldType, String pattern) {
 *         if (fieldType == ZonedDateTime.class) {
 *             return ZonedDateTime.now();
 *         }
 *         return null; // let built-in handler take over / 交给内置处理器
 *     }
 * }
 * }</pre>
 */
@FunctionalInterface
public interface DateValueProvider {

    /**
     * Generate a current date/time value for the given field type and pattern. / 为给定的字段类型和格式生成当前日期/时间值。
     *
     * @param fieldType the Java type of the annotated field / 被注解字段的 Java 类型
     * @param pattern   the date format pattern from {@code @CreateDate} or {@code @UpdateDate} / {@code @CreateDate} 或 {@code @UpdateDate} 中指定的日期格式
     * @return the generated value, or {@code null} to let the built-in handler take over / 生成的值，或返回 {@code null} 交由内置处理器处理
     */
    Object generateCurrentDate(Class<?> fieldType, String pattern);

    /**
     * Sentinel type indicating no custom provider class is specified in annotation. / 哨兵类型，表示注解中未指定自定义提供器类。
     * Do not use this class directly. / 不要直接使用此类。
     */
    final class None implements DateValueProvider {
        private None() {}
        @Override
        public Object generateCurrentDate(Class<?> fieldType, String pattern) {
            throw new UnsupportedOperationException("None is a sentinel, not a real provider / None 是哨兵，不是真正的提供器");
        }
    }
}
