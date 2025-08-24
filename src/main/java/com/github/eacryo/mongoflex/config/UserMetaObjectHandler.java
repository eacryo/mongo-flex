package com.github.eacryo.mongoflex.config;

public interface UserMetaObjectHandler {
    void insertFill(Object object);
    void updateFill(Object object);
    void deleteFill(Object object);
}
