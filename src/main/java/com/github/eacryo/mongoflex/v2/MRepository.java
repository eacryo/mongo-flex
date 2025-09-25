package com.github.eacryo.mongoflex.v2;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE) // 这个注解只能用于类、接口或枚举
@Retention(RetentionPolicy.RUNTIME)
public @interface MRepository {
}
