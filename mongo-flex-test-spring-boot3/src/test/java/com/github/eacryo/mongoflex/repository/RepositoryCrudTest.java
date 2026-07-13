package com.github.eacryo.mongoflex.repository;

import com.github.eacryo.mongoflex.TestApplication;
import com.github.eacryo.mongoflex.bean.Character;
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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RepositoryCrudTest {

    @Autowired
    private CharacterRepository repo;

    @Test
    @Order(1)
    void testInsertAndFindById() {
        log.info("=== test insert + findById ===");
        String id = UlidCreator.getUlid().toString();
        try {
            Character c = new Character();
            c.setId(id);
            c.setName("CrudTest-" + id);
            c.setAddress("Fontaine");
            c.setBirthday(new Date());
            Character inserted = repo.insert(c);
            log.info("inserted: {}", inserted);
            Assertions.assertNotNull(inserted);

            Character found = repo.findById(id);
            log.info("found by id: {}", found);
            Assertions.assertNotNull(found);
            Assertions.assertEquals(id, found.getId());
            Assertions.assertEquals("CrudTest-" + id, found.getName());
            Assertions.assertEquals("Fontaine", found.getAddress());
        } finally {
            repo.deleteById(id);
            log.info("cleaned up: {}", id);
        }
    }

    @Test
    @Order(2)
    void testUpdateById() {
        log.info("=== test updateById ===");
        String id = UlidCreator.getUlid().toString();
        try {
            Character c = new Character();
            c.setId(id);
            c.setName("CrudUpdateTest-" + id);
            c.setAddress("Before");
            c.setBirthday(new Date());
            repo.insert(c);
            log.info("inserted: {}", c);

            c.setAddress("After");
            long updated = repo.updateOneById(c);
            log.info("updateById result: {}", updated);
            Assertions.assertEquals(1, updated);

            Character found = repo.findById(id);
            log.info("after update: {}", found);
            Assertions.assertEquals("After", found.getAddress());
        } finally {
            repo.deleteById(id);
        }
    }

    @Test
    @Order(3)
    void testDeleteById() {
        log.info("=== test deleteById ===");
        String id = UlidCreator.getUlid().toString();
        Character c = new Character();
        c.setId(id);
        c.setName("CrudDeleteTest-" + id);
        c.setBirthday(new Date());
        repo.insert(c);
        log.info("inserted: {}", c);

        Character beforeDelete = repo.findById(id);
        Assertions.assertNotNull(beforeDelete);

        long deleted = repo.deleteById(id);
        log.info("deleteById result: {}", deleted);
        Assertions.assertEquals(1, deleted);

        Character afterDelete = repo.findById(id);
        log.info("after delete: {}", afterDelete);
        Assertions.assertNull(afterDelete);
    }

    @Test
    @Order(4)
    void testFindOneByEntity() throws InterruptedException {
        log.info("=== test findOneByEntity ===");
        String id = UlidCreator.getUlid().toString();
        try {
            String name = "CrudFindOneEntity-" + id;
            Character c = new Character();
            c.setId(id);
            c.setName(name);
            c.setAddress("Snezhnaya");
            c.setBirthday(new Date());
            repo.insert(c);
            Thread.sleep(1000);

            Character query = new Character();
            query.setName(name);
            Character found = repo.findOneByEntity(query);
            log.info("findOneByEntity result: {}", found);
            Assertions.assertNotNull(found);
            Assertions.assertEquals(name, found.getName());
        } finally {
            repo.deleteById(id);
        }
    }

    @Test
    @Order(5)
    void testCount() {
        log.info("=== test count ===");
        long total = repo.count();
        log.info("total count: {}", total);
        Assertions.assertTrue(total >= 0);
    }

    @Test
    @Order(6)
    void testDeleteByEntity() {
        log.info("=== test deleteByEntity ===");
        String id = UlidCreator.getUlid().toString();
        String name = "CrudDeleteByEntity-" + id;
        Character c = new Character();
        c.setId(id);
        c.setName(name);
        c.setBirthday(new Date());
        repo.insert(c);
        log.info("inserted: {}", c);

        Character query = new Character();
        query.setName(name);
        long deleted = repo.deleteByEntity(query);
        log.info("deleteByEntity result: {}", deleted);
        Assertions.assertEquals(1, deleted);

        Character afterDelete = repo.findById(id);
        Assertions.assertNull(afterDelete);
    }

    @Test
    @Order(7)
    void testFindAll() {
        log.info("=== test findAll ===");
        String id = UlidCreator.getUlid().toString();
        try {
            Character c = new Character();
            c.setId(id);
            c.setName("CrudFindAll-" + id);
            c.setBirthday(new Date());
            repo.insert(c);

            List<Character> all = repo.findAll();
            log.info("findAll size: {}", all.size());
            Assertions.assertNotNull(all);
            Assertions.assertFalse(all.isEmpty());
        } finally {
            repo.deleteById(id);
        }
    }

    @Test
    @Order(8)
    void testDeleteAll() {
        log.info("=== test deleteAll ===");
        String id = UlidCreator.getUlid().toString();
        Character c = new Character();
        c.setId(id);
        c.setName("CrudDeleteAll-" + id);
        c.setBirthday(new Date());
        repo.insert(c);

        long deleted = repo.deleteAll();
        log.info("deleteAll result: {}", deleted);
        Assertions.assertTrue(deleted >= 1);

        long after = repo.count();
        log.info("count after deleteAll: {}", after);
        Assertions.assertEquals(0, after);
    }

}

