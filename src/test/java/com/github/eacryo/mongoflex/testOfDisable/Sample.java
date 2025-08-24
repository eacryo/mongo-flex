package com.github.eacryo.mongoflex.testOfDisable;

import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.config.LambdaCriteria;
import com.github.eacryo.mongoflex.config.MongoTemplateFactory;
import com.github.eacryo.mongoflex.config.UpdateBuilder;
import com.github.eacryo.mongoflex.repository.CharacterRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Date;
import java.util.UUID;

import static com.github.eacryo.mongoflex.Constant.COLLECTION_NAME;

public class Sample extends BaseDisableTest {
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private MongoTemplateFactory mongoTemplateFactory;
    @Autowired
    private CharacterRepository characterRepository;

    @Test
    public void testDefaultMongoTemplateUsable() {
        System.out.println(mongoTemplate.find(new Query(), Object.class, "test_connect"));
    }

    @Test
    public void LambdaCriteriaTest() {
        Character character = new Character();
        character.setName("test");
        character.setId(UUID.randomUUID().toString());
        characterRepository.insert(character);
        Query query = new Query();
        query.addCriteria(LambdaCriteria.where(Character::getId).is(character.getId()));
        Character user = mongoTemplateFactory.select().findOne(query, Character.class, COLLECTION_NAME);
        Assertions.assertEquals(character, user);
    }

    @Test
    public void testUpdateBuilder() {
        String id = UUID.randomUUID().toString();
        Character character = new Character();
        character.setName("123");
        character.setId(id);
        characterRepository.insert(character);
        Query query = new Query();
        query.addCriteria(LambdaCriteria.where(Character::getId).is(character.getId()));
        Update update = UpdateBuilder.builder().set(Character::getName, "456").build();
        mongoTemplateFactory.select().updateFirst(query, update, COLLECTION_NAME);
    }

    @Test
    public void testUpdateBuilderFrom() {
        String id = UUID.randomUUID().toString();
        Character character = new Character();
        character.setName("234");
        character.setId(id);
        characterRepository.insert(character);
        Query query = new Query();
        query.addCriteria(LambdaCriteria.where(Character::getId).is(character.getId()));
        character.setName("888");
        character.setAddress("Li Yue");
        character.setCreateAt(new Date());
        Update update = UpdateBuilder.from(character);
        mongoTemplateFactory.select().updateFirst(query, update, COLLECTION_NAME);
    }

    @Test
    public void testAutoGenerateId() {
        Character character = new Character();
        character.setName("Hu Tao");
        characterRepository.insert(character);
    }

    @Test
    public void testManualSetId(){
        Character character = new Character();
        character.setId("specialId");
        character.setName("Hu Tao");
        characterRepository.insert(character);
    }

}
