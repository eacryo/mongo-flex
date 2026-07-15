package com.github.eacryo.mongoflex.lambda;

import com.github.eacryo.mongoflex.TestApplication;
import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.query.LambdaQueryWrapper;
import com.github.eacryo.mongoflex.v2.CharacterRepository;
import com.github.f4b6a3.ulid.UlidCreator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;
import java.util.List;

@Slf4j
@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("v2")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LambdaQueryBasicTest {

    @Autowired
    private CharacterRepository repo;

    private static final String TEST_NAME = "LambdaBasic";
    private String id1, id2;

    @BeforeEach
    void setUp() {
        id1 = UlidCreator.getUlid().toString();
        Character c1 = new Character();
        c1.setId(id1);
        c1.setName(TEST_NAME + "-A-" + id1);
        c1.setAddress("Mondstadt");
        c1.setBirthday(new Date());
        repo.insert(c1);
        log.info("inserted: id={}, name={}", id1, c1.getName());

        id2 = UlidCreator.getUlid().toString();
        Character c2 = new Character();
        c2.setId(id2);
        c2.setName(TEST_NAME + "-B-" + id2);
        c2.setAddress("Liyue");
        c2.setBirthday(new Date());
        repo.insert(c2);
        log.info("inserted: id={}, name={}", id2, c2.getName());
    }

    @AfterEach
    void tearDown() {
        repo.deleteOneById(id1);
        repo.deleteOneById(id2);
        log.info("cleaned up: id1={}, id2={}", id1, id2);
    }

    @Test
    @Order(1)
    void testEqSingle() {
        log.info("=== test eq (single match) ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.eq(Character::getName, TEST_NAME + "-A-" + id1);
        List<Character> result = repo.findList(wrapper);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(id1, result.get(0).getId());
        log.info("result: {}", result.get(0));
    }

    @Test
    @Order(2)
    void testEqNoMatch() {
        log.info("=== test eq (no match) ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.eq(Character::getName, "NonExistentName");
        List<Character> result = repo.findList(wrapper);
        Assertions.assertTrue(result.isEmpty());
        log.info("result: empty (expected)");
    }

    @Test
    @Order(3)
    void testNe() {
        log.info("=== test ne ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.eq(Character::getAddress, "Mondstadt")
               .ne(Character::getName, TEST_NAME + "-A-" + id1);
        List<Character> result = repo.findList(wrapper);
        log.info("ne result size: {}", result.size());
        for (Character c : result) {
            Assertions.assertEquals("Mondstadt", c.getAddress());
            Assertions.assertNotEquals(TEST_NAME + "-A-" + id1, c.getName());
        }
    }

    @Test
    @Order(4)
    void testGtDate() {
        log.info("=== test gt (date) ===");
        Date past = new Date(System.currentTimeMillis() - 3600_000);
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.gt(Character::getBirthday, past)
               .eq(Character::getAddress, "Mondstadt");
        List<Character> result = repo.findList(wrapper);
        log.info("gt date result size: {}", result.size());
        Assertions.assertFalse(result.isEmpty());
        for (Character c : result) {
            Assertions.assertTrue(c.getBirthday().after(past));
        }
    }

    @Test
    @Order(5)
    void testLtDate() {
        log.info("=== test lt (date) ===");
        Date future = new Date(System.currentTimeMillis() + 3600_000);
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.lt(Character::getBirthday, future);
        List<Character> result = repo.findList(wrapper);
        log.info("lt date result size: {}", result.size());
        Assertions.assertFalse(result.isEmpty());
        for (Character c : result) {
            Assertions.assertTrue(c.getBirthday().before(future));
        }
    }

    @Test
    @Order(6)
    void testRangeQuery() {
        log.info("=== test range query (gte + lte) ===");
        Date past = new Date(System.currentTimeMillis() - 3600_000);
        Date future = new Date(System.currentTimeMillis() + 3600_000);
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.gte(Character::getBirthday, past)
               .lte(Character::getBirthday, future);
        List<Character> result = repo.findList(wrapper);
        log.info("range query result size: {}", result.size());
        Assertions.assertFalse(result.isEmpty());
        for (Character c : result) {
            Assertions.assertTrue(c.getBirthday() != null);
        }
    }

    @Test
    @Order(7)
    void testMultipleEq() {
        log.info("=== test multiple eq (AND) ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.eq(Character::getAddress, "Mondstadt")
               .eq(Character::getName, TEST_NAME + "-A-" + id1);
        List<Character> result = repo.findList(wrapper);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(id1, result.get(0).getId());
        log.info("result: {}", result.get(0));
    }

}

