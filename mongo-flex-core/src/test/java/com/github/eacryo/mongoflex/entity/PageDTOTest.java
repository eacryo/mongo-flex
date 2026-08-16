/*
 * Copyright (c) 2024 mongo-flex contributors.
 * Licensed under the Apache License, Version 2.0.
 */
package com.github.eacryo.mongoflex.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PageDTO}: offset-based pagination state and the sortBy()
 * convenience methods (string field, lambda field, SortOrder varargs, expression). /
 * {@link PageDTO} 单元测试：offset 分页状态与 sortBy() 便捷方法
 * （字符串字段、lambda 字段、SortOrder 可变参数、表达式）。
 *
 * <p>Pure in-memory tests — no MongoDB connection required. / 纯内存测试——无需 MongoDB 连接。</p>
 */
@DisplayName("PageDTO offset + sortBy")
class PageDTOTest {

    static class Item {
        private String code;
        public String getCode() { return code; }
    }

    @Test
    @DisplayName("offset defaults to null (page-number mode) / offset 默认为 null（页码模式）")
    void offsetDefaultNull() {
        PageDTO<Item> page = new PageDTO<>();
        assertNull(page.getOffset());
        assertEquals(1L, page.getCurrentPage());
        assertEquals(10L, page.getPageSize());
    }

    @Test
    @DisplayName("offset set via Lombok setOffset(Long), fluent offset(long), and cleared with null / offset 通过 Lombok setOffset(Long)、流畅式 offset(long) 设置，并可用 null 清除")
    void offsetSetters() {
        PageDTO<Item> page = new PageDTO<>();
        page.setCurrentPage(3L);
        page.setOffset(25L);          // Lombok boxed setter / Lombok 装箱 setter
        assertEquals(Long.valueOf(25L), page.getOffset());
        assertEquals(3L, page.getCurrentPage());
        page.offset(5);               // fluent int-friendly / 流畅式 int 友好
        assertEquals(Long.valueOf(5L), page.getOffset());
        page.setOffset(null);         // clears offset mode / 清除 offset 模式
        assertNull(page.getOffset());
    }

    @Test
    @DisplayName("sortBy(String, boolean) appends in order / sortBy(字符串, 方向) 按顺序追加")
    void sortByStringAppends() {
        PageDTO<Item> page = new PageDTO<>();
        page.sortBy("code", true).sortBy("name", false);
        assertEquals(2, page.getOrderBy().size());
        assertEquals("code", page.getOrderBy().get(0).getField());
        assertTrue(page.getOrderBy().get(0).isAscending());
        assertEquals("name", page.getOrderBy().get(1).getField());
        assertFalse(page.getOrderBy().get(1).isAscending());
    }

    @Test
    @DisplayName("sortBy(SFunction, boolean) captures javaFieldName and implClass / sortBy(lambda, 方向) 捕获字段名与声明类")
    void sortByLambdaCapturesMetadata() {
        PageDTO<Item> page = new PageDTO<>();
        page.sortBy(Item::getCode, false);
        assertEquals(1, page.getOrderBy().size());
        SortOrder<Item> order = page.getOrderBy().get(0);
        assertEquals("code", order.getJavaFieldName());
        assertEquals(Item.class, order.getImplClass());
        assertFalse(order.isAscending());
    }

    @Test
    @DisplayName("sortBy(SortOrder...) varargs appends and ignores nulls / sortBy(SortOrder...) 追加并忽略 null")
    void sortByVarargs() {
        PageDTO<Item> page = new PageDTO<>();
        page.sortBy(new SortOrder<>("a", true), null, new SortOrder<>("b", false));
        assertEquals(2, page.getOrderBy().size());
        assertEquals("a", page.getOrderBy().get(0).getField());
        assertEquals("b", page.getOrderBy().get(1).getField());
    }

    @Test
    @DisplayName("sortBy(expression) parses field:asc|desc with default asc / sortBy(表达式) 解析 field:asc|desc，缺省升序")
    void sortByExpression() {
        PageDTO<Item> page = new PageDTO<>();
        page.sortBy("address:desc, level,  name:asc");
        assertEquals(3, page.getOrderBy().size());
        assertEquals("address", page.getOrderBy().get(0).getField());
        assertFalse(page.getOrderBy().get(0).isAscending());
        assertEquals("level", page.getOrderBy().get(1).getField());
        assertTrue(page.getOrderBy().get(1).isAscending()); // 缺省 asc
        assertEquals("name", page.getOrderBy().get(2).getField());
        assertTrue(page.getOrderBy().get(2).isAscending());
    }

