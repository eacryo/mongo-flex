package com.github.eacryo.mongoflex.v2;

import com.github.eacryo.mongoflex.TestApplication;
import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.convertor.MongoMappingConvertor;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;

@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("v2")
public class MongoMappingConvertorTests {
    @Autowired
    private MongoMappingConvertor mappingConvertor;

    @Test
    public void testConvert() {
        Character character = new Character();
        character.setId("testId");
        character.setName("testName");
        character.setArea("Liyue");
        character.setBirthday(new Date());
        Document document = mappingConvertor.write(character);
        System.out.println(document);
        Character fromDoc = mappingConvertor.read(document, Character.class);
        System.out.println(fromDoc);
    }

}
