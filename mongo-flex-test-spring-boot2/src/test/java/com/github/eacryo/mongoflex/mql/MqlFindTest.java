package com.github.eacryo.mongoflex.mql;

import com.github.eacryo.mongoflex.TestApplication;
import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.v2.CharacterRepository;
import com.github.eacryo.mongoflex.ulid.Ulid;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("v2")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MqlFindTest {

    @Autowired
    private CharacterRepository repo;

    private final List<String> ids = new java.util.ArrayList<>();

    @BeforeAll
    void insertData() {
        // Insert 12 documents for skip/limit pagination tests / 插入12条数据用于分页测试
        for (int i = 0; i < 12; i++) {
            String id = Ulid.generate();
            Character c = new Character();
            c.setId(id);
            c.setName("MqlFindTest-" + i + "-" + id);
            c.setAddress("Natlan");
            c.setBirthday(new Date());
            repo.insert(c);
            ids.add(id);
        }
        log.info("inserted {} documents for pagination tests", ids.size());
    }

    @AfterAll
    void cleanup() {
        for (String id : ids) {
            Character del = new Character();
            del.setId(id);
            del.setDeleted(true);
            repo.updateOneById(del);
        }
        log.info("cleaned up {} documents", ids.size());
    }

    @Test
    @Order(1)
    void testFindAll() {
        log.info("=== test @Find findAll ===");
        List<Character> result = repo.findAll();
        log.info("findAll size: {}", result.size());
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isEmpty());
    }

    @Test
    @Order(2)
    void testFindAllObj() {
        log.info("=== test @Find findAllObj ===");
        List<Object> result = repo.findAllObj();
        log.info("findAllObj size: {}", result.size());
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isEmpty());
        // 验证返回的是 Map（Document 已转为普通 Map），且包含文档字段数据
        Object first = result.get(0);
        Assertions.assertTrue(first instanceof Map, "element should be Map, got: " + first.getClass());
        Map<String, Object> map = (Map<String, Object>) first;
        Assertions.assertTrue(map.containsKey("_id"), "Map should contain _id");
        Assertions.assertTrue(map.containsKey("name"), "Map should contain name");
        Assertions.assertNotNull(map.get("_id"), "_id should not be null");
    }

    @Test
    @Order(3)
    void testFindByCriteria() {
        log.info("=== test @Find findListByCriteria ===");
        String firstName = "MqlFindTest-0-" + ids.get(0);
        List<Character> result = repo.findListByCriteria(firstName);
        log.info("findListByCriteria size: {}", result.size());
        Assertions.assertFalse(result.isEmpty());
        Assertions.assertEquals(firstName, result.get(0).getName());
    }

    @Test
    @Order(4)
    void testFindOneByName() {
        log.info("=== test @Find findOneByName ===");
        String firstName = "MqlFindTest-0-" + ids.get(0);
        Character result = repo.findOneByName(firstName);
        log.info("findOneByName: {}", result);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(firstName, result.getName());
    }

    @Test
    @Order(5)
    void testFindOneNoMatch() {
        log.info("=== test @Find findOne (no match) ===");
        Character result = repo.findOneByName("NonExistent" + System.currentTimeMillis());
        log.info("findOne no match: {}", result);
        Assertions.assertNull(result);
    }

    // FIXME: hardcoded data dependency — relies on pre-existing 'Hu Tao' document
    // @Test
    // @Order(6)
    // void testFindListWithoutParam() {
    //     log.info("=== test @Find findListWithoutParam ===");
    //     List<Character> result = repo.findListWithoutParam();
    //     log.info("findListWithoutParam size: {}", result.size());
    //     Assertions.assertNotNull(result);
    // }

    @Test
    @Order(7)
    void testRawListReturnType() {
        log.info("=== test raw List return type ===");
        List result = repo.findListRaw();
        log.info("raw List result size: {}, element: {}", result.size(), result.isEmpty() ? "empty" : result.get(0).getClass());
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isEmpty());
        // 验证原始 List 的元素也是 Map（不是空 Object）
        Object first = result.get(0);
        Assertions.assertTrue(first instanceof Map, "raw List element should be Map, got: " + first.getClass());
        Map<String, Object> map = (Map<String, Object>) first;
        Assertions.assertTrue(map.containsKey("name"), "Map should contain name");
        Assertions.assertNotNull(map.get("name"), "name should not be null");
    }

    @Test
    @Order(8)
    void testFindListWithLimit() {
        log.info("=== test @Find find with limit ===");
        List<Character> result = repo.findListWithLimit();
        log.info("findListWithLimit size: {}", result.size());
        Assertions.assertNotNull(result);
        Assertions.assertEquals(10, result.size(), "limit(10) should return exactly 10 documents");
    }

    @Test
    @Order(9)
    void testFindListWithSkipAndLimit() {
        log.info("=== test @Find find with skip and limit ===");
        // 先获取全部数据的前10条作为参照
        List<Character> all = repo.findAll();
        log.info("total documents: {}", all.size());
        Assertions.assertTrue(all.size() >= 10, "need at least 10 documents for pagination test");

        List<Character> firstPage = repo.findListWithLimit();
        List<Character> secondPage = repo.findListWithSkipAndLimit();

        log.info("firstPage (limit 10) size: {}", firstPage.size());
        log.info("secondPage (skip 5, limit 5) size: {}", secondPage.size());

        Assertions.assertEquals(10, firstPage.size());
        Assertions.assertEquals(5, secondPage.size());

        // 第二页的第1条应该等于全部数据的第6条（skip 5）
        Assertions.assertEquals(all.get(5).getId(), secondPage.get(0).getId(),
                "second page first element should equal all.get(5)");
    }

}

