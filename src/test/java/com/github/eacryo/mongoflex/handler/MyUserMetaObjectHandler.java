package com.github.eacryo.mongoflex.handler;

import com.github.eacryo.mongoflex.config.UserMetaObjectHandler;
import com.github.eacryo.mongoflex.util.ReflectUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Date;

@Component
@Slf4j
public class MyUserMetaObjectHandler implements UserMetaObjectHandler {
    @Override
    public void insertFill(Object object) {
        log.info("insert fill start");
        fill(object, "createDate", Date.class, new Date());
        fill(object, "updateDate", Date.class, new Date());
        fill(object, "description", String.class, "This is a description");
        log.info("insert fill end");
    }

    @Override
    public void updateFill(Object object) {
        log.info("update fill start");
        fill(object, "updateDate", Date.class, new Date());
        log.info("update fill end");
    }

    @Override
    public void deleteFill(Object object) {

    }

    private void fill(Object object, String filedName, Class<?> fieldType, Object fieldValue) {
        Field filed = ReflectUtil.getFiled(object.getClass(), filedName);
        if (filed == null || !filed.getType().equals(fieldType)) {
            log.warn("字段{}不存在或类型错误", filedName);
            return;
        }
        filed.setAccessible(true);
        try {
            filed.set(object, fieldValue);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
