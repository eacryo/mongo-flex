/*
 * Copyright (c) 2024 mongo-flex contributors.
 * Licensed under the Apache License, Version 2.0.
 */
package com.github.eacryo.mongoflex.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link FieldPath} — chained lambda method-reference paths. /
 * {@link FieldPath} 的单元测试——链式 lambda 方法引用路径。
 */
@DisplayName("FieldPath")
class FieldPathTest {

    // ---- Test fixtures / 测试实体 ----

    static class Country {
        private String isoCode;
        public String getIsoCode() { return isoCode; }
    }

    static class Address {
        private String city;
        private Country country;
        public String getCity() { return city; }
        public Country getCountry() { return country; }
    }

    static class User {
        private Address address;
        private boolean active;
        public Address getAddress() { return address; }
        public boolean isActive() { return active; }
    }

    @Test
    @DisplayName("single-level path / 单级路径")
    void singleLevel() {
        FieldPath<User, Address> path = FieldPath.of(User::getAddress);
        assertEquals("address", path.javaPath());
        assertEquals(User.class, path.rootImplClass());
    }

    @Test
    @DisplayName("two-level path via then() / then() 两级路径")
    void twoLevelsViaThen() {
        FieldPath<User, String> path = FieldPath.of(User::getAddress).then(Address::getCity);
        assertEquals("address.city", path.javaPath());
        assertEquals(User.class, path.rootImplClass());
    }

    @Test
    @DisplayName("two-level convenience factory / 两级便捷工厂")
    void twoLevelFactory() {
        FieldPath<User, String> path = FieldPath.of(User::getAddress, Address::getCity);
        assertEquals("address.city", path.javaPath());
        assertEquals(User.class, path.rootImplClass());
    }

    @Test
    @DisplayName("three-level convenience factory / 三级便捷工厂")
    void threeLevelFactory() {
        FieldPath<User, String> path = FieldPath.of(User::getAddress, Address::getCountry, Country::getIsoCode);
        assertEquals("address.country.isoCode", path.javaPath());
        assertEquals(User.class, path.rootImplClass());
    }

    @Test
    @DisplayName("is-prefixed getter resolves per JavaBeans convention / is 前缀 getter 按 JavaBeans 规范解析")
    void isPrefixedGetter() {
        FieldPath<User, Boolean> path = FieldPath.of(User::isActive);
        assertEquals("active", path.javaPath());
    }

    @Test
    @DisplayName("then() returns a new immutable instance / then() 返回新的不可变实例")
    void immutability() {
        FieldPath<User, Address> base = FieldPath.of(User::getAddress);
        FieldPath<User, String> extended = base.then(Address::getCity);
        assertEquals("address", base.javaPath());
        assertEquals("address.city", extended.javaPath());
        assertNotEquals(base, extended);
    }

    @Test
    @DisplayName("equals/hashCode based on path and root class / equals/hashCode 基于路径与根类")
    void equality() {
        FieldPath<User, String> a = FieldPath.of(User::getAddress, Address::getCity);
        FieldPath<User, String> b = FieldPath.of(User::getAddress).then(Address::getCity);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("null arguments are rejected / 拒绝 null 参数")
    void nullRejection() {
        assertThrows(NullPointerException.class, () -> FieldPath.of(null));
        assertThrows(NullPointerException.class, () -> FieldPath.of(User::getAddress).then(null));
    }
}
