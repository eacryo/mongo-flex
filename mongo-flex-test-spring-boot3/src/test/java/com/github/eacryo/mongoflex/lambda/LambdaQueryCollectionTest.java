package com.github.eacryo.mongoflex.lambda;

import com.github.eacryo.mongoflex.TestApplication;
import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.lambda.LambdaQueryWrapper;
import com.github.eacryo.mongoflex.v2.CharacterRepository;
import com.github.f4b6a3.ulid.UlidCreator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Slf4j
@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("v2")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LambdaQueryCollectionTest {

    @Autowired
    private CharacterRepository repo;

    private String id1, id2, id3;

    @BeforeEach
    void setUp() {
        id1 = UlidCreator.getUlid().toString();
        Character c1 = new Character();
        c1.setId(id1);
        c1.setName("CollectionTest-A-" + id1);
        c1.setAddress("Mondstadt");
        c1.setBirthday(new Date());
        repo.insert(c1);

        id2 = UlidCreator.getUlid().toString();
        Character c2 = new Character();
        c2.setId(id2);
        c2.setName("CollectionTest-B-" + id2);
        c2.setAddress("Liyue");
        c2.setBirthday(new Date());
        repo.insert(c2);

        id3 = UlidCreator.getUlid().toString();
        Character c3 = new Character();
        c3.setId(id3);
        c3.setName("CollectionTest-C-" + id3);
        c3.setAddress("Inazuma");
        c3.setEmail("test@example.com");
        c3.setBirthday(new Date());
        repo.insert(c3);

        log.info("inserted 3: {}, {}, {}", id1, id2, id3);
    }

    @AfterEach
    void tearDown() {
        repo.deleteOneById(id1);
        repo.deleteOneById(id2);
        repo.deleteOneById(id3);
        log.info("cleaned up");
    }

    @Test
    @Order(1)
    void testIn() {
        log.info("=== test in ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.in(Character::getAddress, Arrays.asList("Mondstadt", "Liyue"));
        List<Character> result = repo.findList(wrapper);
        log.info("in result size: {}", result.size());
        Assertions.assertTrue(result.size() >= 2);
        for (Character c : result) {
            Assertions.assertTrue(
                    "Mondstadt".equals(c.getAddress()) || "Liyue".equals(c.getAddress()));
        }
    }

    @Test
    @Order(2)
    void testInNoMatch() {
        log.info("=== test in (no match) ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.in(Character::getAddress, Arrays.asList("NonExistent1", "NonExistent2"));
        List<Character> result = repo.findList(wrapper);
        log.info("in no match result size: {}", result.size());
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    @Order(3)
    void testNin() {
        log.info("=== test nin ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.nin(Character::getAddress, Arrays.asList("Mondstadt"));
        List<Character> result = repo.findList(wrapper);
        log.info("nin result size: {}", result.size());
        for (Character c : result) {
            Assertions.assertNotEquals("Mondstadt", c.getAddress());
        }
    }

    @Test
    @Order(4)
    void testExists() {
        log.info("=== test exists ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.exists(Character::getEmail, true);
        List<Character> result = repo.findList(wrapper);
        log.info("exists=true result size: {}", result.size());
        Assertions.assertFalse(result.isEmpty());
        for (Character c : result) {
            Assertions.assertNotNull(c.getEmail());
        }
    }

    @Test
    @Order(5)
    void testExistsFalse() {
        log.info("=== test exists (false) ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.exists(Character::getEmail, false);
        List<Character> result = repo.findList(wrapper);
        log.info("exists=false result size: {}", result.size());
        for (Character c : result) {
            Assertions.assertNull(c.getEmail());
        }
    }

    @Test
    @Order(6)
    void testEmptyWrapperRead() {
        log.info("=== test empty wrapper (read should not throw) ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        List<Character> result = repo.findList(wrapper);
        log.info("empty wrapper findList size: {}", result.size());
        Assertions.assertNotNull(result);
    }

}

