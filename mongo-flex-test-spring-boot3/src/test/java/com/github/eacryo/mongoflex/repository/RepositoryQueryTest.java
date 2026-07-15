package com.github.eacryo.mongoflex.repository;

import com.github.eacryo.mongoflex.TestApplication;
import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.query.LambdaQueryWrapper;
import com.github.eacryo.mongoflex.v2.CharacterRepository;
import com.github.eacryo.mongoflex.ulid.Ulid;
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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RepositoryQueryTest {

    @Autowired
    private CharacterRepository repo;

    private String id1, id2;

    @BeforeAll
    void insertData() {
        id1 = Ulid.generate();
        Character c1 = new Character();
        c1.setId(id1);
        c1.setName("RepoQuery-A-" + id1);
        c1.setArea("Sumeru");
        c1.setAddress("Port Ormos");
        c1.setBirthday(new Date());
        repo.insert(c1);
        log.info("inserted: id={}, name={}", id1, c1.getName());

        id2 = Ulid.generate();
        Character c2 = new Character();
        c2.setId(id2);
        c2.setName("RepoQuery-B-" + id2);
        c2.setArea("Sumeru");
        c2.setAddress("Gandharva Ville");
        c2.setBirthday(new Date());
        repo.insert(c2);
        log.info("inserted: id={}, name={}", id2, c2.getName());
    }

    @AfterAll
    void cleanup() {
        repo.deleteOneById(id1);
        repo.deleteOneById(id2);
        log.info("cleaned up");
    }

    @Test
    @Order(1)
    void testFindOneByField() {
        log.info("=== test findOne(field, value) ===");
        Character found = repo.findOne(Character::getName, "RepoQuery-A-" + id1);
        log.info("result: {}", found);
        Assertions.assertNotNull(found);
        Assertions.assertEquals(id1, found.getId());
    }

    @Test
    @Order(2)
    void testFindListByWrapper() {
        log.info("=== test findList(wrapper) ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.eq(Character::getArea, "Sumeru");
        List<Character> list = repo.findList(wrapper);
        log.info("findList result size: {}", list.size());
        Assertions.assertTrue(list.size() >= 2);
        for (Character c : list) {
            Assertions.assertEquals("Sumeru", c.getArea());
        }
    }

    @Test
    @Order(3)
    void testCountByEntity() {
        log.info("=== test count(entity) ===");
        Character query = new Character();
        query.setArea("Sumeru");
        long count = repo.countByEntity(query);
        log.info("countByEntity(entity) result: {}", count);
        Assertions.assertTrue(count >= 2);
    }

    @Test
    @Order(4)
    void testCountByField() {
        log.info("=== test count(field, value) ===");
        long count = repo.count(Character::getArea, "Sumeru");
        log.info("count(field,value) result: {}", count);
        Assertions.assertTrue(count >= 2);
    }

    @Test
    @Order(5)
    void testCountByWrapper() {
        log.info("=== test count(wrapper) ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.eq(Character::getArea, "Sumeru");
        long count = repo.count(wrapper);
        log.info("count(wrapper) result: {}", count);
        Assertions.assertTrue(count >= 2);
    }

    @Test
    @Order(6)
    void testFindOneByEntityEmpty() {
        log.info("=== test findOneByEntity (empty entity) ===");
        Character query = new Character();
        Character found = repo.findOneByEntity(query);
        log.info("findOneByEntity(empty) result: {}", found);
        Assertions.assertNotNull(found);
    }

    @Test
    @Order(7)
    void testCountByEntityEmpty() {
        log.info("=== test countByEntity(empty entity) ===");
        Character query = new Character();
        long count = repo.countByEntity(query);
        long total = repo.count();
        log.info("count(empty)={}, total={}", count, total);
        Assertions.assertEquals(total, count);
    }

}

