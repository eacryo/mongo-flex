package com.github.eacryo.mongoflex.bean;

import com.github.eacryo.mongoflex.Constant;
import com.github.eacryo.mongoflex.annotation.CollectionId;
import com.github.eacryo.mongoflex.annotation.CollectionName;
import com.github.eacryo.mongoflex.constant.IdType;
import lombok.Data;
import lombok.ToString;

@CollectionName(Constant.COLLECTION_NAME)
@Data
@ToString
public class IdGeneratorTestBean {
    @CollectionId(IdType.INPUT)
    private String id;
    private String text;
}
