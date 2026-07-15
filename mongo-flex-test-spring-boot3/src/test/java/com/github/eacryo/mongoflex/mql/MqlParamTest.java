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

@Slf4j
@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("v2")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MqlParamTest {

    @Autowired
    private CharacterRepository repo;

    private String id;

    @BeforeAll
    void insertData() {
        id = Ulid.generate();
        Character c = new Character();
        c.setId(id);
        c.setName("MqlParam-" + id);
        c.setAddress("Inazuma");
        c.setBirthday(new Date());
        repo.insert(c);
        log.info("inserted: id={}, name={}", id, c.getName());
    }

    @AfterAll
    void cleanup() {
        repo.deleteOneById(id);
    }

    @Test
    @Order(1)
    void testSingleParam() {
        log.info("=== test @Param single ===");
        List<Character> result = repo.findListByCriteria("MqlParam-" + id);
        log.info("single param result size: {}", result.size());
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("MqlParam-" + id, result.get(0).getName());
    }

    @Test
    @Order(2)
    void testMultipleParams() {
        log.info("=== test @Param multiple ===");
        List<Character> result = repo.findListByNameAndId("MqlParam-" + id, id);
        log.info("multi param result size: {}", result.size());
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(id, result.get(0).getId());
    }

    // FIXME: hardcoded data dependency — relies on pre-existing 'Hu Tao' document
    // @Test
    // @Order(3)
    // void testNoParamQuery() {
    //     log.info("=== test @Find without @Param ===");
    //     List<Character> result = repo.findListWithoutParam();
    //     log.info("no param result size: {}", result.size());
    //     Assertions.assertNotNull(result);
    // }

    @Test
    @Order(4)
    void testCountWithParam() {
        log.info("=== test @Find count with @Param ===");
        long count = repo.countByCriteria("MqlParam-" + id);
        log.info("count with param: {}", count);
        Assertions.assertEquals(1, count);
    }

    @Test
    @Order(5)
    void testFindOneWithParam() {
        log.info("=== test @Find findOne with @Param ===");
        Character result = repo.findOneByName("MqlParam-" + id);
        log.info("findOne with param: {}", result);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("MqlParam-" + id, result.getName());
    }

}

