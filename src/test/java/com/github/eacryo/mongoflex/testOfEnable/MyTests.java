package com.github.eacryo.mongoflex.testOfEnable;

import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.bean.GenshinCharacter;
import com.github.eacryo.mongoflex.config.LambdaCriteria;
import com.github.eacryo.mongoflex.repository.CharacterRepository;
import com.github.f4b6a3.ulid.Ulid;
import com.github.f4b6a3.ulid.UlidCreator;
import com.github.f4b6a3.ulid.UlidFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Date;


public class MyTests extends BaseMultiTenantsTest{

    @Autowired
    private CharacterRepository characterRepository;

    @Test
    public void testLoad(){

    }

    @Test
    public void testSave(){
        Character character = new Character();
        character.setName("test");
        character.setBirthday(new Date());
        characterRepository.save(character);
    }

    @Test
    public void testSaveChildClass(){
        GenshinCharacter genshinCharacter = new GenshinCharacter();
        genshinCharacter.setName("Hu Tao");
        genshinCharacter.setElement("Pyro");
        genshinCharacter.setId(UlidCreator.getUlid().toString());
        characterRepository.save(genshinCharacter);
        Character byId = characterRepository.findById(genshinCharacter.getId());
        System.out.println(byId);
        Assertions.assertEquals(genshinCharacter,byId);
    }


}
