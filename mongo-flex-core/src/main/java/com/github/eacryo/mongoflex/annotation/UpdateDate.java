package com.github.eacryo.mongoflex.annotation;

import com.github.eacryo.mongoflex.config.DateValueProvider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a field to be auto-filled with the current date/time on every insert and update. / 标记字段在每次插入和更新时自动填充当前日期/时间。
 * <p>
 * Resolution order / 解析优先级：
 * <ol>
 *   <li>{@link #providerClass()} — per-field custom provider (if set) / 按字段的自定义提供器（如有设置）</li>
 *   <li>Global {@link DateValueProvider} Spring bean (if any) / 全局 {@link DateValueProvider} Spring bean（如有）</li>
 *   <li>Built-in type table / 内置类型表</li>
 * </ol>
 *
 * @see DateValueProvider
 * @see CreateDate
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UpdateDate {
    /** Date format pattern when the field type is String. / 当字段类型为 String 时的日期格式化模式。 */
    String pattern() default "yyyy-MM-dd HH:mm:ss";

    /**
     * Custom date value provider class. / 自定义日期值提供器类。
     * The class must implement {@link DateValueProvider} and have a no-arg constructor. / 该类必须实现 {@link DateValueProvider} 并具有无参构造函数。
     * If not specified (defaults to {@code DateValueProvider.None}), falls back to the global bean or built-in types. / 如果未指定（默认为 {@code DateValueProvider.None}），回退到全局 bean 或内置类型。
     */
    Class<? extends DateValueProvider> providerClass() default DateValueProvider.None.class;
}
