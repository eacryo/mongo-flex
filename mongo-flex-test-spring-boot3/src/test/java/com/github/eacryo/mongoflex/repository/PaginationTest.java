package com.github.eacryo.mongoflex.repository;

import com.github.eacryo.mongoflex.TestApplication;
import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.entity.PageDTO;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for offset-based pagination and the sortBy() conveniences on
 * {@link PageDTO}. / {@link PageDTO} 的 offset 分页与 sortBy() 便捷方法的集成测试。
 *
 * <p>Each test uses a per-test unique name and real deletes in tearDown, so exact
 * assertions are deterministic regardless of database state. / 每个测试使用独立的唯一名字
 * 并在 tearDown 真实删除，因此精确断言不受数据库状态影响。</p>
 */
@Slf4j
@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("v2")
public class PaginationTest {

    @Autowired
    private CharacterRepository repo;

    private String commonName;

    @BeforeEach
    void setUp() {
        commonName = "PaginationTest-" + Ulid.generate();
    }

    @AfterEach
    void tearDown() {
        repo.deleteMany(Character::getName, commonName);
    }

    private Character insert(String address, int level) {
        Character c = new Character();
        c.setId(Ulid.generate());
        c.setName(commonName);
        c.setAddress(address);
        c.setLevel(level);
        repo.insert(c);
        return c;
    }

    private Character queryProbe() {
        Character query = new Character();
        query.setName(commonName);
        return query;
    }

    @Test
    void testPageByOffset() {
        log.info("=== offset=2, pageSize=2, sort address asc — returns 3rd and 4th docs ===");
        for (int i = 0; i < 6; i++) {
            insert("OffAddr-" + i, i);
        }
        PageDTO<Character> pageDTO = new PageDTO<>();
        pageDTO.setOffset(2L);
        pageDTO.setPageSize(2L);
        pageDTO.sortBy("address", true);
        PageDTO<Character> result = repo.findPageByEntity(queryProbe(), pageDTO);
        assertEquals(6, result.getTotal());
        assertEquals(2, result.getRecords().size());
        assertEquals("OffAddr-2", result.getRecords().get(0).getAddress());
        assertEquals("OffAddr-3", result.getRecords().get(1).getAddress());
    }

    @Test
    void testPageByOffsetZero() {
        log.info("=== offset=0 starts from the first document ===");
        for (int i = 0; i < 3; i++) {
            insert("OffZero-" + i, i);
        }
        PageDTO<Character> pageDTO = new PageDTO<>();
        pageDTO.setOffset(0L);
        pageDTO.setPageSize(2L);
        pageDTO.sortBy("address", true);
        PageDTO<Character> result = repo.findPageByEntity(queryProbe(), pageDTO);
        assertEquals("OffZero-0", result.getRecords().get(0).getAddress());
        assertEquals("OffZero-1", result.getRecords().get(1).getAddress());
    }

    @Test
    void testPageNumberModeRegression() {
        log.info("=== page-number mode still works when offset is not set ===");
        for (int i = 0; i < 6; i++) {
            insert("PageNum-" + i, i);
        }
        PageDTO<Character> pageDTO = new PageDTO<>();
        pageDTO.setCurrentPage(2L);
        pageDTO.setPageSize(2L);
        pageDTO.sortBy("address", true);
        PageDTO<Character> result = repo.findPageByEntity(queryProbe(), pageDTO);
        assertEquals(6, result.getTotal());
        assertEquals(2, result.getRecords().size());
        assertEquals("PageNum-2", result.getRecords().get(0).getAddress());
        assertEquals("PageNum-3", result.getRecords().get(1).getAddress());
    }

