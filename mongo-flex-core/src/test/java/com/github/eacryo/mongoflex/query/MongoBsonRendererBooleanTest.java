/*
 * Copyright (c) 2024 mongo-flex contributors.
 * Licensed under the Apache License, Version 2.0.
 */
package com.github.eacryo.mongoflex.query;

import com.github.eacryo.mongoflex.annotation.CollectionField;
import com.github.eacryo.mongoflex.convertor.MongoMappingConvertor;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.Filters;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for nested boolean group composition (and/or/not) in {@link LambdaQueryWrapper}. /
 * {@link LambdaQueryWrapper} 嵌套布尔分组（and/or/not）渲染的单元测试。
 *
 * <p>Pure in-memory tests — no MongoDB connection required. / 纯内存测试——无需 MongoDB 连接。</p>
 */
@DisplayName("MongoBsonRenderer nested boolean groups")
class MongoBsonRendererBooleanTest {

    // ---- Test fixtures / 测试实体 ----

    static class Product {
        @CollectionField("sku_code")
        private String sku;
        private String category;
        private Double price;
        public String getSku() { return sku; }
        public String getCategory() { return category; }
        public Double getPrice() { return price; }
    }

    private final MongoMappingConvertor convertor = new MongoMappingConvertor();

    private static BsonDocument toBsonDoc(Bson bson) {
        return bson.toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());
    }

    private static void assertRendered(Bson expected, LambdaQueryWrapper<?> wrapper, MongoMappingConvertor convertor) {
        assertEquals(toBsonDoc(expected), toBsonDoc(MongoBsonRenderer.render(wrapper, convertor)));
    }

    @Test
    @DisplayName("A AND (B OR C) via and() + or() separator / and() 嵌套 + or() 分隔符实现 A AND (B OR C)")
    void andWithOrSeparatorInside() {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>(Product.class)
                .eq(Product::getCategory, "food")
                .and(x -> x.eq(Product::getSku, "S1").or().eq(Product::getSku, "S2"));
        assertRendered(
                Filters.and(
                        Filters.eq("category", "food"),
                        Filters.or(
                                Filters.and(Filters.eq("sku_code", "S1")),
                                Filters.and(Filters.eq("sku_code", "S2")))),
                wrapper, convertor);
    }

    @Test
    @DisplayName("A AND (B OR C) via nested or(consumer) / 嵌套 or(consumer) 实现 A AND (B OR C)")
    void andWithOrConsumerInside() {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>(Product.class)
                .eq(Product::getCategory, "food")
                .and(x -> x.eq(Product::getPrice, 1.0).or(y -> y.eq(Product::getPrice, 2.0)));
        assertRendered(
                Filters.and(
                        Filters.eq("category", "food"),
                        Filters.and(
                                Filters.eq("price", 1.0),
                                Filters.or(Filters.and(Filters.eq("price", 2.0))))),
                wrapper, convertor);
    }

    @Test
    @DisplayName("(A OR B) AND C / or(consumer) 嵌套实现 (A OR B) AND C")
    void orGroupAndedWithSibling() {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>(Product.class)
                .or(x -> x.eq(Product::getSku, "S1").eq(Product::getCategory, "food"))
                .eq(Product::getPrice, 9.9);
        assertRendered(
                Filters.and(
                        Filters.or(Filters.and(
                                Filters.eq("sku_code", "S1"),
                                Filters.eq("category", "food"))),
                        Filters.eq("price", 9.9)),
                wrapper, convertor);
    }

    @Test
    @DisplayName("A AND NOT (B OR C) / not(consumer) 嵌套实现 A AND NOT (B OR C)")
    void notGroup() {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>(Product.class)
                .eq(Product::getCategory, "food")
                .not(x -> x.eq(Product::getSku, "S1").or().eq(Product::getSku, "S2"));
        assertRendered(
                Filters.and(
                        Filters.eq("category", "food"),
                        Filters.nor(Filters.or(
                                Filters.and(Filters.eq("sku_code", "S1")),
                                Filters.and(Filters.eq("sku_code", "S2"))))),
                wrapper, convertor);
    }

    @Test
    @DisplayName("deep nesting: A AND (B OR (C AND D)) / 深层嵌套")
    void deepNesting() {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>(Product.class)
                .eq(Product::getCategory, "food")
                .and(x -> x.eq(Product::getSku, "S1")
                        .or(y -> y.eq(Product::getSku, "S2").eq(Product::getPrice, 3.0)));
        assertRendered(
                Filters.and(
                        Filters.eq("category", "food"),
                        Filters.and(
                                Filters.eq("sku_code", "S1"),
                                Filters.or(Filters.and(
                                        Filters.eq("sku_code", "S2"),
                                        Filters.eq("price", 3.0))))),
                wrapper, convertor);
    }

    @Test
    @DisplayName("empty nested groups contribute nothing / 空嵌套分组不贡献条件")
    void emptyNestedGroupSkipped() {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>(Product.class)
                .eq(Product::getCategory, "food")
                .and(x -> { })
                .or(y -> { })
                .eq(Product::getPrice, 1.0);
        assertRendered(
                Filters.and(
                        Filters.eq("category", "food"),
                        Filters.eq("price", 1.0)),
                wrapper, convertor);
    }

    @Test
    @DisplayName("nested groups resolve @CollectionField without entityClass / 无 entityClass 时嵌套组仍解析 @CollectionField")
    void nestedGroupsWithoutEntityClass() {
        // No entityClass on the wrapper — field mapping comes from the lambda's implClass. /
        // wrapper 未设置 entityClass——字段映射由 lambda 的 implClass 提供。
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getCategory, "food")
               .and(x -> x.eq(Product::getSku, "S1").or().eq(Product::getSku, "S2"));
        assertRendered(
                Filters.and(
                        Filters.eq("category", "food"),
                        Filters.or(
                                Filters.and(Filters.eq("sku_code", "S1")),
                                Filters.and(Filters.eq("sku_code", "S2")))),
                wrapper, convertor);
    }

    @Test
    @DisplayName("backward compat: top-level or() separator / 兼容：顶层 or() 分隔符")
    void legacyOrSeparatorStillWorks() {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>(Product.class)
                .eq(Product::getCategory, "food")
                .or()
                .eq(Product::getCategory, "drink");
        assertRendered(
                Filters.or(
                        Filters.and(Filters.eq("category", "food")),
                        Filters.and(Filters.eq("category", "drink"))),
                wrapper, convertor);
    }

    @Test
    @DisplayName("hasEffectiveConditions: recursive check over nested groups / 递归有效性检查")
    void hasEffectiveConditions() {
        assertFalse(LambdaQueryWrapper.hasEffectiveConditions(new LambdaQueryWrapper<>(Product.class)));
        assertFalse(LambdaQueryWrapper.hasEffectiveConditions(
                new LambdaQueryWrapper<>(Product.class).or()));
        assertFalse(LambdaQueryWrapper.hasEffectiveConditions(
                new LambdaQueryWrapper<>(Product.class).and(x -> { })));
        assertFalse(LambdaQueryWrapper.hasEffectiveConditions(
                new LambdaQueryWrapper<>(Product.class)
                        .and(x -> x.and(y -> { }))));
        assertTrue(LambdaQueryWrapper.hasEffectiveConditions(
                new LambdaQueryWrapper<>(Product.class).eq(Product::getCategory, "food")));
        assertTrue(LambdaQueryWrapper.hasEffectiveConditions(
                new LambdaQueryWrapper<Product>().and(x -> x.eq(Product::getSku, "S1"))));
        assertTrue(LambdaQueryWrapper.hasEffectiveConditions(
                new LambdaQueryWrapper<Product>()
                        .and(x -> x.or(y -> y.eq(Product::getSku, "S1")))));
        assertTrue(LambdaQueryWrapper.hasEffectiveConditions(
                new LambdaQueryWrapper<Product>().not(x -> x.eq(Product::getSku, "S1"))));
    }
}
