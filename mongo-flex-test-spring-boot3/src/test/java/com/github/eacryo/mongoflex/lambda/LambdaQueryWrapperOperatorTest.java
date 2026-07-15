package com.github.eacryo.mongoflex.lambda;

import com.github.eacryo.mongoflex.TestApplication;
import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.bean.LiyueCharacter;
import com.github.eacryo.mongoflex.query.LambdaQueryWrapper;
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

/**
 * Tests for newly added query operators: LIKE, NOT_LIKE, BETWEEN,
 * IS_NULL, IS_NOT_NULL, NOT, MOD, TYPE — using Character's new
 * fields (vision, weapon, rarity, level, constellation, baseATK,
 * friendship, isArchon, talents).
 */
@Slf4j
@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("v2")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LambdaQueryWrapperOperatorTest {

    @Autowired
    private CharacterRepository repo;

    private String id1, id2;

    /**
     * c1 = Hu Tao style: Pyro, Polearm, 5★, Lv90, C6, high ATK, max friendship, not Archon
     * c2 = Zhongli style: Geo, Polearm, 5★, Lv80, C0, mid ATK, friendship 7, IS Archon
     */
    @BeforeEach
    void setUp() {
        id1 = UlidCreator.getUlid().toString();
        Character c1 = new Character();
        c1.setId(id1);
        c1.setName("OperatorTest-A-" + id1);
        c1.setAddress("Liyue");
        c1.setEmail("hutao@example.com");
        c1.setBirthday(new Date());
        c1.setVision("Pyro");
        c1.setWeapon("Polearm");
        c1.setRarity(5);
        c1.setLevel(90);
        c1.setConstellation(6);
        c1.setBaseATK(350);
        c1.setFriendship(10);
        c1.setIsArchon(false);
        c1.setTalents(Arrays.asList("蝶引来生", "血之灶火", "彼岸蝶舞"));
        repo.insert(c1);

        id2 = UlidCreator.getUlid().toString();
        Character c2 = new Character();
        c2.setId(id2);
        c2.setName("OperatorTest-B-" + id2);
        c2.setAddress("Liyue");
        c2.setBirthday(new Date(System.currentTimeMillis() + 86400000L));
        c2.setVision("Geo");
        c2.setWeapon("Polearm");
        c2.setRarity(5);
        c2.setLevel(80);
        c2.setConstellation(0);
        c2.setBaseATK(300);
        c2.setFriendship(7);
        c2.setIsArchon(true);
        c2.setTalents(Arrays.asList("地心", "天星", "天动万象"));
        repo.insert(c2);

        log.info("inserted: id1={} (Pyro Lv90 C6), id2={} (Geo Lv80 C0, Archon)", id1, id2);
    }

    @AfterEach
    void tearDown() {
        repo.deleteOneById(id1);
        repo.deleteOneById(id2);
        log.info("cleaned up");
    }

    // ========== LIKE / NOT_LIKE ==========

    @Test
    @Order(1)
    void testLike() {
        log.info("=== test like ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.like(Character::getName, "*OperatorTest-*");
        List<Character> result = repo.findList(wrapper);
        log.info("like result size: {}", result.size());
        Assertions.assertTrue(result.size() >= 2);
        for (Character c : result) {
            Assertions.assertTrue(c.getName().contains("OperatorTest-"));
        }
    }

    @Test
    @Order(2)
    void testLikePercentWildcard() {
        log.info("=== test like with % wildcard ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.like(Character::getName, "%OperatorTest%");
        List<Character> result = repo.findList(wrapper);
        log.info("like % result size: {}", result.size());
        Assertions.assertTrue(result.size() >= 2);
    }

    @Test
    @Order(3)
    void testNotLike() {
        log.info("=== test notLike ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.notLike(Character::getName, "*OperatorTest-*");
        List<Character> result = repo.findList(wrapper);
        log.info("notLike result size: {}", result.size());
        for (Character c : result) {
            Assertions.assertFalse(c.getName().contains("OperatorTest-"));
        }
    }

    // ========== BETWEEN ==========

    @Test
    @Order(4)
    void testBetweenDate() {
        log.info("=== test between (date) ===");
        Date past = new Date(System.currentTimeMillis() - 3600_000);
        Date future = new Date(System.currentTimeMillis() + 172800_000);
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.between(Character::getBirthday, past, future)
               .like(Character::getName, "*OperatorTest-*");
        List<Character> result = repo.findList(wrapper);
        log.info("between date result size: {}", result.size());
        Assertions.assertTrue(result.size() >= 2);
    }

    @Test
    @Order(5)
    void testBetweenNumeric() {
        log.info("=== test between (numeric: baseATK 250~400) ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.between(Character::getBaseATK, 250, 400)
               .like(Character::getName, "*OperatorTest-*");
        List<Character> result = repo.findList(wrapper);
        log.info("between baseATK result size: {}", result.size());
        Assertions.assertEquals(2, result.size());
    }

    @Test
    @Order(6)
    void testBetweenNumericNarrow() {
        log.info("=== test between (numeric: level 85~95) ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.between(Character::getLevel, 85, 95)
               .like(Character::getName, "*OperatorTest-*");
        List<Character> result = repo.findList(wrapper);
        log.info("between level 85~95 result size: {}", result.size());
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(Integer.valueOf(90), result.get(0).getLevel());
    }

    // ========== IS_NULL / IS_NOT_NULL ==========

    @Test
    @Order(7)
    void testIsNull() {
        log.info("=== test isNull ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.isNull(Character::getPhone)
               .like(Character::getName, "*OperatorTest-*");
        List<Character> result = repo.findList(wrapper);
        log.info("isNull result size: {}", result.size());
        Assertions.assertTrue(result.size() >= 2);
        for (Character c : result) {
            Assertions.assertNull(c.getPhone());
        }
    }

    @Test
    @Order(8)
    void testIsNotNull() {
        log.info("=== test isNotNull (vision should be set for both) ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.isNotNull(Character::getVision)
               .like(Character::getName, "*OperatorTest-*");
        List<Character> result = repo.findList(wrapper);
        log.info("isNotNull vision result size: {}", result.size());
        Assertions.assertEquals(2, result.size());
        for (Character c : result) {
            Assertions.assertNotNull(c.getVision());
        }
    }

    // ========== NOT ==========

    @Test
    @Order(9)
    void testNot() {
        log.info("=== test not (address != Mondstadt) ===");
        LambdaQueryWrapper<Character> sub = new LambdaQueryWrapper<>(Character.class);
        sub.eq(Character::getAddress, "Mondstadt");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.not(sub)
               .like(Character::getName, "*OperatorTest-*");
        List<Character> result = repo.findList(wrapper);
        log.info("not result size: {}", result.size());
        Assertions.assertEquals(2, result.size());
        for (Character c : result) {
            Assertions.assertNotEquals("Mondstadt", c.getAddress());
        }
    }

    // ========== MOD ==========

    @Test
    @Order(10)
    void testModLevel() {
        log.info("=== test mod (level % 10 == 0) ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.mod(Character::getLevel, 10, 0)
               .like(Character::getName, "*OperatorTest-*");
        List<Character> result = repo.findList(wrapper);
        log.info("mod level%10==0 result size: {}", result.size());
        Assertions.assertEquals(2, result.size()); // Lv80 and Lv90 both match
    }

    @Test
    @Order(11)
    void testModConstellation() {
        log.info("=== test mod (constellation % 2 == 0 → even) ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.mod(Character::getConstellation, 2, 0)
               .like(Character::getName, "*OperatorTest-*");
        List<Character> result = repo.findList(wrapper);
        log.info("mod C%2==0 result size: {}", result.size());
        // c1 C6 (even), c2 C0 (even) → both match
        Assertions.assertEquals(2, result.size());
    }

    // ========== TYPE ==========

    @Test
    @Order(12)
    void testTypeString() {
        log.info("=== test type (vision = string) ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.type(Character::getVision, "string")
               .like(Character::getName, "*OperatorTest-*");
        List<Character> result = repo.findList(wrapper);
        log.info("type=string result size: {}", result.size());
        Assertions.assertEquals(2, result.size());
    }

    @Test
    @Order(13)
    void testTypeInt() {
        log.info("=== test type (level = int) ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.type(Character::getLevel, "int")
               .like(Character::getName, "*OperatorTest-*");
        List<Character> result = repo.findList(wrapper);
        log.info("type=int result size: {}", result.size());
        Assertions.assertEquals(2, result.size());
    }

    @Test
    @Order(14)
    void testTypeBool() {
        log.info("=== test type (isArchon = bool) ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.type(Character::getIsArchon, "bool")
               .like(Character::getName, "*OperatorTest-*");
        List<Character> result = repo.findList(wrapper);
        log.info("type=bool result size: {}", result.size());
        Assertions.assertEquals(2, result.size());
    }

    @Test
    @Order(15)
    void testTypeArray() {
        log.info("=== test type (talents = array) ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.type(Character::getTalents, "array")
               .like(Character::getName, "*OperatorTest-*");
        List<Character> result = repo.findList(wrapper);
        log.info("type=array result size: {}", result.size());
        Assertions.assertEquals(2, result.size());
    }

    // ========== @CollectionField 映射 ==========

    @Test
    @Order(16)
    void testCollectionFieldMappingWeapon() {
        log.info("=== test @CollectionField mapping (weapon → weapon_type) ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.eq(Character::getWeapon, "Polearm")
               .like(Character::getName, "*OperatorTest-*");
        List<Character> result = repo.findList(wrapper);
        log.info("weapon=Polearm result size: {}", result.size());
        Assertions.assertEquals(2, result.size());
    }

    @Test
    @Order(17)
    void testCollectionFieldMappingIsArchon() {
        log.info("=== test @CollectionField mapping (isArchon → is_archon) ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.eq(Character::getIsArchon, true)
               .like(Character::getName, "*OperatorTest-*");
        List<Character> result = repo.findList(wrapper);
        log.info("isArchon=true result size: {}", result.size());
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(id2, result.get(0).getId());
    }

    // ========== LiyueCharacter 子类 ==========

    @Test
    @Order(18)
    void testLiyueCharacterSubclass() {
        log.info("=== test LiyueCharacter subclass fields ===");
        // Insert a LiyueCharacter — polymorphism works through CharacterRepository
        String id3 = UlidCreator.getUlid().toString();
        LiyueCharacter lc = new LiyueCharacter();
        lc.setId(id3);
        lc.setName("OperatorTest-C-" + id3);
        lc.setAddress("Liyue");
        lc.setVision("Hydro");
        lc.setWeapon("Sword");
        lc.setRarity(5);
        lc.setLevel(90);
        lc.setTitle("掩月天权");
        lc.setAffiliation("璃月七星");
        lc.setIsAdeptus(false);
        lc.setMoraAmount(999_999_999L);

        try {
            repo.insert(lc);

            // Query via parent field (inherited)
            LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
            wrapper.eq(Character::getVision, "Hydro")
                   .like(Character::getName, "*OperatorTest-*");
            List<Character> result = repo.findList(wrapper);
            log.info("LiyueCharacter query by vision=Hydro: size={}", result.size());
            Assertions.assertEquals(1, result.size());

            // Subclass-specific fields are stored in MongoDB but queried via raw field names
            // since CharacterRepository uses Character class which doesn't have Liyue fields
            // (this is expected behavior — use a dedicated LiyueCharacterRepository for that)
        } finally {
            repo.deleteOneById(id3);
        }
    }

    // ========== Combined ==========

    @Test
    @Order(19)
    void testCombinedNewOperators() {
        log.info("=== test combined new operators ===");
        LambdaQueryWrapper<Character> notSub = new LambdaQueryWrapper<>(Character.class);
        notSub.eq(Character::getLevel, 80);
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.like(Character::getName, "*OperatorTest-*")
               .isNotNull(Character::getVision)
               .between(Character::getBaseATK, 250, 400)
               .not(notSub);
        List<Character> result = repo.findList(wrapper);
        log.info("combined result size: {}", result.size());
        // Should only match c1: Pyro, baseATK 350, NOT level 80
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("Pyro", result.get(0).getVision());
    }
}
