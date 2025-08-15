package com.github.eacryo.mongoflex.testOfDisable;

import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.config.LambdaCriteria;
import com.github.eacryo.mongoflex.config.MongoTemplateFactory;
import com.github.eacryo.mongoflex.repository.CharacterRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.UUID;

public class Sample extends BaseDisableTest{
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private MongoTemplateFactory mongoTemplateFactory;
    @Autowired
    private CharacterRepository characterRepository;

    @Test
    public void testDefaultMongoTemplateUsable(){
        System.out.println(mongoTemplate.find(new Query(), Object.class, "test_connect"));
    }

    @Test
    public void LambdaCriteriaTest(){
        Character character = new Character();
        character.setName("test");
        character.setId(UUID.randomUUID().toString());
        characterRepository.save(character);
        Query query = new Query();
        query.addCriteria(LambdaCriteria.where(Character::getId).is(character.getId()));
        Character user = mongoTemplateFactory.select().findOne(query, Character.class, "testBean");
        Assertions.assertEquals(character,user);
    }
    
}
