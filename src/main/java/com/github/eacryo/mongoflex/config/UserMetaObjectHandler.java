package com.github.eacryo.mongoflex.config;

import org.springframework.data.mongodb.core.query.Update;

public interface UserMetaObjectHandler {
    void insertFill(Object object);
    void updateFill(Update update);
    void deleteFill(Object object);
}
