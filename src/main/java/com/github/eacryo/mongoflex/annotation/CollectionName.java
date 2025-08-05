package com.github.eacryo.mongoflex.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME) //必须有这个注解才能在运行时获取到该注解
//标注在和数据库相关的bean上面，用来指示该bean对应的数据库集合名称
public @interface CollectionName {
    String value() default "";
}
