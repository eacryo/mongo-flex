package com.github.eacryo.mongoflex.aspect;

import com.github.eacryo.mongoflex.annotation.CollectionId;
import com.github.eacryo.mongoflex.config.IdGenerator;
import com.github.eacryo.mongoflex.config.UserMetaObjectHandler;
import com.github.eacryo.mongoflex.constant.IdType;
import com.github.eacryo.mongoflex.util.ReflectUtil;
import com.github.f4b6a3.ulid.UlidCreator;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Aspect
@Component
@Slf4j
public class MongoTemplateAspect {

    @Autowired(required = false)
    private UserMetaObjectHandler userMetaObjectHandler;
    @Autowired(required = false)
    private IdGenerator<?> idGenerator;

    //这里只拦截insert不拦截save，save语义不清晰无法判断到底是insert还是update
    @Around("execution(* org.springframework.data.mongodb.core.MongoTemplate.insert(..))")
    public Object handleInsert(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("命中insert,save操作");
        Object[] args = joinPoint.getArgs();
        if (args[0] instanceof Collection<?> collection) {
            collection.forEach(this::fillForInsert);
        } else {
            fillForInsert(args[0]);
        }
        return joinPoint.proceed();
    }


    @Around("execution(* org.springframework.data.mongodb.core.MongoTemplate.updateFirst(..)) || " +
            "execution(* org.springframework.data.mongodb.core.MongoTemplate.updateMulti(..))")
    public Object handleUpdate(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("命中update操作");
        Object[] args = joinPoint.getArgs();
        if (args[1] instanceof Update update && userMetaObjectHandler != null) {
            userMetaObjectHandler.updateFill(update);
        }
        return joinPoint.proceed();
    }

    private void fillForInsert(Object entity) {
        setCreateDateField(entity);
        setIdField(entity);
        if (userMetaObjectHandler != null) {
            userMetaObjectHandler.insertFill(entity);
        }
    }

    private void fillForUpdate(Update update) {
        userMetaObjectHandler.updateFill(update);
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
            if (Objects.equals(fieldType, Instant.class)) {
                o = Instant.now();
            }
            if (Objects.equals(fieldType, LocalDateTime.class)) {
                o = LocalDateTime.now();
            }
            createDateField.set(entity, o);
        }
    }

    @SneakyThrows
    private void setUpdateDateField() {

    }

    @SneakyThrows
    private void setIdField(Object entity) {
        List<Field> fields = ReflectUtil.getAllFieldsIncludingInherited(entity.getClass());
        Field idField = ReflectUtil.getIdFieldNotThrow(fields);
        if (idField == null) return;
        //如果id字段有值，不做操作
        idField.setAccessible(true);
        if (Objects.nonNull(idField.get(entity))) return;
        CollectionId annotation = idField.getAnnotation(CollectionId.class);
        if (annotation.value().equals(IdType.NONE)) {
            return;
        }
        if (annotation.value().equals(IdType.ULID)) {
            idField.set(entity, UlidCreator.getUlid().toString());
        }
        if (annotation.value().equals(IdType.UUID)) {
            idField.set(entity, UUID.randomUUID().toString());
        }
        if (annotation.value().equals(IdType.INPUT)){
            if (idGenerator == null)
                throw new IllegalArgumentException("未找到idGenerator的实现类");
            idField.set(entity, idGenerator.create());
        }
    }
}
