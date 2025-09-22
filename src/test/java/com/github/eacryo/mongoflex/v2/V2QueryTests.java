package com.github.eacryo.mongoflex.v2;

import com.github.eacryo.mongoflex.TestApplication;
import com.github.eacryo.mongoflex.config.MongoFlexProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("v2")
public class V2QueryTests {

    @Autowired
    private MongoFlexProperties mongoFlexProperties;

    @Test
    public void loadContext() {

    }
}
