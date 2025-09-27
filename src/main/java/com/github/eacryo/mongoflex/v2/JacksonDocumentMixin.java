package com.github.eacryo.mongoflex.v2;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface JacksonDocumentMixin {
    // 告诉 Jackson 在序列化时，将这个方法对应的字段（即 id）重命名为 "_id"
    @JsonProperty("_id")
    String getId();

    @JsonProperty("_id")
    void setId(String _id);
}
