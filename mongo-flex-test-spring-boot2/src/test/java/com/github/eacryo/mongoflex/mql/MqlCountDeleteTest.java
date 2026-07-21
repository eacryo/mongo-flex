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

@Slf4j
@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("v2")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MqlCountDeleteTest {

    @Autowired
    private CharacterRepository repo;

    private String id;

    @Test
    @Order(1)
    void testCountByMql() {
        log.info("=== test @Find count ===");
        long total = repo.countByMql();
        log.info("countByMql: {}", total);
        Assertions.assertTrue(total >= 0);
    }

    @Test
    @Order(2)
    void testCountByCriteria() {
        log.info("=== test @Find count with criteria ===");
        id = Ulid.generate();
        String name = "MqlCount-" + id;
        Character c = new Character();
        c.setId(id);
        c.setName(name);
        c.setBirthday(new Date());
        repo.insert(c);
        log.info("inserted: {}", c);

        long count = repo.countByCriteria(name);
        log.info("countByCriteria: {}", count);
        Assertions.assertEquals(1, count);
    }

    @Test
    @Order(3)
    void testCountByRepoMql() {
        log.info("=== test repo count (from @Find) ===");
        long total = repo.countByMql();
        long repoCount = repo.count();
        log.info("countByMql={}, count()={}", total, repoCount);
        Assertions.assertEquals(total, repoCount);
    }

    @Test
    @Order(4)
    void testDeleteByIdAfterMqlCount() {
        log.info("=== test logical delete after @Count ===");
        Assertions.assertNotNull(id);
        Character del = new Character();
        del.setId(id);
        del.setDeleted(true);
        long deleted = repo.updateOneById(del);
        log.info("logical delete result: {}", deleted);
        Assertions.assertEquals(1, deleted);

        Character afterDelete = repo.findById(id);
        Assertions.assertNull(afterDelete);
    }

}

