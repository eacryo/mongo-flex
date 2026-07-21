package com.github.eacryo.mongoflex.v2;

import com.github.eacryo.mongoflex.TestApplication;
import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.bean.ObjectIdEntity;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 ID 类型转换逻辑仅对 {@code IdType.OBJECT_ID} 生效，
 * ULID / UUID / INPUT 类型的 String ID 不会被误转为 ObjectId。
 */
@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("v2")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ObjectIdConversionTest {

    @Autowired
    private ObjectIdEntityRepository noneRepo;

    @Autowired
    private CharacterRepository characterRepository;

    private static String noneId;

    // ──── IdType.OBJECT_ID: 应该转换 ────

    @Test
    @Order(1)
    void testNoneInsertGeneratesObjectId() {
        ObjectIdEntity entity = new ObjectIdEntity();
        entity.setName("test-none");

        noneRepo.insert(entity);

        // MongoDB 自动生成了 ObjectId，Java 侧拿到 hex String
        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getId()).hasSize(24);
        assertThat(ObjectId.isValid(entity.getId())).isTrue();

        noneId = entity.getId();
        System.out.println("OBJECT_ID id = " + noneId);
    }

    @Test
    @Order(2)
    void testNoneFindById() {
        assertThat(noneId).isNotNull();

        // 查询时 String → ObjectId 转换生效
        ObjectIdEntity found = noneRepo.findById(noneId);
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("test-none");
    }

    @Test
    @Order(3)
    void testNoneDeleteById() {
        assertThat(noneId).isNotNull();

        ObjectIdEntity del = new ObjectIdEntity();
        del.setId(noneId);
        del.setDeleted(true);
        long deleted = noneRepo.updateOneById(del);
        assertThat(deleted).isEqualTo(1);

        assertThat(noneRepo.findById(noneId)).isNull();
    }

    // ──── IdType.ULID: 不应该转换 ────

    @Test
    @Order(4)
    void testUlidInsertAndFind() {
        // 使用现有 Character 实体（IdType.ULID），验证正常插入和查询
        Character character = new Character();
        character.setName("test-ulid-no-convert");

        characterRepository.insert(character);

        assertThat(character.getId()).isNotNull();
        assertThat(character.getId()).hasSize(26); // ULID length

        Character found = characterRepository.findById(character.getId());
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("test-ulid-no-convert");

        // 清理
        Character del = new Character();
        del.setId(character.getId());
        del.setDeleted(true);
        characterRepository.updateOneById(del);
    }
}
