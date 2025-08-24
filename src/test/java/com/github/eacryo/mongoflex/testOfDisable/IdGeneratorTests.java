package com.github.eacryo.mongoflex.testOfDisable;

import com.github.eacryo.mongoflex.bean.IdGeneratorTestBean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

public class IdGeneratorTests extends BaseDisableTest {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    public void testGenerateIdByInput() {
        IdGeneratorTestBean bean = new IdGeneratorTestBean();
        bean.setText("this is a text");
        Assertions.assertTrue(mongoTemplate.insert(bean).getId().endsWith("_INPUT"));
    }
}
