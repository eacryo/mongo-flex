package com.github.eacryo.mongoflex.mql;

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
public class MqlFindTest {

    @Autowired
    private CharacterRepository repo;

    private String id;

    @BeforeAll
    void insertData() {
        id = UlidCreator.getUlid().toString();
        Character c = new Character();
        c.setId(id);
        c.setName("MqlFindTest-" + id);
        c.setAddress("Natlan");
        c.setBirthday(new Date());
        repo.insert(c);
        log.info("inserted: id={}, name={}", id, c.getName());
    }

    @AfterAll
    void cleanup() {
        repo.deleteById(id);
        log.info("cleaned up");
    }

    @Test
    @Order(1)
    void testFindAll() {
        log.info("=== test @Mql findAll ===");
        List<Character> result = repo.findAll();
        log.info("findAll size: {}", result.size());
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isEmpty());
    }

    @Test
    @Order(2)
    void testFindAllObj() {
        log.info("=== test @Mql findAllObj ===");
        List<Object> result = repo.findAllObj();
        log.info("findAllObj size: {}", result.size());
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isEmpty());
    }

    @Test
    @Order(3)
    void testFindByCriteria() {
        log.info("=== test @Mql findListByCriteria ===");
        List<Character> result = repo.findListByCriteria("MqlFindTest-" + id);
        log.info("findListByCriteria size: {}", result.size());
        Assertions.assertFalse(result.isEmpty());
        Assertions.assertEquals("MqlFindTest-" + id, result.get(0).getName());
    }

    @Test
    @Order(4)
    void testFindOneByName() {
        log.info("=== test @Mql findOneByName ===");
        Character result = repo.findOneByName("MqlFindTest-" + id);
        log.info("findOneByName: {}", result);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("MqlFindTest-" + id, result.getName());
    }

    @Test
    @Order(5)
    void testFindOneNoMatch() {
        log.info("=== test @Mql findOne (no match) ===");
        Character result = repo.findOneByName("NonExistent" + System.currentTimeMillis());
        log.info("findOne no match: {}", result);
        Assertions.assertNull(result);
    }

    @Test
    @Order(6)
    void testFindListWithoutParam() {
        log.info("=== test @Mql findListWithoutParam ===");
        List<Character> result = repo.findListWithoutParam();
        log.info("findListWithoutParam size: {}", result.size());
        Assertions.assertNotNull(result);
    }

}

