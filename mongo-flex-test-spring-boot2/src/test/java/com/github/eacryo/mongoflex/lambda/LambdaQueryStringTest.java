package com.github.eacryo.mongoflex.lambda;

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
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LambdaQueryStringTest {

    @Autowired
    private CharacterRepository repo;

    private String id1;

    @BeforeEach
    void setUp() {
        id1 = Ulid.generate();
        Character c = new Character();
        c.setId(id1);
        c.setName("StringTest-A-" + id1);
        c.setArea("Natlan");
        c.setBirthday(new Date());
        repo.insert(c);
        log.info("inserted: id={}, name={}, area={}", id1, c.getName(), c.getArea());
    }

    @AfterEach
    void tearDown() {
        Character mark = new Character();
        mark.setDeleted(true);
        mark.setId(id1);
        repo.updateOneById(mark);
        log.info("cleaned up");
    }

    @Test
    @Order(1)
    void testRegex() {
        log.info("=== test regex ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.regex(Character::getName, "^StringTest-");
        List<Character> result = repo.findList(wrapper);
        log.info("regex result size: {}", result.size());
        Assertions.assertFalse(result.isEmpty());
        for (Character c : result) {
            Assertions.assertTrue(c.getName().startsWith("StringTest-"));
        }
    }

    @Test
    @Order(2)
    void testRegexNoMatch() {
        log.info("=== test regex (no match) ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.regex(Character::getName, "^NonExistent");
        List<Character> result = repo.findList(wrapper);
        log.info("regex no match size: {}", result.size());
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    @Order(3)
    void testCombinedRegexAndEq() {
        log.info("=== test combined regex + eq ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.regex(Character::getName, "^StringTest-")
               .eq(Character::getArea, "Natlan");
        List<Character> result = repo.findList(wrapper);
        log.info("combined result size: {}", result.size());
        Assertions.assertFalse(result.isEmpty());
        for (Character c : result) {
            Assertions.assertTrue(c.getName().startsWith("StringTest-"));
            Assertions.assertEquals("Natlan", c.getArea());
        }
    }

    @Test
    @Order(4)
    void testCollectionFieldMapping() {
        log.info("=== test @CollectionField mapping (area -> c_area) ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.eq(Character::getArea, "Natlan");
        List<Character> result = repo.findList(wrapper);
        log.info("@CollectionField result size: {}", result.size());
        Assertions.assertFalse(result.isEmpty());
        Assertions.assertEquals("Natlan", result.get(0).getArea());
        log.info("verified: Java field 'area' mapped to MongoDB field 'c_area'");
    }

    @Test
    @Order(5)
    void testIdToUnderscoreIdMapping() {
        log.info("=== test id -> _id mapping ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.eq(Character::getId, id1);
        List<Character> result = repo.findList(wrapper);
        log.info("id -> _id result size: {}", result.size());
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(id1, result.get(0).getId());
        log.info("verified: Java field 'id' mapped to MongoDB '_id'");
    }

}

