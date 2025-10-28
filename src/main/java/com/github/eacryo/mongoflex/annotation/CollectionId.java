package com.github.eacryo.mongoflex.annotation;

import com.github.eacryo.mongoflex.constant.IdType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CollectionId {
    IdType value() default IdType.NONE;
}
