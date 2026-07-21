package com.github.eacryo.mongoflex.v2;

import com.github.eacryo.mongoflex.TestApplication;
import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.bean.CharacterWithWeapon;
import com.github.eacryo.mongoflex.bean.Weapon;
import com.github.eacryo.mongoflex.query.AggregationWrapper;
import com.github.eacryo.mongoflex.query.MongoOps;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("v2")
public class AggregationTest {

    @Autowired
    private CharacterRepository characterRepositoryV2;

    @Autowired
    private WeaponRepository weaponRepository;

    @Autowired
    private MongoOps mongoOps;

    private String huTaoId;
    private String zhongliId;
    private String homaId;
    private String vvId;

    @BeforeEach
    void setUp() {
        // Insert Hu Tao
        Character huTao = new Character();
        huTao.setName("HuTao");
        huTao.setVision("Pyro");
        huTao.setLevel(90);
        huTao.setArea("Liyue");
        huTao.setRarity(5);
        huTao.setWeapon("Polearm");
        huTao.setBaseATK(106);
        huTao.setIsArchon(false);
        huTao = characterRepositoryV2.insert(huTao);
        huTaoId = huTao.getId();

        // Insert Zhongli
        Character zhongli = new Character();
        zhongli.setName("Zhongli");
        zhongli.setVision("Geo");
        zhongli.setLevel(90);
        zhongli.setArea("Liyue");
        zhongli.setRarity(5);
        zhongli.setWeapon("Polearm");
        zhongli.setBaseATK(251);
        zhongli.setIsArchon(true);
        zhongli = characterRepositoryV2.insert(zhongli);
        zhongliId = zhongli.getId();

        // Insert a weapon for Hu Tao
        Weapon homa = new Weapon();
        homa.setName("Staff of Homa");
        homa.setType("Polearm");
        homa.setBaseAtk(608);
        homa.setSubStat("CRIT DMG");
        homa.setSubValue(66.2);
        homa.setRarity(5);
        homa.setCharacterId(huTaoId);
        homa = weaponRepository.insert(homa);
        homaId = homa.getId();

        // Insert a weapon for Zhongli
        Weapon vv = new Weapon();
        vv.setName("Vortex Vanquisher");
        vv.setType("Polearm");
        vv.setBaseAtk(608);
        vv.setSubStat("ATK");
        vv.setSubValue(49.6);
        vv.setRarity(5);
        vv.setCharacterId(zhongliId);
        vv = weaponRepository.insert(vv);
        vvId = vv.getId();
    }

    @AfterEach
    void tearDown() {
        // Mark test characters as deleted / 标记测试角色为逻辑删除
        Character huTao = new Character();
        huTao.setId(huTaoId);
        huTao.setDeleted(true);
        characterRepositoryV2.updateOneById(huTao);

        Character zhongli = new Character();
        zhongli.setId(zhongliId);
        zhongli.setDeleted(true);
        characterRepositoryV2.updateOneById(zhongli);

        // Mark test weapons as deleted / 标记测试武器为逻辑删除
        Weapon homa = new Weapon();
        homa.setId(homaId);
        homa.setDeleted(true);
        weaponRepository.updateOneById(homa);

        Weapon vv = new Weapon();
        vv.setId(vvId);
        vv.setDeleted(true);
        weaponRepository.updateOneById(vv);
    }

    // ── Annotation @Aggregate path / 注解 @Aggregate 路径 ──

    @Test
    public void testLookupWeaponsByAnnotation() {
        List<CharacterWithWeapon> results = characterRepositoryV2.lookupWeapons();
        assertNotNull(results);
        assertFalse(results.isEmpty(), "should have at least one joined result");
        for (CharacterWithWeapon r : results) {
            assertNotNull(r.getName());
            assertNotNull(r.getWeapon(), "weapon should not be null after $lookup + $unwind");
            assertNotNull(r.getWeapon().getName());
        }
        System.out.println("lookupWeapons result count: " + results.size());
        if (!results.isEmpty()) {
            System.out.println("  first: " + results.get(0).getName() + " -> " + results.get(0).getWeapon().getName());
        }
    }

    @Test
    public void testLookupWeaponsByVisionWithMatch() {
        List<CharacterWithWeapon> pyroResults = characterRepositoryV2.lookupWeaponsByVision("Pyro");
        assertNotNull(pyroResults);
        assertFalse(pyroResults.isEmpty());
        for (CharacterWithWeapon r : pyroResults) {
            assertEquals("Pyro", r.getVision(), "should only have Pyro characters");
            assertNotNull(r.getWeapon());
        }
        System.out.println("lookupWeaponsByVision(Pyro) count: " + pyroResults.size());

        List<CharacterWithWeapon> geoResults = characterRepositoryV2.lookupWeaponsByVision("Geo");
        assertNotNull(geoResults);
        assertFalse(geoResults.isEmpty());
        for (CharacterWithWeapon r : geoResults) {
            assertEquals("Geo", r.getVision(), "should only have Geo characters");
            assertNotNull(r.getWeapon());
        }
        System.out.println("lookupWeaponsByVision(Geo) count: " + geoResults.size());
    }

