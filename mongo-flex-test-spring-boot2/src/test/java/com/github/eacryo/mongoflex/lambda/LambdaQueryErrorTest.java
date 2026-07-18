package com.github.eacryo.mongoflex.lambda;

import com.github.eacryo.mongoflex.TestApplication;
import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.query.LambdaQueryWrapper;
import com.github.eacryo.mongoflex.util.SFunction;
import com.github.eacryo.mongoflex.v2.CharacterRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("v2")
public class LambdaQueryErrorTest {

    @Autowired
    private CharacterRepository repo;

    @Test
    void testNullField() {
        log.info("=== test null field -> NPE ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        // Cast disambiguates the SFunction overload from the FieldPath overload / 强制转型以区分 SFunction 与 FieldPath 重载
        assertThrows(NullPointerException.class, () ->
                wrapper.eq((SFunction<Character, String>) null, "value"));
        log.info("correctly threw NullPointerException");
    }

    @Test
    void testNullWrapper() {
        log.info("=== test null wrapper -> NPE ===");
        assertThrows(NullPointerException.class, () ->
                repo.findOne(null));
        log.info("correctly threw NullPointerException");
    }

    @Test
    void testNullWrapperForFindList() {
        log.info("=== test null wrapper findList -> NPE ===");
        assertThrows(NullPointerException.class, () ->
                repo.findList(null));
        log.info("correctly threw NullPointerException");
    }

    @Test
    void testNullWrapperForCount() {
        log.info("=== test null wrapper count -> NPE ===");
        assertThrows(NullPointerException.class, () ->
                repo.count(null));
        log.info("correctly threw NullPointerException");
    }

    @Test
    void testNullWrapperForUpdate() {
        log.info("=== test null wrapper update -> NPE ===");
        assertThrows(NullPointerException.class, () ->
                repo.updateMany(null, new Character()));
        log.info("correctly threw NullPointerException");
    }

    @Test
    void testNullWrapperForDelete() {
        log.info("=== test null wrapper delete -> NPE ===");
        assertThrows(NullPointerException.class, () ->
                repo.deleteMany(null));
        log.info("correctly threw NullPointerException");
    }

    @Test
    void testNullEntityForInsert() {
        log.info("=== test null entity insert -> NPE ===");
        assertThrows(NullPointerException.class, () ->
                repo.insert(null));
        log.info("correctly threw NullPointerException");
    }

    @Test
    void testNullEntityForFindOneByEntity() {
        log.info("=== test null entity findOneByEntity -> NPE ===");
        assertThrows(NullPointerException.class, () ->
                repo.findOneByEntity(null));
        log.info("correctly threw NullPointerException");
    }

    @Test
    void testNullEntityForCountByEntity() {
        log.info("=== test null entity count -> NPE ===");
        assertThrows(NullPointerException.class, () ->
                repo.countByEntity((Character) null));
        log.info("correctly threw NullPointerException");
    }

    @Test
    void testNullEntityForDeleteByEntity() {
        log.info("=== test null entity deleteByEntity -> NPE ===");
        assertThrows(NullPointerException.class, () ->
                repo.deleteByEntity(null));
        log.info("correctly threw NullPointerException");
    }

    @Test
    void testNullEntityForUpdateById() {
        log.info("=== test null entity updateById -> NPE ===");
        assertThrows(NullPointerException.class, () ->
                repo.updateOneById(null));
        log.info("correctly threw NullPointerException");
    }

    @Test
    void testNullIdForFindById() {
        log.info("=== test null id findById -> NPE ===");
        assertThrows(NullPointerException.class, () ->
                repo.findById(null));
        log.info("correctly threw NullPointerException");
    }

    @Test
    void testNullIdForDeleteById() {
        log.info("=== test null id deleteById -> NPE ===");
        assertThrows(NullPointerException.class, () ->
                repo.deleteOneById(null));
        log.info("correctly threw NullPointerException");
    }

    @Test
    void testEmptyWrapperDelete() {
        log.info("=== test empty wrapper delete -> should throw ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                repo.deleteMany(wrapper));
        log.info("correctly threw IllegalArgumentException: {}", ex.getMessage());
        assertTrue(ex.getMessage().contains("deleteAll"));
    }

    @Test
    void testEmptyWrapperUpdate() {
        log.info("=== test empty wrapper update -> should throw ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                repo.updateMany(wrapper, new Character()));
        log.info("correctly threw IllegalArgumentException: {}", ex.getMessage());
        assertTrue(ex.getMessage().contains("requires at least one condition"));
    }

}

