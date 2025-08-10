package com.github.eacryo.mongoflex.testOfDisable;


import com.github.eacryo.mongoflex.TestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("disable-multi-tenants")
public class MyTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    public void testLoad(){
        System.out.println(mongoTemplate.find(new Query(), Object.class, "testBean"));
    }
}
