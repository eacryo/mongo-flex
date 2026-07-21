package com.github.eacryo.mongoflex.bean;

import com.github.eacryo.mongoflex.Constant;
import com.github.eacryo.mongoflex.annotation.CollectionId;
import com.github.eacryo.mongoflex.annotation.CollectionName;
import com.github.eacryo.mongoflex.constant.IdType;
import lombok.Data;
import lombok.ToString;

/**
 * Entity using annotation-driven per-entity ID generator. / 使用注解驱动的按实体 ID 生成器的实体。
 * <p>
 * ID format / ID 格式: "user-{timestamp}" e.g. "user-1734567890123"
 *
 * @see TimestampIdGenerator
 */
@CollectionName(Constant.COLLECTION_NAME)
@Data
@ToString
public class CustomGeneratorEntity {

    @CollectionId(value = IdType.INPUT, generatorClass = TimestampIdGenerator.class)
    private String id;

    private String name;
    private Integer age;

    private Boolean deleted;
}
