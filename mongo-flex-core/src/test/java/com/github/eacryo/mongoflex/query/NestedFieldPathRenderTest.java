/*
 * Copyright (c) 2024 mongo-flex contributors.
 * Licensed under the Apache License, Version 2.0.
 */
package com.github.eacryo.mongoflex.query;

import com.github.eacryo.mongoflex.annotation.CollectionField;
import com.github.eacryo.mongoflex.convertor.MongoMappingConvertor;
import com.github.eacryo.mongoflex.util.FieldPath;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.Filters;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for rendering nested {@link FieldPath} conditions to Bson filters. /
 * 嵌套 {@link FieldPath} 条件渲染为 Bson 过滤器的单元测试。
 *
 * <p>Pure in-memory tests — no MongoDB connection required. / 纯内存测试——无需 MongoDB 连接。</p>
 */
@DisplayName("MongoBsonRenderer nested FieldPath rendering")
class NestedFieldPathRenderTest {

    // ---- Test fixtures / 测试实体 ----

    static class Country {
        @CollectionField("iso_code")
        private String isoCode;
        public String getIsoCode() { return isoCode; }
    }

    static class Address {
        private String city;
        @CollectionField("street_name")
        private String street;
        private Country country;
        public String getCity() { return city; }
        public String getStreet() { return street; }
        public Country getCountry() { return country; }
    }

    static class Profile {
        private List<Address> addresses;
        public List<Address> getAddresses() { return addresses; }
    }

    static class User {
        private String name;
        @CollectionField("addr")
        private Address address;
        private Profile profile;
        public String getName() { return name; }
        public Address getAddress() { return address; }
        public Profile getProfile() { return profile; }
    }

    private final MongoMappingConvertor convertor = new MongoMappingConvertor();

    private static BsonDocument toBsonDoc(Bson bson) {
        return bson.toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());
    }

    private static void assertRendered(Bson expected, LambdaQueryWrapper<?> wrapper, MongoMappingConvertor convertor) {
        assertEquals(toBsonDoc(expected), toBsonDoc(MongoBsonRenderer.render(wrapper, convertor)));
    }

    @Test
    @DisplayName("eq with two-level path applies @CollectionField on the root segment / 两级路径 eq，首段应用 @CollectionField")
    void eqTwoLevelPath() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>(User.class)
                .eq(FieldPath.of(User::getAddress, Address::getCity), "NY");
        assertRendered(Filters.and(Filters.eq("addr.city", "NY")), wrapper, convertor);
    }

    @Test
    @DisplayName("eq with leaf segment renamed via @CollectionField / 末段 @CollectionField 重命名")
    void eqLeafRenamed() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>(User.class)
                .eq(FieldPath.of(User::getAddress).then(Address::getStreet), "Main St");
        assertRendered(Filters.and(Filters.eq("addr.street_name", "Main St")), wrapper, convertor);
    }

    @Test
    @DisplayName("three-level path renames every segment / 三级路径每段重命名")
    void threeLevelPath() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>(User.class)
                .eq(FieldPath.of(User::getAddress, Address::getCountry, Country::getIsoCode), "US");
        assertRendered(Filters.and(Filters.eq("addr.country.iso_code", "US")), wrapper, convertor);
    }

    @Test
    @DisplayName("mixing SFunction and FieldPath conditions in one wrapper / 单字段与嵌套路径条件混用")
    void mixedConditions() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>(User.class)
                .eq(User::getName, "Tom")
                .gt(FieldPath.of(User::getAddress, Address::getCity), "A");
        assertRendered(
                Filters.and(Filters.eq("name", "Tom"), Filters.gt("addr.city", "A")),
                wrapper, convertor);
    }

    @Test
    @DisplayName("between on a nested path / 嵌套路径 between")
    void betweenNestedPath() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>(User.class)
                .between(FieldPath.of(User::getAddress, Address::getCity), "A", "M");
        assertRendered(
                Filters.and(Filters.and(Filters.gte("addr.city", "A"), Filters.lte("addr.city", "M"))),
                wrapper, convertor);
    }

    @Test
    @DisplayName("elemMatch traverses a nested list path / elemMatch 穿透嵌套 List 路径")
    void elemMatchNestedListPath() {
        LambdaQueryWrapper<Address> sub = new LambdaQueryWrapper<Address>()
                .eq(Address::getStreet, "Main St");
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>(User.class)
                .elemMatch(FieldPath.of(User::getProfile, Profile::getAddresses), sub);
        assertRendered(
                Filters.and(Filters.elemMatch("profile.addresses",
                        Filters.and(Filters.eq("street_name", "Main St")))),
                wrapper, convertor);
    }

    @Test
    @DisplayName("or groups work with nested paths / OR 分组支持嵌套路径")
    void orGroupsWithNestedPath() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>(User.class)
                .eq(FieldPath.of(User::getAddress, Address::getCity), "NY")
                .or()
                .eq(FieldPath.of(User::getAddress, Address::getCity), "LA");
        assertRendered(
                Filters.or(
                        Filters.and(Filters.eq("addr.city", "NY")),
                        Filters.and(Filters.eq("addr.city", "LA"))),
                wrapper, convertor);
    }

    @Test
    @DisplayName("orderBy and projection carry the nested Java path / 排序与投影携带嵌套 Java 路径")
    void orderByAndProjectionCarryPath() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>(User.class)
                .orderByAsc(FieldPath.of(User::getAddress, Address::getCity))
                .include(FieldPath.of(User::getAddress, Address::getStreet));

        assertEquals(1, wrapper.getOrderBys().size());
        assertEquals("address.city", wrapper.getOrderBys().get(0).getJavaFieldName());
        assertEquals(User.class, wrapper.getOrderBys().get(0).getImplClass());
        assertEquals("addr.city",
                convertor.resolveMongoFieldPath(User.class, wrapper.getOrderBys().get(0).getJavaFieldName()));

        assertEquals(1, wrapper.getProjections().size());
        assertEquals("address.street", wrapper.getProjections().get(0).getJavaFieldName());
        assertEquals("addr.street_name",
                convertor.resolveMongoFieldPath(User.class, wrapper.getProjections().get(0).getJavaFieldName()));
    }
}
