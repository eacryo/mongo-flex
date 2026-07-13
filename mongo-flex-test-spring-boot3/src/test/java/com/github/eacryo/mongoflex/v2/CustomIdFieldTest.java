package com.github.eacryo.mongoflex.v2;

import com.github.eacryo.mongoflex.TestApplication;
import com.github.eacryo.mongoflex.bean.CustomIdEntity;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 {@code @CollectionId} 在非 "id" 命名字段上的行为与 Spring Data MongoDB 一致：
 * 标注了 @CollectionId 的字段强制映射到 {@code _id}。
 */
@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("v2")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CustomIdFieldTest {

    @Autowired
    private CustomIdEntityRepository repository;

    private static String savedId;

    @Test
    @Order(1)
    void testInsertGeneratesUlid() {
        CustomIdEntity entity = new CustomIdEntity();
        entity.setName("alice");
        entity.setAge(30);

        repository.insert(entity);

        // insert 后 userId 应被 ULID 填充
        assertThat(entity.getUserId()).isNotNull();
        assertThat(entity.getUserId()).hasSize(26); // ULID length

        savedId = entity.getUserId();
        System.out.println("Inserted entity with userId = " + savedId);
    }

    @Test
    @Order(2)
    void testFindById() {
        assertThat(savedId).isNotNull();

        CustomIdEntity found = repository.findById(savedId);

        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("alice");
        assertThat(found.getAge()).isEqualTo(30);
        assertThat(found.getUserId()).isEqualTo(savedId);
    }

    @Test
    @Order(3)
    void testUpdateById() {
        assertThat(savedId).isNotNull();

        CustomIdEntity entity = new CustomIdEntity();
        entity.setUserId(savedId);
        entity.setName("alice-updated");
        entity.setAge(31);

        long modified = repository.updateOneById(entity);

        assertThat(modified).isEqualTo(1);

        CustomIdEntity found = repository.findById(savedId);
        assertThat(found.getName()).isEqualTo("alice-updated");
        assertThat(found.getAge()).isEqualTo(31);
    }

    @Test
    @Order(4)
    void testFindOneByEntity() {
        assertThat(savedId).isNotNull();

        CustomIdEntity probe = new CustomIdEntity();
        probe.setUserId(savedId);

        CustomIdEntity found = repository.findOneByEntity(probe);

        assertThat(found).isNotNull();
        assertThat(found.getUserId()).isEqualTo(savedId);
    }

    @Test
    @Order(5)
    void testDeleteById() {
        assertThat(savedId).isNotNull();

        long deleted = repository.deleteById(savedId);
        assertThat(deleted).isEqualTo(1);

        CustomIdEntity found = repository.findById(savedId);
        assertThat(found).isNull();
    }
}
