package com.github.eacryo.mongoflex.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UpdateDate {
    //当字段类型为string时的日期格式化
    String pattern() default "yyyy-MM-dd HH:mm:ss";
}
