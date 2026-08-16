package com.github.eacryo.mongoflex.lambda;

import com.github.eacryo.mongoflex.TestApplication;
import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.query.LambdaQueryWrapper;
import com.github.eacryo.mongoflex.v2.CharacterRepository;
import com.github.eacryo.mongoflex.ulid.Ulid;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for nested boolean group composition (and/or/not) in
 * {@link LambdaQueryWrapper}: A AND (B OR C), (A AND B) OR C, NOT groups,
 * deep nesting, and @CollectionField resolution inside nested groups. /
 * {@link LambdaQueryWrapper} 嵌套布尔分组（and/or/not）的集成测试：
 * A AND (B OR C)、(A AND B) OR C、NOT 分组、深层嵌套、以及嵌套组内的
 * {@code @CollectionField} 解析。
 *
 * <p>Each test class load uses a unique name prefix, and tearDown performs a real
 * delete, so exact-count assertions are deterministic regardless of database state. /
 * 每次类加载使用唯一名字前缀，tearDown 做真实删除，因此精确数量断言不受库中历史数据影响。</p>
 */
@Slf4j
@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("v2")
public class LambdaQueryBooleanTest {

    @Autowired
    private CharacterRepository repo;

    /** Unique per class-load prefix so no historical data can match / 每次类加载唯一前缀，防止历史数据命中 */
    private static final String PREFIX = "BoolTest-" + Ulid.generate();

    private String id1, id2, id3;

    /**
     * c1 = Pyro Lv90, not Archon, Polearm
     * c2 = Geo  Lv80, Archon,    Sword
     * c3 = Pyro Lv60, Archon,    Catalyst
     */
    @BeforeEach
    void setUp() {
        id1 = Ulid.generate();
        Character c1 = new Character();
        c1.setId(id1);
        c1.setName(PREFIX + "-A-" + id1);
        c1.setVision("Pyro");
        c1.setLevel(90);
        c1.setIsArchon(false);
        c1.setWeapon("Polearm");
        repo.insert(c1);

        id2 = Ulid.generate();
        Character c2 = new Character();
        c2.setId(id2);
        c2.setName(PREFIX + "-B-" + id2);
        c2.setVision("Geo");
        c2.setLevel(80);
        c2.setIsArchon(true);
        c2.setWeapon("Sword");
        repo.insert(c2);

        id3 = Ulid.generate();
        Character c3 = new Character();
        c3.setId(id3);
        c3.setName(PREFIX + "-C-" + id3);
        c3.setVision("Pyro");
        c3.setLevel(60);
        c3.setIsArchon(true);
        c3.setWeapon("Catalyst");
        repo.insert(c3);

        log.info("inserted: id1={} (Pyro Lv90), id2={} (Geo Archon), id3={} (Pyro Archon Catalyst)", id1, id2, id3);
    }

    @AfterEach
    void tearDown() {
        repo.deleteOneById(id1);
        repo.deleteOneById(id2);
        repo.deleteOneById(id3);
        log.info("cleaned up real deletes");
    }

    private Set<String> ids(List<Character> list) {
        return list.stream().map(Character::getId).collect(Collectors.toSet());
    }

    @Test
    void testAndOrNested() {
        log.info("=== A AND (B OR C): vision=Pyro AND (level>=80 OR isArchon=true) ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.eq(Character::getVision, "Pyro")
               .and(x -> x.gte(Character::getLevel, 80)
                           .or()
                           .eq(Character::getIsArchon, true))
               .like(Character::getName, PREFIX + "*");
        List<Character> result = repo.findList(wrapper);
        // c1 (Pyro Lv90) and c3 (Pyro, Archon) match; c2 (Geo) does not / c1、c3 命中；c2 不命中
        assertEquals(new HashSet<>(Arrays.asList(id1, id3)), ids(result));
    }

    @Test
    void testOrGroupAnded() {
        log.info("=== (A AND B) OR C: (vision=Pyro AND level>=80) OR weapon=Sword ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.or(x -> x.eq(Character::getVision, "Pyro")
                         .gte(Character::getLevel, 80)
                         .or()
                         .eq(Character::getWeapon, "Sword"))
               .like(Character::getName, PREFIX + "*");
        List<Character> result = repo.findList(wrapper);
        // c1 (Pyro Lv90) and c2 (Sword) match; c3 (Catalyst, Lv60) does not / c1、c2 命中；c3 不命中
        assertEquals(new HashSet<>(Arrays.asList(id1, id2)), ids(result));
    }

    @Test
    void testNotGroup() {
        log.info("=== NOT (A AND B): NOT(vision=Pyro AND level>=80) ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.like(Character::getName, PREFIX + "*")
               .not(x -> x.eq(Character::getVision, "Pyro")
                           .gte(Character::getLevel, 80));
        List<Character> result = repo.findList(wrapper);
        // c1 excluded; c2 and c3 match / c1 被排除；c2、c3 命中
        assertEquals(new HashSet<>(Arrays.asList(id2, id3)), ids(result));
    }

    @Test
    void testDeepNesting() {
        log.info("=== A AND ((B AND C) OR (D AND E)): Pyro AND ((Lv>=80 AND !Archon) OR (Catalyst AND Archon)) ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.eq(Character::getVision, "Pyro")
               .and(x -> x.and(y -> y.gte(Character::getLevel, 80)
                                      .eq(Character::getIsArchon, false))
                           .or()
                           .and(z -> z.eq(Character::getWeapon, "Catalyst")
                                      .eq(Character::getIsArchon, true)))
               .like(Character::getName, PREFIX + "*");
        List<Character> result = repo.findList(wrapper);
        // c1 (Pyro Lv90 !Archon) and c3 (Pyro Catalyst Archon) match; c2 (Geo) does not / c1、c3 命中；c2 不命中
        assertEquals(new HashSet<>(Arrays.asList(id1, id3)), ids(result));
    }

    @Test
    void testNestedCollectionFieldMapping() {
        log.info("=== @CollectionField inside nested groups: weapon_type=Sword OR weapon_type=Catalyst ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.like(Character::getName, PREFIX + "*")
               .and(x -> x.eq(Character::getWeapon, "Sword")
                           .or()
                           .eq(Character::getWeapon, "Catalyst"));
        List<Character> result = repo.findList(wrapper);
        // weapon is mapped to weapon_type; c2 (Sword) and c3 (Catalyst) match / weapon 映射为 weapon_type；c2、c3 命中
        assertEquals(new HashSet<>(Arrays.asList(id2, id3)), ids(result));
    }

    @Test
    void testNestedGroupWriteGuard() {
        log.info("=== delete/update with only empty nested groups must be rejected ===");
        // An empty nested AND group yields no effective conditions — destructive operations
        // must be rejected instead of running against the whole collection. /
        // 只有空嵌套 AND 分组时没有有效条件——破坏性操作必须被拒绝，而不是全集合执行。
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.and(x -> { });
        IllegalArgumentException deleteEx = assertThrows(IllegalArgumentException.class,
                () -> repo.deleteMany(wrapper));
        assertTrue(deleteEx.getMessage().contains("deleteAll"));
        IllegalArgumentException updateEx = assertThrows(IllegalArgumentException.class,
                () -> repo.updateMany(wrapper, new Character()));
        assertTrue(updateEx.getMessage().contains("requires at least one condition"));
    }
}
