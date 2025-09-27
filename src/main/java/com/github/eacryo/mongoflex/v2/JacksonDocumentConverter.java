package com.github.eacryo.mongoflex.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.springframework.stereotype.Component;

@Component
public class JacksonDocumentConverter {

    public <T> T convert(Document document,Class<T> clazz) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.addMixIn(clazz, JacksonDocumentMixin.class);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper.convertValue(document, clazz);
    }

    public <T> Document convert(T entity) {
        //TODO：这里会有两个ObjectMapper实例，考虑优化
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.addMixIn(entity.getClass(), JacksonDocumentMixin.class);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            String jsonStr = objectMapper.writeValueAsString(entity);
            return Document.parse(jsonStr);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
