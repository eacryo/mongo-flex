package com.github.eacryo.mongoflex.v2;

import com.github.eacryo.mongoflex.bean.CustomGeneratorEntity;

/**
 * Repository for {@link CustomGeneratorEntity}, which uses annotation-driven per-entity ID generator. / {@link CustomGeneratorEntity} 的 Repository，使用注解驱动的按实体 ID 生成器。
 */
@MRepository
public interface CustomGeneratorEntityRepository extends MongoRepository<CustomGeneratorEntity, String> {
}
