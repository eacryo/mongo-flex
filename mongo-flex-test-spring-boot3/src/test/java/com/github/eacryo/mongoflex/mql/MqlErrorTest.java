package com.github.eacryo.mongoflex.mql;

import com.github.eacryo.mongoflex.TestApplication;
import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.v2.CharacterRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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
    void testFindByNonExistingName() {
        log.info("=== test @Find with non-existing param ===");
        List<Character> result = repo.findListByCriteria("NonExistent" + System.currentTimeMillis());
        log.info("result size: {}", result.size());
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    @Order(2)
    void testMalformedJsonTemplate() {
        log.info("=== test malformed JSON template ===");
        assertThrows(RuntimeException.class, () ->
                errorRepo.malformedCommand());
        log.info("correctly threw RuntimeException");
    }

    @Test
    @Order(3)
    void testNullParam() {
        log.info("=== test null param handled correctly ===");
        List<Character> result = errorRepo.withNullParam(null);
        log.info("result size: {}", result.size());
        Assertions.assertTrue(result.isEmpty());
    }

}
