package com.github.eacryo.mongoflex.testOfEnable;

import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.bean.GenshinCharacter;
import com.github.eacryo.mongoflex.config.MongoTemplateFactory;
import com.github.eacryo.mongoflex.constant.MongoFlexConstant;
import com.github.eacryo.mongoflex.repository.CharacterRepository;
import com.github.f4b6a3.ulid.UlidCreator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

@Slf4j
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
        characterRepository.insert(character);
    }

    @Test
    public void testSaveAnotherTenant(){
        MDC.clear();
        MDC.put(MongoFlexConstant.TENANT,"remote");
        Character character = new Character();
        character.setId(UlidCreator.getUlid().toString());
        character.setName("Ganyu_"+System.currentTimeMillis());
        character.setBirthday(new Date());
        characterRepository.insert(character);
        Character byId = characterRepository.findById(character.getId());
        log.info("character:{}",character);
        log.info("byId:{}",byId);
        MDC.clear();
        Assertions.assertEquals(character,byId);
    }

    @Test
    public void testSaveChildClass(){
        GenshinCharacter genshinCharacter = new GenshinCharacter();
        genshinCharacter.setName("Hu Tao");
        genshinCharacter.setElement("Pyro");
        genshinCharacter.setId(UlidCreator.getUlid().toString());
        characterRepository.insert(genshinCharacter);
        Character byId = characterRepository.findById(genshinCharacter.getId());
        System.out.println(byId);
        Assertions.assertEquals(genshinCharacter,byId);
    }


}
