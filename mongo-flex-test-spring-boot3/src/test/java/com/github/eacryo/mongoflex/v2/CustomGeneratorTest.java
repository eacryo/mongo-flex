package com.github.eacryo.mongoflex.v2;

import com.github.eacryo.mongoflex.TestApplication;
import com.github.eacryo.mongoflex.bean.CustomGeneratorEntity;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verify per-entity custom ID generator via {@code @CollectionId(generatorClass = ...)}. / 验证通过 {@code @CollectionId(generatorClass = ...)} 指定的按实体自定义 ID 生成器。
 * <p>
 * The {@link com.github.eacryo.mongoflex.bean.TimestampIdGenerator} produces IDs in the format
 * "user-{timestamp}". Tests cover single insert, batch insert, and ID non-overwrite behavior. / {@link com.github.eacryo.mongoflex.bean.TimestampIdGenerator} 生成 "user-{timestamp}" 格式的 ID，测试覆盖单条插入、批量插入、以及 ID 不覆盖行为。
 */
@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("v2")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CustomGeneratorTest {

    @Autowired
    private CustomGeneratorEntityRepository repository;

    private static String singleId;
    private static List<String> batchIds;

    // ---- single insert / 单条插入 ----

    @Test
    @Order(1)
    void testSingleInsertGeneratesTimestampId() {
        CustomGeneratorEntity entity = new CustomGeneratorEntity();
        entity.setName("test-single");
        entity.setAge(25);

        repository.insert(entity);

        // ID should be auto-generated in "user-{timestamp}-{seq}" format / ID 应为 "user-{timestamp}-{seq}" 格式
        String id = entity.getId();
        assertThat(id).isNotNull();
        assertThat(id).startsWith("user-");
        assertThat(id.length()).isGreaterThan("user-".length());

        // Parse timestamp and sequence: "user-1734567890123-1" / 解析时间戳和序列号
        String[] parts = id.split("-");
        assertThat(parts).hasSize(3); // user, timestamp, seq
        long ts = Long.parseLong(parts[1]);
        long seq = Long.parseLong(parts[2]);
        assertThat(ts).isPositive();
        assertThat(seq).isGreaterThanOrEqualTo(1);
        System.out.println("Generated ID: " + id + " | timestamp: " + ts + " | seq: " + seq);

        singleId = id;
    }

    @Test
    @Order(2)
    void testFindByIdAfterSingleInsert() {
        assertThat(singleId).isNotNull();

        CustomGeneratorEntity found = repository.findById(singleId);

        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("test-single");
        assertThat(found.getAge()).isEqualTo(25);
        assertThat(found.getId()).isEqualTo(singleId);
    }

    // ---- ID non-overwrite: if already set, generator is skipped / ID 不覆盖：已设置则跳过生成器 ----

    @Test
    @Order(3)
    void testInsertDoesNotOverwriteExistingId() {
        CustomGeneratorEntity entity = new CustomGeneratorEntity();
        entity.setId("my-custom-id-12345");
        entity.setName("test-custom-id");
        entity.setAge(30);

        repository.insert(entity);

        // Should keep the manually set ID / 应保留手动设置的 ID
        assertThat(entity.getId()).isEqualTo("my-custom-id-12345");
        System.out.println("Preserved custom ID: " + entity.getId());

        // Verify in DB / 验证数据库
        CustomGeneratorEntity found = repository.findById("my-custom-id-12345");
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("test-custom-id");

        // Cleanup / 清理
        repository.deleteById("my-custom-id-12345");
    }

    // ---- batch insert many / 批量插入 ----

    @Test
    @Order(4)
    void testInsertManyGeneratesUniqueIds() {
        CustomGeneratorEntity e1 = new CustomGeneratorEntity();
        e1.setName("batch-1");
        e1.setAge(10);

        CustomGeneratorEntity e2 = new CustomGeneratorEntity();
        e2.setName("batch-2");
        e2.setAge(20);

        CustomGeneratorEntity e3 = new CustomGeneratorEntity();
        e3.setName("batch-3");
        e3.setAge(30);

        List<CustomGeneratorEntity> inserted = repository.insertMany(Arrays.asList(e1, e2, e3));

        assertThat(inserted).hasSize(3);

        // All IDs should be unique and follow the pattern / 所有 ID 应唯一且符合格式
        for (CustomGeneratorEntity entity : inserted) {
            assertThat(entity.getId()).isNotNull();
            assertThat(entity.getId()).startsWith("user-");
            System.out.println("InsertMany ID: " + entity.getId() + " | name: " + entity.getName());
        }

        // Verify uniqueness / 验证唯一性
        long distinctCount = inserted.stream().map(CustomGeneratorEntity::getId).distinct().count();
        assertThat(distinctCount).isEqualTo(3);

        batchIds = Arrays.asList(
                inserted.get(0).getId(),
                inserted.get(1).getId(),
                inserted.get(2).getId()
        );
    }

    @Test
    @Order(5)
    void testFindAllAfterBatchInsert() {
        assertThat(batchIds).isNotNull().hasSize(3);

        for (String id : batchIds) {
            CustomGeneratorEntity found = repository.findById(id);
            assertThat(found).isNotNull();
            assertThat(found.getId()).isEqualTo(id);
        }
    }

    @Test
    @Order(6)
    void testUpdateByIdWithGeneratorEntity() {
        assertThat(batchIds).isNotNull().isNotEmpty();
        String id = batchIds.get(0);

        CustomGeneratorEntity entity = new CustomGeneratorEntity();
        entity.setId(id);
        entity.setName("batch-1-updated");
        entity.setAge(99);

        long modified = repository.updateOneById(entity);
        assertThat(modified).isEqualTo(1);

        CustomGeneratorEntity found = repository.findById(id);
        assertThat(found.getName()).isEqualTo("batch-1-updated");
        assertThat(found.getAge()).isEqualTo(99);
    }

    @Test
    @Order(7)
    void testDeleteAllAfterBatchInsert() {
        assertThat(batchIds).isNotNull().hasSize(3);

        for (String id : batchIds) {
            long deleted = repository.deleteById(id);
            assertThat(deleted).isEqualTo(1);
        }

        // All should be gone / 全部应已删除
        for (String id : batchIds) {
            assertThat(repository.findById(id)).isNull();
        }
    }

    // ---- cleanup single insert / 清理单条插入 ----

    @Test
    @Order(8)
    void testCleanupSingleInsert() {
        assertThat(singleId).isNotNull();

        long deleted = repository.deleteById(singleId);
        assertThat(deleted).isEqualTo(1);
        assertThat(repository.findById(singleId)).isNull();
    }
}
