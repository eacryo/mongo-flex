package com.github.eacryo.mongoflex.v2;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueryParserTest {

    private final QueryParser parser = new QueryParser();

    // ======================== parse() tests ========================

    @Test
    @DisplayName("单参数 find 查询")
    void testSingleArgFind() {
        QueryParser.QueryCommand cmd = parser.parse(
                "db.getCollection('users').find({'name':'Alice'})");

        assertEquals("users", cmd.collectionName);
        assertEquals("find", cmd.operation);
        assertEquals(1, cmd.arguments.size());
        assertEquals("Alice", cmd.arguments.get(0).getString("name"));
    }

    @Test
    @DisplayName("空参数 find() → 生成一个空 Document")
    void testEmptyArgs() {
        QueryParser.QueryCommand cmd = parser.parse(
                "db.getCollection('users').find()");

        assertEquals(1, cmd.arguments.size());
        assertTrue(cmd.arguments.get(0).isEmpty());
    }

    @Test
    @DisplayName("空字符串参数 → 生成一个空 Document")
    void testEmptyStringArgs() {
        QueryParser.QueryCommand cmd = parser.parse(
                "db.getCollection('users').find( )");

        assertEquals(1, cmd.arguments.size());
        assertTrue(cmd.arguments.get(0).isEmpty());
    }

    @Test
    @DisplayName("双参数 find(filter, projection)")
    void testTwoArgsFindProjection() {
        QueryParser.QueryCommand cmd = parser.parse(
                "db.getCollection('users').find({'name':'z'},{'name':1,'_id':0})");

        assertEquals(2, cmd.arguments.size());
        assertEquals("z", cmd.arguments.get(0).getString("name"));
        assertEquals(1, cmd.arguments.get(1).getInteger("name"));
        assertEquals(0, cmd.arguments.get(1).getInteger("_id"));
    }

    @Test
    @DisplayName("三参数 updateOne(filter, update, options)")
    void testThreeArgsUpdateOne() {
        QueryParser.QueryCommand cmd = parser.parse(
                "db.getCollection('users').updateOne({'name':'zhang'},{$set:{'age':30}},{upsert:true})");

        assertEquals(3, cmd.arguments.size());
        assertEquals("zhang", cmd.arguments.get(0).getString("name"));

        Document update = cmd.arguments.get(1);
        assertNotNull(update.get("$set"));
        Document setDoc = (Document) update.get("$set");
        assertEquals(30, setDoc.getInteger("age"));

        assertTrue(cmd.arguments.get(2).getBoolean("upsert"));
    }

    @Test
    @DisplayName("嵌套花括号 — updateMany with nested $set")
    void testNestedBraces() {
        QueryParser.QueryCommand cmd = parser.parse(
                "db.getCollection('users').updateMany({'a':1},{$set:{'nested':{'x':1,'y':2}}})");

        assertEquals(2, cmd.arguments.size());
        Document update = cmd.arguments.get(1);
        Document setDoc = (Document) update.get("$set");
        Document nested = (Document) setDoc.get("nested");
        assertEquals(1, nested.getInteger("x"));
        assertEquals(2, nested.getInteger("y"));
    }

    @Test
    @DisplayName("参数间有空格")
    void testWhitespaceBetweenArgs() {
        QueryParser.QueryCommand cmd = parser.parse(
                "db.getCollection('users').find( {'a':1} , {'b':1} )");

        assertEquals(2, cmd.arguments.size());
        assertEquals(1, cmd.arguments.get(0).getInteger("a"));
        assertEquals(1, cmd.arguments.get(1).getInteger("b"));
    }

    @Test
    @DisplayName("引号内包含逗号 — 不分隔")
    void testCommaInsideQuotes() {
        QueryParser.QueryCommand cmd = parser.parse(
                "db.getCollection('users').find({'name':'hello,world'})");

        assertEquals(1, cmd.arguments.size());
        assertEquals("hello,world", cmd.arguments.get(0).getString("name"));
    }

    @Test
    @DisplayName("双引号内包含逗号 — 不分隔")
    void testCommaInsideDoubleQuotes() {
        QueryParser.QueryCommand cmd = parser.parse(
                "db.getCollection('users').find({\"name\":\"hello,world\"})");

        assertEquals(1, cmd.arguments.size());
        assertEquals("hello,world", cmd.arguments.get(0).getString("name"));
    }

    @Test
    @DisplayName("花括号在引号内 — 不影响深度")
    void testBraceInQuotes() {
        QueryParser.QueryCommand cmd = parser.parse(
                "db.getCollection('users').find({'name':'{unbalanced'})");

        assertEquals(1, cmd.arguments.size());
        assertEquals("{unbalanced", cmd.arguments.get(0).getString("name"));
    }

    @Test
    @DisplayName("findOne 操作")
    void testFindOne() {
        QueryParser.QueryCommand cmd = parser.parse(
                "db.getCollection('users').findOne({'name':'Alice'})");

        assertEquals("findOne", cmd.operation);
        assertEquals(1, cmd.arguments.size());
    }

    @Test
    @DisplayName("count 操作")
    void testCount() {
        QueryParser.QueryCommand cmd = parser.parse(
                "db.getCollection('users').count({'status':'active'})");

        assertEquals("count", cmd.operation);
        assertEquals(1, cmd.arguments.size());
        assertEquals("active", cmd.arguments.get(0).getString("status"));
    }

    @Test
    @DisplayName("deleteOne 操作")
    void testDeleteOne() {
        QueryParser.QueryCommand cmd = parser.parse(
                "db.getCollection('users').deleteOne({'_id':'123'})");

        assertEquals("deleteOne", cmd.operation);
        assertEquals(1, cmd.arguments.size());
    }

    @Test
    @DisplayName("insertOne 操作")
    void testInsertOne() {
        QueryParser.QueryCommand cmd = parser.parse(
                "db.getCollection('users').insertOne({'name':'Bob','age':25})");

        assertEquals("insertOne", cmd.operation);
        assertEquals(1, cmd.arguments.size());
        assertEquals("Bob", cmd.arguments.get(0).getString("name"));
        assertEquals(25, cmd.arguments.get(0).getInteger("age"));
    }

    @Test
    @DisplayName("非法命令格式 — 抛异常")
    void testInvalidCommand() {
        assertThrows(IllegalArgumentException.class, () ->
                parser.parse("garbage"));
        assertThrows(IllegalArgumentException.class, () ->
                parser.parse("db.collection.find()"));
    }

    // ======================== splitTopLevelArguments() tests ========================

    @Test
    @DisplayName("split: 空字符串 → 空列表")
    void testSplitEmpty() {
        List<String> result = QueryParser.splitTopLevelArguments("");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("split: null → 空列表")
    void testSplitNull() {
        List<String> result = QueryParser.splitTopLevelArguments(null);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("split: 单个文档")
    void testSplitSingleDoc() {
        List<String> result = QueryParser.splitTopLevelArguments("{a:1}");
        assertEquals(1, result.size());
        assertEquals("{a:1}", result.get(0));
    }

    @Test
    @DisplayName("split: 两个简单文档")
    void testSplitTwoDocs() {
        List<String> result = QueryParser.splitTopLevelArguments("{a:1},{b:2}");
        assertEquals(2, result.size());
        assertEquals("{a:1}", result.get(0));
        assertEquals("{b:2}", result.get(1));
    }

    @Test
    @DisplayName("split: 三个文档，含嵌套花括号")
    void testSplitThreeDocsWithNesting() {
        List<String> result = QueryParser.splitTopLevelArguments(
                "{a:1}, {$set: {b: 2}}, {upsert: true}");
        assertEquals(3, result.size());
        assertEquals("{a:1}", result.get(0).trim());
        assertEquals("{$set: {b: 2}}", result.get(1).trim());
        assertEquals("{upsert: true}", result.get(2).trim());
    }

    @Test
    @DisplayName("split: 数组参数")
    void testSplitArrayArg() {
        List<String> result = QueryParser.splitTopLevelArguments(
                "[{$match:{x:1}},{$project:{y:1}}]");
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("split: 引号内含逗号不分隔")
    void testSplitCommaInQuotes() {
        List<String> result = QueryParser.splitTopLevelArguments(
                "{name:'a,b'}");
        assertEquals(1, result.size());
        assertEquals("{name:'a,b'}", result.get(0));
    }

    @Test
    @DisplayName("split: 混合单双引号")
    void testSplitMixedQuotes() {
        List<String> result = QueryParser.splitTopLevelArguments(
                "{a:'single',b:\"double\"},{c:1}");
        assertEquals(2, result.size());
    }
}
