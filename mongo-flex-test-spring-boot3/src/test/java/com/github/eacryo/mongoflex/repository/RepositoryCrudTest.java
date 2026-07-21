package com.github.eacryo.mongoflex.repository;

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
        String id = Ulid.generate();
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
            Character del = new Character();
            del.setId(id);
            del.setDeleted(true);
            repo.updateOneById(del);
            log.info("cleaned up: {}", id);
        }
    }

    @Test
    @Order(2)
    void testUpdateById() {
        log.info("=== test updateById ===");
        String id = Ulid.generate();
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
            Character del = new Character();
            del.setId(id);
            del.setDeleted(true);
            repo.updateOneById(del);
        }
    }

    @Test
    @Order(3)
    void testDeleteById() {
        log.info("=== test deleteById ===");
        String id = Ulid.generate();
        Character c = new Character();
        c.setId(id);
        c.setName("CrudDeleteTest-" + id);
        c.setBirthday(new Date());
        repo.insert(c);
        log.info("inserted: {}", c);

        Character beforeDelete = repo.findById(id);
        Assertions.assertNotNull(beforeDelete);

        long deleted = repo.deleteOneById(id);
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
        String id = Ulid.generate();
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
            Character del = new Character();
            del.setId(id);
            del.setDeleted(true);
            repo.updateOneById(del);
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
        String id = Ulid.generate();
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
        String id = Ulid.generate();
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
            Character del = new Character();
            del.setId(id);
            del.setDeleted(true);
            repo.updateOneById(del);
        }
    }

    @Test
    @Order(8)
    void testDeleteAll() {
        log.info("=== test deleteAll (logical via updateMany) ===");
        String id = Ulid.generate();
        String name = "CrudDeleteAll-" + id;
        Character c = new Character();
        c.setId(id);
        c.setName(name);
        c.setBirthday(new Date());
        repo.insert(c);

        Character del = new Character();
        del.setDeleted(true);
        long updated = repo.updateMany(Character::getName, name, del);
        log.info("updateMany (logical delete) result: {}", updated);
        Assertions.assertEquals(1, updated);

        Character afterDelete = repo.findById(id);
        Assertions.assertNull(afterDelete);
    }

}

