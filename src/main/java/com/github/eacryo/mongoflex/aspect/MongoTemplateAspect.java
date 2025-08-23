package com.github.eacryo.mongoflex.aspect;

import com.github.eacryo.mongoflex.util.ReflectUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Aspect
@Component
@Slf4j
public class MongoTemplateAspect {

    @Around("execution(* org.springframework.data.mongodb.core.MongoTemplate.insert(Object, String)) ||" +
            "execution(* org.springframework.data.mongodb.core.MongoTemplate.save(Object, String))")
    public Object handleInsertOrSave(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("命中insert,save操作");
        Object[] args = joinPoint.getArgs();
        Object entity = args[0];
        setCreateDateField(entity);
        return joinPoint.proceed();
    }


    @SneakyThrows
    private void setCreateDateField(Object entity) {
        List<Field> fields = ReflectUtil.getAllFieldsIncludingInherited(entity.getClass());
        Field createDateField = ReflectUtil.getCreateDateField(fields);
        if (createDateField != null) {
            createDateField.setAccessible(true);
            Class<?> fieldType = createDateField.getType();
            Object o = null;
            if (Objects.equals(fieldType, Date.class)) {
                o = new Date();
            }
//            if (Objects.equals(fieldType, Instant.class)) {
//                o = Instant.now();
//            }
//            if (Objects.equals(fieldType, LocalDateTime.class)) {
//                o = LocalDateTime.now();
//            }
            createDateField.set(entity, o);
        }
    }
}
