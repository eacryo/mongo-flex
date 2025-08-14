package com.github.eacryo.mongoflex.testOfDisable;

import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.config.MongoTemplateFactory;
import com.github.eacryo.mongoflex.repository.CharacterRepository;
import com.github.f4b6a3.ulid.UlidCreator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Date;

public class RepositoryTest extends BaseDisableTest{

    @Autowired
    private CharacterRepository characterRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoTemplateFactory mongoTemplateFactory;

    @Test
    public void testLoad(){

    }

    @Test
    public void testSaveAndFindById(){
        Character character = new Character();
        String ulid = UlidCreator.getUlid().toString();
        character.setId(ulid);
        character.setName("Ukinami Yuzuha");
        character.setBirthday(new Date());
        characterRepository.save(character);
        Character byId = characterRepository.findById(ulid);
        System.out.println(byId);
        Assertions.assertNotNull(byId);
        Assertions.assertEquals(character, byId);
        Assertions.assertEquals(mongoTemplate,mongoTemplateFactory.select());
    }
}
