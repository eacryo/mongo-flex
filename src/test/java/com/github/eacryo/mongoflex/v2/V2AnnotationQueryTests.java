package com.github.eacryo.mongoflex.v2;

import com.fasterxml.uuid.Generators;
import com.github.eacryo.mongoflex.TestApplication;
import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.config.MongoFlexProperties;
import com.github.f4b6a3.ulid.UlidCreator;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("v2")
public class V2AnnotationQueryTests {

    @Autowired
    private MongoFlexProperties mongoFlexProperties;

    @Autowired
    private DynamicMongoClient dynamicMongoClient;

    @Autowired
    private CharacterRepositoryV2 characterRepositoryV2;

    @Test
    public void testFindAll() {
        List<Character> characterList = characterRepositoryV2.findAll();
        characterList.forEach(System.out::println);
    }

    @Test
    public void testFindByCriteria() {
        List<Character> characterList = characterRepositoryV2.findListByCriteria("Hu Tao");
        characterList.forEach(System.out::println);
        List<Character> anotherList = characterRepositoryV2.findListByCriteria("Ganyu");
        anotherList.forEach(System.out::println);
        //给一个确定的参数
        characterRepositoryV2.findListWithoutParam().forEach(System.out::println);
    }

    @Test
    public void testFindListByNameAndId() {
        characterRepositoryV2.findListByNameAndId("Hu Tao", "specialId").forEach(System.out::println);
    }

    @Test
    public void testFindByParentMethod() {
        Character character = new Character();
        System.out.println(characterRepositoryV2.findByEntity(character));
    }

    @Test
    public void testFindByParentMethodOfId() {
        System.out.println(characterRepositoryV2.findById("specialId"));
    }

    @Test
    public void testInsert(){
        Character character = new Character();
        character.setId(UlidCreator.getUlid().toString());
        character.setName("Furina");
        character.setAddress("Fontaine");
        characterRepositoryV2.insert(character);
    }

    @Test
    public void testFastXmlUuid(){
        UUID uuidV7 = Generators.timeBasedEpochGenerator().generate();
        System.out.println(uuidV7);
    }

    @Test
    public void testCount(){
        System.out.println(characterRepositoryV2.count());
    }
}
