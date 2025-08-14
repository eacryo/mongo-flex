package com.github.eacryo.mongoflex.testOfDisable;

import com.github.eacryo.mongoflex.repository.CharacterRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

public class Sample extends BaseDisableTest{
    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private CharacterRepository characterRepository;

    @Test
    public void testDefaultMongoTemplateUsable(){
        System.out.println(mongoTemplate.find(new Query(), Object.class, "test_connect"));
    }
    
}
