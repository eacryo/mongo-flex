package com.github.eacryo.mongoflex.lambda;

import com.github.eacryo.mongoflex.TestApplication;
import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.bean.Region;
import com.github.eacryo.mongoflex.entity.PageDTO;
import com.github.eacryo.mongoflex.query.LambdaQueryWrapper;
import com.github.eacryo.mongoflex.ulid.Ulid;
import com.github.eacryo.mongoflex.util.FieldPath;
import com.github.eacryo.mongoflex.v2.CharacterRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

/**
 * Integration tests for nested field (dot-notation) queries across all query paths. /
 * 嵌套字段（点号语法）查询在各查询路径上的集成测试。
 * <ul>
 *   <li>Lambda path via {@link FieldPath} chained method references / Lambda 路径：{@link FieldPath} 链式方法引用</li>
 *   <li>Annotation path via {@code @Find} raw JSON dot keys / 注解路径：{@code @Find} 原始 JSON 点号键</li>
 *   <li>Entity path — exact subdocument match (single-layer semantics) / Entity 路径——精确子文档匹配（单层语义）</li>
 * </ul>
 * Field mapping under test: {@code region} → {@code home_region}, {@code mainCity} → {@code main_city},
 * so the rendered path is {@code home_region.main_city}. / 被测字段映射：{@code region} → {@code home_region}、
 * {@code mainCity} → {@code main_city}，渲染后的路径为 {@code home_region.main_city}。
 */
@Slf4j
@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("v2")
public class NestedFieldQueryTest {

    @Autowired
    private CharacterRepository repo;

    private String runId;
    private String namePrefix;
    private String liyueCity;
    private String idLiyue, idMondstadt, idNoRegion;

    @BeforeEach
    void setUp() {
        runId = Ulid.generate();
        namePrefix = "NestedField-" + runId;
        liyueCity = "Liyue Harbor-" + runId;

        idLiyue = Ulid.generate();
        Character liyue = new Character();
        liyue.setId(idLiyue);
        liyue.setName(namePrefix + "-Liyue");
        liyue.setRegion(new Region("Liyue", liyueCity, 500));
        repo.insert(liyue);

        idMondstadt = Ulid.generate();
        Character mondstadt = new Character();
        mondstadt.setId(idMondstadt);
        mondstadt.setName(namePrefix + "-Mondstadt");
        mondstadt.setRegion(new Region("Mondstadt", "Mondstadt City-" + runId, 1500));
        repo.insert(mondstadt);

        idNoRegion = Ulid.generate();
        Character noRegion = new Character();
        noRegion.setId(idNoRegion);
        noRegion.setName(namePrefix + "-NoRegion");
        repo.insert(noRegion);

        log.info("inserted: liyue={}, mondstadt={}, noRegion={}", idLiyue, idMondstadt, idNoRegion);
    }

    @AfterEach
    void tearDown() {
        repo.deleteOneById(idLiyue);
        repo.deleteOneById(idMondstadt);
        repo.deleteOneById(idNoRegion);
        log.info("cleaned up run {}", runId);
    }

    /** Scope a wrapper to this run's documents / 将查询范围限定到本次运行插入的文档 */
    private LambdaQueryWrapper<Character> scoped() {
        return new LambdaQueryWrapper<>(Character.class)
                .like(Character::getName, namePrefix + "*");
    }

    @Test
    void testFieldPathEq() {
        log.info("=== FieldPath eq: home_region.main_city ===");
        List<Character> result = repo.findList(scoped()
                .eq(FieldPath.of(Character::getRegion, Region::getMainCity), liyueCity));
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(idLiyue, result.get(0).getId());
        log.info("matched: {}", result.get(0).getName());
    }

    @Test
    void testFieldPathFluentThen() {
        log.info("=== FieldPath of().then(): home_region.nation ===");
        List<Character> result = repo.findList(scoped()
                .eq(FieldPath.of(Character::getRegion).then(Region::getNation), "Mondstadt"));
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(idMondstadt, result.get(0).getId());
    }