    @Test
    @DisplayName("sortBy(expression) tolerates DESC uppercase, blank items and null/empty input / sortBy(表达式) 容忍大写 DESC、空段与 null/空输入")
    void sortByExpressionEdgeCases() {
        PageDTO<Item> page = new PageDTO<>();
        page.sortBy("a:DESC,, b : asc , ,");
        assertEquals(2, page.getOrderBy().size());
        assertFalse(page.getOrderBy().get(0).isAscending());
        assertTrue(page.getOrderBy().get(1).isAscending());

        PageDTO<Item> empty = new PageDTO<>();
        empty.sortBy((String) null).sortBy("").sortBy(" , ");
        assertNull(empty.getOrderBy());
    }

    @Test
    @DisplayName("sortBy appends after setOrderBy — both coexist / sortBy 在 setOrderBy 之后追加，两者共存")
    void sortByAppendsToExistingList() {
        PageDTO<Item> page = new PageDTO<>();
        page.setOrderBy(new java.util.ArrayList<>());
        page.getOrderBy().add(new SortOrder<>("x", true));
        page.sortBy("y", false);
        assertEquals(2, page.getOrderBy().size());
        assertEquals("x", page.getOrderBy().get(0).getField());
        assertEquals("y", page.getOrderBy().get(1).getField());
    }

    @Test
    @DisplayName("totalPage still computed from total/pageSize / totalPage 仍由 total/pageSize 计算")
    void totalPageComputed() {
        PageDTO<Item> page = new PageDTO<>();
        page.setPageSize(3L);
        page.setTotal(10L);
        assertEquals(4L, page.getTotalPage());
    }

    @Test
    @DisplayName("countTotal defaults to true and is switchable / countTotal 默认为 true 且可切换")
    void countTotalDefault() {
        PageDTO<Item> page = new PageDTO<>();
        assertTrue(page.isCountTotal());
        page.setCountTotal(false);
        assertFalse(page.isCountTotal());
    }

    @Test
    @DisplayName("navigation properties in page-number mode derive from total / 页码模式导航属性基于 total 推导")
    void navigationPageNumberMode() {
        // 10 docs, pageSize 3 → pages 4
        PageDTO<Item> first = new PageDTO<>();
        first.setCurrentPage(1L);
        first.setPageSize(3L);
        first.setTotal(10L);
        assertTrue(first.isFirst());
        assertFalse(first.hasPrevious());
        assertTrue(first.getHasNext());
        assertFalse(first.isLast());

        PageDTO<Item> mid = new PageDTO<>();
        mid.setCurrentPage(2L);
        mid.setPageSize(3L);
        mid.setTotal(10L);
        assertFalse(mid.isFirst());
        assertTrue(mid.hasPrevious());
        assertTrue(mid.getHasNext());
        assertFalse(mid.isLast());

        PageDTO<Item> last = new PageDTO<>();
        last.setCurrentPage(4L);
        last.setPageSize(3L);
        last.setTotal(10L);
        assertFalse(last.isFirst());
        assertTrue(last.hasPrevious());
        assertFalse(last.getHasNext());
        assertTrue(last.isLast());
    }

    @Test
    @DisplayName("navigation properties in offset mode derive from offset/total / offset 模式导航属性基于 offset/total 推导")
    void navigationOffsetMode() {
        PageDTO<Item> first = new PageDTO<>();
        first.offset(0L);
        first.setPageSize(3L);
        first.setTotal(10L);
        assertTrue(first.isFirst());
        assertFalse(first.hasPrevious());
        assertTrue(first.getHasNext());
        assertFalse(first.isLast());

        PageDTO<Item> last = new PageDTO<>();
        last.offset(9L);
        last.setPageSize(3L);
        last.setTotal(10L);
        assertFalse(last.isFirst());
        assertTrue(last.hasPrevious());
        assertFalse(last.getHasNext());
        assertTrue(last.isLast());
    }

    @Test
    @DisplayName("getHasNext prefers the executor-filled value over total derivation / getHasNext 优先使用执行器填充值")
    void hasNextPrefersFilledValue() {
        // total says there IS a next page, but the executor's extra-document fetch says no
        PageDTO<Item> page = new PageDTO<>();
        page.setCurrentPage(1L);
        page.setPageSize(3L);
        page.setTotal(10L);
        page.setHasNext(false);
        assertFalse(page.getHasNext());
        assertTrue(page.isLast());

        page.setHasNext(null); // back to total-derived / 回到基于 total 推导
        assertTrue(page.getHasNext());
        assertFalse(page.isLast());
    }
}
