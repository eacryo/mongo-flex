package com.github.eacryo.mongoflex.repository;

import com.github.eacryo.mongoflex.TestApplication;
import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.query.LambdaQueryWrapper;
import com.github.eacryo.mongoflex.v2.CharacterRepository;
import com.github.f4b6a3.ulid.UlidCreator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("v2")
public class RepositoryWriteGuardTest {

    @Autowired
    private CharacterRepository repo;

    @Test
    void testInsertNullEntity() {
        log.info("=== test insert(null) ===");
        assertThrows(NullPointerException.class, () -> repo.insert(null));
        log.info("correctly threw NullPointerException");
    }

    @Test
    void testFindByIdNullId() {
        log.info("=== test findById(null) ===");
        assertThrows(NullPointerException.class, () -> repo.findById(null));
        log.info("correctly threw NullPointerException");
    }

    @Test
    void testUpdateByIdNullEntity() {
        log.info("=== test updateById(null) ===");
        assertThrows(NullPointerException.class, () -> repo.updateOneById(null));
        log.info("correctly threw NullPointerException");
    }

    @Test
    void testDeleteByEntityNullEntity() {
        log.info("=== test deleteByEntity(null) ===");
        assertThrows(NullPointerException.class, () -> repo.deleteByEntity(null));
        log.info("correctly threw NullPointerException");
    }

    @Test
    void testDeleteByEntityEmptyEntity() {
        log.info("=== test deleteByEntity(empty entity) ===");
        Character empty = new Character();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                repo.deleteByEntity(empty));
        log.info("correctly threw IllegalArgumentException: {}", ex.getMessage());
        assertTrue(ex.getMessage().contains("deleteAll"));
    }

    @Test
    void testDeleteEmptyWrapper() {
        log.info("=== test delete(empty wrapper) ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                repo.deleteMany(wrapper));
        log.info("correctly threw IllegalArgumentException: {}", ex.getMessage());
        assertTrue(ex.getMessage().contains("deleteAll"));
    }

    @Test
    void testUpdateEmptyWrapper() {
        log.info("=== test update(empty wrapper, entity) ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                repo.updateMany(wrapper, new Character()));
        log.info("correctly threw IllegalArgumentException: {}", ex.getMessage());
        assertTrue(ex.getMessage().contains("requires at least one condition"));
    }

    @Test
    void testUpdateWrapperNullEntity() {
        log.info("=== test update(wrapper, null entity) ===");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
        wrapper.eq(Character::getName, "test");
        assertThrows(NullPointerException.class, () -> repo.updateMany(wrapper, null));
        log.info("correctly threw NullPointerException");
    }

    @Test
    void testDeleteWrapperNonNull() {
        log.info("=== test delete(non-empty wrapper) should work ===");
        String id = UlidCreator.getUlid().toString();
        try {
            Character c = new Character();
            c.setId(id);
            c.setName("WriteGuard-" + id);
            c.setBirthday(new Date());
            repo.insert(c);

            LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
            wrapper.eq(Character::getName, "WriteGuard-" + id);
            long deleted = repo.deleteMany(wrapper);
            log.info("delete result: {}", deleted);
            assertEquals(1, deleted);
        } finally {
            repo.deleteOneById(id);
        }
    }

}

