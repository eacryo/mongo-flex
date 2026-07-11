package com.github.eacryo.mongoflex.bean;

import com.github.eacryo.mongoflex.Constant;
import com.github.eacryo.mongoflex.annotation.CollectionId;
import com.github.eacryo.mongoflex.annotation.CollectionName;
import com.github.eacryo.mongoflex.constant.IdType;
import lombok.Data;
import lombok.ToString;

/**
 * 用于验证 {@code IdType.NONE} 下 String 到 ObjectId 的转换逻辑。
 * MongoDB 生成的 ObjectId 在 Java 侧映射为 hex String 字段，查询时需转回 ObjectId。
 */
@CollectionName(Constant.COLLECTION_NAME)
@Data
@ToString
public class ObjectIdEntity {

    @CollectionId(IdType.NONE)
    private String id;

    private String name;
}
