package com.github.eacryo.mongoflex.util;

import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

public class DateValueGenerator {
    private static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final ConcurrentHashMap<String, DateTimeFormatter> FORMATTER_CACHE = new ConcurrentHashMap<>();

    private static DateTimeFormatter getFormatter(String pattern) {
        return FORMATTER_CACHE.computeIfAbsent(pattern, DateTimeFormatter::ofPattern);
    }

    public static Object generateCurrentDate(Class<?> fieldType){
        return generateCurrentDate(fieldType, DEFAULT_PATTERN);
    }

    public static Object generateCurrentDate(Class<?> fieldType,String pattern){
        if (fieldType == Date.class){
            return new Date();
        }
        if (fieldType == String.class){
            DateTimeFormatter formatter = getFormatter(pattern);
            return java.time.LocalDateTime.now().format(formatter);
        }
        throw new IllegalArgumentException("@CreateDate/@UpdateDate not support for type: " + fieldType.getName());
    }
}