    @Test
    public void testGroupByVisionCount() {
        List<Object> results = characterRepositoryV2.groupByVisionCount();
        assertNotNull(results);
        assertFalse(results.isEmpty(), "should have group results");
        System.out.println("groupByVisionCount result count: " + results.size());
        if (!results.isEmpty()) {
            Object first = results.get(0);
            System.out.println("  first element type: " + first.getClass().getName());
            if (first instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) first;
                System.out.println("  first: " + map);
            }
        }
    }

    // ── Lambda AggregationWrapper + MongoOps path / Lambda AggregationWrapper + MongoOps 路径 ──

    @Test
    public void testLookupWeaponsByLambda() {
        List<CharacterWithWeapon> results = mongoOps.aggregate(
                new AggregationWrapper<>(Character.class)
                        .lookup(Weapon.class, Character::getId, Weapon::getCharacterId, "weapon")
                        .unwind("weapon"),
                CharacterWithWeapon.class);

        assertNotNull(results);
        assertFalse(results.isEmpty());
        for (CharacterWithWeapon r : results) {
            assertNotNull(r.getName());
            assertNotNull(r.getWeapon());
            assertNotNull(r.getWeapon().getName());
        }
        System.out.println("lambda lookup result count: " + results.size());
        if (!results.isEmpty()) {
            System.out.println("  first: " + results.get(0).getName() + " -> " + results.get(0).getWeapon().getName());
        }
    }

    @Test
    public void testLookupWeaponsByLambdaWithMatch() {
        List<CharacterWithWeapon> results = mongoOps.aggregate(
                new AggregationWrapper<>(Character.class)
                        .match(w -> w.eq(Character::getVision, "Pyro"))
                        .lookup(Weapon.class, Character::getId, Weapon::getCharacterId, "weapon")
                        .unwind("weapon")
                        .sortDesc("level"),
                CharacterWithWeapon.class);

        assertNotNull(results);
        assertFalse(results.isEmpty());
        for (CharacterWithWeapon r : results) {
            assertEquals("Pyro", r.getVision());
            assertNotNull(r.getWeapon());
            System.out.println("  " + r.getName() + " (Lv." + r.getLevel() + ") -> " + r.getWeapon().getName());
        }
    }

    @Test
    public void testLookupWeaponsWithLimit() {
        List<CharacterWithWeapon> results = mongoOps.aggregate(
                new AggregationWrapper<>(Character.class)
                        .lookup(Weapon.class, Character::getId, Weapon::getCharacterId, "weapon")
                        .unwind("weapon")
                        .sortDesc("level")
                        .limit(1),
                CharacterWithWeapon.class);

        assertNotNull(results);
        assertEquals(1, results.size(), "limit should restrict to 1");
        assertNotNull(results.get(0).getWeapon());
        System.out.println("lambda lookup with limit: " + results.get(0).getName() + " -> " + results.get(0).getWeapon().getName());
    }

    @Test
    public void testGroupByVisionWithLambda() {
        // Group by vision, count characters and average level
        // 按元素分组，统计角色数量和平均等级
        List<VisionStats> stats = mongoOps.aggregate(
                new AggregationWrapper<>(Character.class)
                        .group(Character::getVision)
                            .count("count")
                            .avg("avgLevel", Character::getLevel)
                            .end()
                        .sortDesc("count")
                        .limit(5),
                VisionStats.class);

        assertNotNull(stats);
        assertFalse(stats.isEmpty());
        boolean foundPyro = false;
        for (VisionStats s : stats) {
            if (s.getVisionId() != null) {
                System.out.println("  vision=" + s.getVisionId() + " count=" + s.getCount() + " avgLevel=" + s.getAvgLevel());
                if ("Pyro".equals(s.getVisionId())) {
                    foundPyro = true;
                    assertTrue(s.getCount() > 0);
                }
            } else {
                System.out.println("  vision=null (documents without vision) count=" + s.getCount());
            }
        }
        assertTrue(foundPyro, "should have Pyro group from setUp data");
    }

    @Test
    public void testMatchLookupGroupByLambda() {
        // Characters in Liyue grouped by rarity: count and total baseATK
        List<RarityStats> stats = mongoOps.aggregate(
                new AggregationWrapper<>(Character.class)
                        .match(w -> w.eq(Character::getArea, "Liyue"))
                        .group(Character::getRarity)
                            .count("characterCount")
                            .sum("totalBaseAtk", Character::getBaseATK)
                            .end()
                        .sortDesc("characterCount"),
                RarityStats.class);

        assertNotNull(stats);
        assertFalse(stats.isEmpty());
        for (RarityStats s : stats) {
            assertNotNull(s.getRarity());
            assertTrue(s.getCharacterCount() > 0);
            System.out.println("  rarity=" + s.getRarity() + " count=" + s.getCharacterCount() + " totalBaseAtk=" + s.getTotalBaseAtk());
        }
    }

    // ── Output DTOs for group tests / 分组测试的输出 DTO ──

    @SuppressWarnings("unused")
    public static class VisionStats {
        @com.github.eacryo.mongoflex.annotation.CollectionField("_id")
        private String visionId;
        private long count;
        private Double avgLevel;

        public String getVisionId() { return visionId; }
        public void setVisionId(String visionId) { this.visionId = visionId; }
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
        public Double getAvgLevel() { return avgLevel; }
        public void setAvgLevel(Double avgLevel) { this.avgLevel = avgLevel; }
    }

    @SuppressWarnings("unused")
    public static class RarityStats {
        @com.github.eacryo.mongoflex.annotation.CollectionField("_id")
        private Integer rarity;
        private long characterCount;
        private Integer totalBaseAtk;

        public Integer getRarity() { return rarity; }
        public void setRarity(Integer rarity) { this.rarity = rarity; }
        public long getCharacterCount() { return characterCount; }
        public void setCharacterCount(long characterCount) { this.characterCount = characterCount; }
        public Integer getTotalBaseAtk() { return totalBaseAtk; }
        public void setTotalBaseAtk(Integer totalBaseAtk) { this.totalBaseAtk = totalBaseAtk; }
    }
}