    @Test
    void testFieldPathNumericCompare() {
        log.info("=== FieldPath gt/between: home_region.altitude ===");
        List<Character> high = repo.findList(scoped()
                .gt(FieldPath.of(Character::getRegion, Region::getAltitude), 1000));
        Assertions.assertEquals(1, high.size());
        Assertions.assertEquals(idMondstadt, high.get(0).getId());

        List<Character> low = repo.findList(scoped()
                .between(FieldPath.of(Character::getRegion, Region::getAltitude), 0, 1000));
        Assertions.assertEquals(1, low.size());
        Assertions.assertEquals(idLiyue, low.get(0).getId());
    }

    @Test
    void testFieldPathExists() {
        log.info("=== FieldPath exists: home_region.main_city ===");
        List<Character> withRegion = repo.findList(scoped()
                .exists(FieldPath.of(Character::getRegion, Region::getMainCity), true));
        Assertions.assertEquals(2, withRegion.size());

        List<Character> withoutRegion = repo.findList(scoped()
                .exists(FieldPath.of(Character::getRegion, Region::getMainCity), false));
        Assertions.assertEquals(1, withoutRegion.size());
        Assertions.assertEquals(idNoRegion, withoutRegion.get(0).getId());
    }

    @Test
    void testFieldPathSortInPage() {
        log.info("=== FieldPath orderByDesc in findPage: home_region.altitude ===");
        PageDTO<Character> pageDTO = new PageDTO<>();
        pageDTO.setCurrentPage(1L);
        pageDTO.setPageSize(10L);

        PageDTO<Character> result = repo.findPage(scoped()
                        .exists(FieldPath.of(Character::getRegion, Region::getAltitude), true)
                        .orderByDesc(FieldPath.of(Character::getRegion, Region::getAltitude)),
                pageDTO);
        Assertions.assertEquals(2, result.getRecords().size());
        Assertions.assertEquals(idMondstadt, result.getRecords().get(0).getId());
        Assertions.assertEquals(idLiyue, result.getRecords().get(1).getId());
    }

    @Test
    void testFindAnnotationDotNotation() {
        log.info("=== @Find raw JSON dot notation: home_region.main_city ===");
        List<Character> result = repo.findListByRegionCity(liyueCity);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(idLiyue, result.get(0).getId());
    }

    @Test
    void testNestedRoundTrip() {
        log.info("=== nested subdocument write/read round trip ===");
        Character loaded = repo.findById(idLiyue);
        Assertions.assertNotNull(loaded.getRegion());
        Assertions.assertEquals("Liyue", loaded.getRegion().getNation());
        Assertions.assertEquals(liyueCity, loaded.getRegion().getMainCity());
        Assertions.assertEquals(500, loaded.getRegion().getAltitude());
    }

    /**
     * Pin the documented byEntity semantics: nested objects are matched as an exact
     * subdocument (single-layer query), NOT flattened to per-field dot notation. For
     * per-field nested matching use FieldPath or @Find dot notation. /
     * 固化文档化的 byEntity 语义：嵌套对象按精确子文档匹配（单层查询），不会展平为逐字段点号匹配。
     * 需要逐字段嵌套匹配时请使用 FieldPath 或 @Find 点号语法。
     */
    @Test
    void testByEntityExactSubdocumentSemantics() {
        log.info("=== byEntity: exact subdocument match semantics ===");
        // Full identical subdocument — matches / 完整一致的子文档——命中
        Character fullProbe = new Character();
        fullProbe.setName(namePrefix + "-Liyue");
        fullProbe.setRegion(new Region("Liyue", liyueCity, 500));
        List<Character> fullMatch = repo.findListByEntity(fullProbe);
        Assertions.assertEquals(1, fullMatch.size());
        Assertions.assertEquals(idLiyue, fullMatch.get(0).getId());

        // Partial subdocument — no match (exact match requires all fields) / 部分子文档——不命中（精确匹配要求全部字段一致）
        Character partialProbe = new Character();
        partialProbe.setName(namePrefix + "-Liyue");
        partialProbe.setRegion(new Region(null, liyueCity, null));
        List<Character> partialMatch = repo.findListByEntity(partialProbe);
        Assertions.assertTrue(partialMatch.isEmpty());
    }
}
