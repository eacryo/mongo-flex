package com.github.eacryo.mongoflex.v2;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Mql {
    String value();
    // MongoDB查询JSON字符串，例如: "{ name: #{name}, age: #{age} }"
    // 这里使用Mybatis风格的参数占位符#{paramName}，而不使用spring-data-mongodb的占位符?0
}