    @Test
    void testPageByOffsetWithWrapper() {
        log.info("=== offset mode also works through findPage(wrapper, pageDTO) ===");
        for (int i = 0; i < 4; i++) {
            insert("OffWrap-" + i, i);
        }
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class)
                .eq(Character::getName, commonName);
        PageDTO<Character> pageDTO = new PageDTO<>();
        pageDTO.setOffset(1L);
        pageDTO.setPageSize(2L);
        pageDTO.sortBy("address", true);
        PageDTO<Character> result = repo.findPage(wrapper, pageDTO);
        assertEquals(4, result.getTotal());
        assertEquals("OffWrap-1", result.getRecords().get(0).getAddress());
        assertEquals("OffWrap-2", result.getRecords().get(1).getAddress());
    }

    @Test
    void testSortByExpressionMultiLevel() {
        log.info("=== sortBy(\"address:desc, level:asc\") — multi-level expression sort ===");
        insert("Addr-B", 10);
        insert("Addr-B", 30);
        insert("Addr-A", 20);
        insert("Addr-A", 40);
        insert("Addr-C", 15);
        insert("Addr-C", 25);
        PageDTO<Character> pageDTO = new PageDTO<>();
        pageDTO.setPageSize(3L);
        pageDTO.sortBy("address:desc, level:asc");
        PageDTO<Character> result = repo.findPageByEntity(queryProbe(), pageDTO);
        assertEquals(6, result.getTotal());
        assertEquals(3, result.getRecords().size());
        assertEquals("Addr-C", result.getRecords().get(0).getAddress());
        assertEquals(Integer.valueOf(15), result.getRecords().get(0).getLevel());
        assertEquals("Addr-C", result.getRecords().get(1).getAddress());
        assertEquals(Integer.valueOf(25), result.getRecords().get(1).getLevel());
        assertEquals("Addr-B", result.getRecords().get(2).getAddress());
        assertEquals(Integer.valueOf(10), result.getRecords().get(2).getLevel());
    }

    @Test
    void testSortByLambdaChain() {
        log.info("=== sortBy(addr asc).sortBy(level desc) — chained lambda sort ===");
        insert("Addr-B", 10);
        insert("Addr-B", 30);
        insert("Addr-A", 20);
        insert("Addr-A", 40);
        insert("Addr-C", 15);
        insert("Addr-C", 25);
        PageDTO<Character> pageDTO = new PageDTO<>();
        pageDTO.setPageSize(2L);
        pageDTO.sortBy(Character::getAddress, true).sortBy(Character::getLevel, false);
        PageDTO<Character> result = repo.findPageByEntity(queryProbe(), pageDTO);
        assertEquals(2, result.getRecords().size());
        assertEquals("Addr-A", result.getRecords().get(0).getAddress());
        assertEquals(Integer.valueOf(40), result.getRecords().get(0).getLevel());
        assertEquals("Addr-A", result.getRecords().get(1).getAddress());
        assertEquals(Integer.valueOf(20), result.getRecords().get(1).getLevel());
    }

    @Test
    void testSortByResolvesCollectionField() {
        log.info("=== sortBy(lambda) resolves @CollectionField (weapon → weapon_type) ===");
        Character c = new Character();
        c.setId(Ulid.generate());
        c.setName(commonName);
        c.setAddress("W-Polearm");
        c.setLevel(2);
        c.setWeapon("Polearm");
        repo.insert(c);
        c = new Character();
        c.setId(Ulid.generate());
        c.setName(commonName);
        c.setAddress("W-Sword");
        c.setLevel(3);
        c.setWeapon("Sword");
        repo.insert(c);
        c = new Character();
        c.setId(Ulid.generate());
        c.setName(commonName);
        c.setAddress("W-Catalyst");
        c.setLevel(4);
        c.setWeapon("Catalyst");
        repo.insert(c);

        PageDTO<Character> pageDTO = new PageDTO<>();
        pageDTO.setPageSize(10L);
        pageDTO.sortBy(Character::getWeapon, true);
        PageDTO<Character> result = repo.findPageByEntity(queryProbe(), pageDTO);
        // weapon_type asc: Catalyst < Polearm < Sword / weapon_type 升序：Catalyst < Polearm < Sword
        assertEquals(3, result.getRecords().size());
        assertEquals("Catalyst", result.getRecords().get(0).getWeapon());
        assertEquals("Polearm", result.getRecords().get(1).getWeapon());
        assertEquals("Sword", result.getRecords().get(2).getWeapon());
    }

    @Test
    void testOffsetNegativeRejected() {
        log.info("=== negative offset must be rejected ===");
        insert("Neg-0", 0);
        PageDTO<Character> pageDTO = new PageDTO<>();
        pageDTO.setOffset(-1L);
        assertThrows(IllegalArgumentException.class,
                () -> repo.findPageByEntity(queryProbe(), pageDTO));
    }

    @Test
    void testInvalidPageParamsRejected() {
        log.info("=== currentPage < 1 and pageSize < 1 must be rejected up front ===");
        insert("Invalid-0", 0);

        PageDTO<Character> pageZero = new PageDTO<>();
        pageZero.setCurrentPage(0L);
        pageZero.setPageSize(2L);
        assertThrows(IllegalArgumentException.class,
                () -> repo.findPageByEntity(queryProbe(), pageZero));

        PageDTO<Character> sizeZero = new PageDTO<>();
        sizeZero.setCurrentPage(1L);
        sizeZero.setPageSize(0L);
        assertThrows(IllegalArgumentException.class,
                () -> repo.findPageByEntity(queryProbe(), sizeZero));
    }

    @Test
    void testLightweightPagingNoCount() {
        log.info("=== countTotal=false: no count query, hasNext from extra-document fetch (Slice semantics) ===");
        for (int i = 0; i < 5; i++) {
            insert("Light-" + i, i);
        }
        PageDTO<Character> pageDTO = new PageDTO<>();
        pageDTO.offset(0L);
        pageDTO.setPageSize(2L);
        pageDTO.setCountTotal(false);
        pageDTO.sortBy("address", true);

        PageDTO<Character> page1 = repo.findPageByEntity(queryProbe(), pageDTO);
        // no count → total stays 0, totalPage meaningless / 不查 count → total 保持 0
        assertEquals(0L, page1.getTotal());
        assertEquals(2, page1.getRecords().size());
        assertTrue(page1.getHasNext());   // 2 fetched + 1 extra → hasNext
        assertFalse(page1.isLast());

        PageDTO<Character> lastPage = new PageDTO<>();
        lastPage.offset(4L);
        lastPage.setPageSize(2L);
        lastPage.setCountTotal(false);
        lastPage.sortBy("address", true);
        PageDTO<Character> page3 = repo.findPageByEntity(queryProbe(), lastPage);
        assertEquals(1, page3.getRecords().size());
        assertFalse(page3.getHasNext());  // only 1 fetched, no extra → no next page
        assertTrue(page3.isLast());
    }

    @Test
    void testLightweightPagingPageNumberMode() {
        log.info("=== countTotal=false in page-number mode ===");
        for (int i = 0; i < 5; i++) {
            insert("LightPage-" + i, i);
        }
        PageDTO<Character> pageDTO = new PageDTO<>();
        pageDTO.setCurrentPage(1L);
        pageDTO.setPageSize(2L);
        pageDTO.setCountTotal(false);
        pageDTO.sortBy("address", true);
        PageDTO<Character> page1 = repo.findPageByEntity(queryProbe(), pageDTO);
        assertEquals(2, page1.getRecords().size());
        assertTrue(page1.getHasNext());

        PageDTO<Character> page3Req = new PageDTO<>();
        page3Req.setCurrentPage(3L);
        page3Req.setPageSize(2L);
        page3Req.setCountTotal(false);
        page3Req.sortBy("address", true);
        PageDTO<Character> page3 = repo.findPageByEntity(queryProbe(), page3Req);
        assertEquals(1, page3.getRecords().size());
        assertFalse(page3.getHasNext());
    }

    @Test
    void testNavigationProperties() {
        log.info("=== navigation properties with countTotal=true ===");
        for (int i = 0; i < 6; i++) {
            insert("Nav-" + i, i);
        }
        PageDTO<Character> pageDTO = new PageDTO<>();
        pageDTO.setCurrentPage(1L);
        pageDTO.setPageSize(2L);
        pageDTO.sortBy("address", true);
        PageDTO<Character> page1 = repo.findPageByEntity(queryProbe(), pageDTO);
        assertEquals(6L, page1.getTotal());
        assertTrue(page1.isFirst());
        assertFalse(page1.hasPrevious());
        assertTrue(page1.getHasNext());
        assertFalse(page1.isLast());

        PageDTO<Character> page3Req = new PageDTO<>();
        page3Req.setCurrentPage(3L);
        page3Req.setPageSize(2L);
        page3Req.sortBy("address", true);
        PageDTO<Character> page3 = repo.findPageByEntity(queryProbe(), page3Req);
        assertEquals(2, page3.getRecords().size());
        assertEquals(6L, page3.getTotal());
        assertFalse(page3.isFirst());
        assertTrue(page3.hasPrevious());
        assertFalse(page3.getHasNext());
        assertTrue(page3.isLast());
    }
}
