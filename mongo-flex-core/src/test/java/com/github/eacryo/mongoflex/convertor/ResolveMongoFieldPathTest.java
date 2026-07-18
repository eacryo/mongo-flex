/*
 * Copyright (c) 2024 mongo-flex contributors.
 * Licensed under the Apache License, Version 2.0.
 */
package com.github.eacryo.mongoflex.convertor;

import com.github.eacryo.mongoflex.annotation.CollectionField;
import com.github.eacryo.mongoflex.annotation.CollectionId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link MongoMappingConvertor#resolveMongoFieldPath(Class, String)}. /
 * {@link MongoMappingConvertor#resolveMongoFieldPath(Class, String)} 的单元测试。
 *
 * <p>Pure in-memory tests — no MongoDB connection required. / 纯内存测试——无需 MongoDB 连接。</p>
 */
@DisplayName("MongoMappingConvertor.resolveMongoFieldPath")
class ResolveMongoFieldPathTest {

    // ---- Test fixtures / 测试实体 ----

    static class Country {
        private String name;
        @CollectionField("iso_code")
        private String isoCode;
    }

    static class Address {
        private String city;
        @CollectionField("street_name")
        private String street;
        private Country country;
        private String id;
    }

    static class User {
        @CollectionId
        private String id;
        private String name;
        @CollectionField("addr")
        private Address address;
        private Address billingAddress;
        private List<Address> previousAddresses;
        private List<String> tags;
    }

    private final MongoMappingConvertor convertor = new MongoMappingConvertor();

    // ---- Single segment — legacy behavior / 单段——旧行为回归 ----

    @Test
    @DisplayName("single segment: plain field name is returned as-is / 单段：普通字段名原样返回")
    void singleSegmentPlain() {
        assertEquals("name", convertor.resolveMongoFieldPath(User.class, "name"));
    }

    @Test
    @DisplayName("single segment: id maps to _id / 单段：id 映射为 _id")
    void singleSegmentId() {
        assertEquals("_id", convertor.resolveMongoFieldPath(User.class, "id"));
    }

    @Test
    @DisplayName("single segment: @CollectionField rename / 单段：@CollectionField 重命名")
    void singleSegmentCollectionField() {
        assertEquals("addr", convertor.resolveMongoFieldPath(User.class, "address"));
    }

    @Test
    @DisplayName("single segment: unknown field falls back to raw name / 单段：未知字段回退为原名")
    void singleSegmentUnknown() {
        assertEquals("nonexistent", convertor.resolveMongoFieldPath(User.class, "nonexistent"));
    }

    // ---- Multi segment / 多段路径 ----

    @Test
    @DisplayName("two segments: plain nested path / 两段：普通嵌套路径")
    void twoSegmentsPlain() {
        assertEquals("billingAddress.city", convertor.resolveMongoFieldPath(User.class, "billingAddress.city"));
    }

    @Test
    @DisplayName("two segments: @CollectionField applied on the first segment / 两段：首段应用 @CollectionField")
    void twoSegmentsRootRenamed() {
        assertEquals("addr.city", convertor.resolveMongoFieldPath(User.class, "address.city"));
    }

    @Test
    @DisplayName("two segments: @CollectionField applied on the last segment / 两段：末段应用 @CollectionField")
    void twoSegmentsLeafRenamed() {
        assertEquals("addr.street_name", convertor.resolveMongoFieldPath(User.class, "address.street"));
    }

    @Test
    @DisplayName("three segments: renames applied on every segment / 三段：每一段都应用重命名")
    void threeSegments() {
        assertEquals("addr.country.iso_code",
                convertor.resolveMongoFieldPath(User.class, "address.country.isoCode"));
    }

    @Test
    @DisplayName("nested id segment maps to _id, consistent with write() / 嵌套段中的 id 映射为 _id，与 write() 行为一致")
    void nestedIdSegment() {
        assertEquals("addr._id", convertor.resolveMongoFieldPath(User.class, "address.id"));
    }

    @Test
    @DisplayName("list segment traverses into generic element type / List 段穿透到泛型元素类型")
    void listSegmentTraversal() {
        assertEquals("previousAddresses.street_name",
                convertor.resolveMongoFieldPath(User.class, "previousAddresses.street"));
    }

    @Test
    @DisplayName("unknown middle segment: remaining segments kept as-is / 未知中间段：其后所有段原样保留")
    void unknownMiddleSegment() {
        assertEquals("name.city.zip", convertor.resolveMongoFieldPath(User.class, "name.city.zip"));
    }

    @Test
    @DisplayName("unknown root segment: whole path kept as-is / 未知首段：整条路径原样保留")
    void unknownRootSegment() {
        assertEquals("ghost.city", convertor.resolveMongoFieldPath(User.class, "ghost.city"));
    }

    // ---- getFieldGenericElementType with paths / 点号路径的泛型元素类型解析 ----

    @Test
    @DisplayName("getFieldGenericElementType supports dot paths / getFieldGenericElementType 支持点号路径")
    void genericElementTypeByPath() {
        assertEquals(Address.class, convertor.getFieldGenericElementType(User.class, "previousAddresses"));
        assertEquals(String.class, convertor.getFieldGenericElementType(User.class, "tags"));
        assertNull(convertor.getFieldGenericElementType(User.class, "address.city.ghost"));
    }
}
