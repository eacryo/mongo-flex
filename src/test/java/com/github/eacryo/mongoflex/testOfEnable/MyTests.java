package com.github.eacryo.mongoflex.testOfEnable;

import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.repository.CharacterRepository;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;


public class MyTests extends BaseMultiTenantsTest{

    @Autowired
    private CharacterRepository characterRepository;

    @Test
    public void testLoad(){

    }

    @Test
    public void testSave(){
        MDC.put("tenant","testTenant");
        Character character = new Character();
        character.setName("test");
        character.setBirthday(new Date());
        characterRepository.save(character);
    }


}
