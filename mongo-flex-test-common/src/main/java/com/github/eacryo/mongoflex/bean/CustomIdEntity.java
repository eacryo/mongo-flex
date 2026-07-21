package com.github.eacryo.mongoflex.bean;

import com.github.eacryo.mongoflex.Constant;
import com.github.eacryo.mongoflex.annotation.CollectionId;
import com.github.eacryo.mongoflex.annotation.CollectionName;
import com.github.eacryo.mongoflex.constant.IdType;
import lombok.Data;
import lombok.ToString;

/**
 * 用于验证 {@code @CollectionId} 在非 "id" 命名字段上也能正确映射到 {@code _id}。
 */
@CollectionName(Constant.COLLECTION_NAME)
@Data
@ToString
public class CustomIdEntity {

    @CollectionId(IdType.ULID)
    private String userId;   // 字段名不是 "id"，验证 @CollectionId 强制 _id 映射

    private String name;
    private Integer age;

    private Boolean deleted;
}
