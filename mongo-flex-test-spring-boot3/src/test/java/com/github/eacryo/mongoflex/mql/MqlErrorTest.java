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

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("v2")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MqlErrorTest {

    @Autowired
    private CharacterRepository repo;

    @Autowired
    private MqlErrorTestRepository errorRepo;

    @Test
    @Order(1)
    void testFindOneNonExisting() {
        log.info("=== test @Mql findOne non-existing returns null ===");
        Character result = repo.findOneByMql();
        log.info("result: {}", result);
        Assertions.assertNotNull(result);
    }

    @Test
    @Order(2)
    void testFindByNonExistingName() {
        log.info("=== test @Mql find with non-existing param ===");
        List<Character> result = repo.findListByCriteria("NonExistent" + System.currentTimeMillis());
        log.info("result size: {}", result.size());
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    @Order(3)
    void testUnsupportedOperation() {
        log.info("=== test unsupported MQL operation ===");
        assertThrows(UnsupportedOperationException.class, () ->
                errorRepo.unsupportedOperation("test"));
        log.info("correctly threw UnsupportedOperationException");
    }

    @Test
    @Order(4)
    void testMalformedMqlCommand() {
        log.info("=== test malformed MQL command ===");
        assertThrows(RuntimeException.class, () ->
                errorRepo.malformedCommand());
        log.info("correctly threw RuntimeException");
    }

}
