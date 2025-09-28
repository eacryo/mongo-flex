package com.github.eacryo.mongoflex.v2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.springframework.stereotype.Component;

@Component
public class JacksonDocumentConverter {

    private ObjectMapper objectMapper;

    private ObjectMapper getObjectMapper(Class<?> clazz) {
        if (this.objectMapper == null) {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.setAnnotationIntrospector(new FieldMappingIntrospector());
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            this.objectMapper = objectMapper;
        }
        this.objectMapper.addMixIn(clazz, JacksonDocumentMixin.class);
        return objectMapper;
    }


    public <T> T convert(Document document, Class<T> clazz) {
        ObjectMapper objectMapper = getObjectMapper(clazz);
        return objectMapper.convertValue(document, clazz);
    }

    public <T> Document convert(T entity) {
        ObjectMapper objectMapper = getObjectMapper(entity.getClass());
        try {
            String jsonStr = objectMapper.writeValueAsString(entity);
            return Document.parse(jsonStr);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
