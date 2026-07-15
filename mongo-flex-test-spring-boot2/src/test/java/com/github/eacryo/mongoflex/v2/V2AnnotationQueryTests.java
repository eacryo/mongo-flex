package com.github.eacryo.mongoflex.v2;

import com.fasterxml.uuid.Generators;
import com.github.eacryo.mongoflex.TestApplication;
import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.config.MongoFlexProperties;
import com.github.eacryo.mongoflex.repository.DynamicMongoClient;
import com.github.eacryo.mongoflex.ulid.Ulid;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.text.ParseException;
import java.util.Date;
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
    private CharacterRepository characterRepositoryV2;

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
        // FIXME: hardcoded data dependency — findListWithoutParam relies on pre-existing 'Hu Tao' document
        // characterRepositoryV2.findListWithoutParam().forEach(System.out::println);
    }

    @Test
    public void testFindListByNameAndId() {
        characterRepositoryV2.findListByNameAndId("Hu Tao", "specialId").forEach(System.out::println);
    }

    @Test
    public void testFindByParentMethod() {
        Character character = new Character();
        System.out.println(characterRepositoryV2.findOneByEntity(character));
    }

    @Test
    public void testFindByParentMethodOfId() {
        System.out.println(characterRepositoryV2.findById("specialId"));
    }

    @Test
    public void testFindOneByEntry() {
        // 构造查询实体，参考 repository 中的 findOneByMql 所使用的 _id 和 name 字段
        Character query = new Character();
        query.setName("Ganyu");
        System.out.println(characterRepositoryV2.findOneByEntity(query));
    }

    @Test
    public void testInsert() throws ParseException {
        Character character = new Character();
        character.setId(Ulid.generate());
        character.setName("Furina");
        character.setAddress("Fontaine");
        character.setBirthday(new Date());
        characterRepositoryV2.insert(character);
    }

    @Test
    public void testFastXmlUuid(){
        UUID uuidV7 = Generators.timeBasedEpochGenerator().generate();
        System.out.println(uuidV7);
    }

    @Test
    public void testCount(){
        System.out.println(characterRepositoryV2.countByMql());
    }

    @Test
    public void testCount_2(){
        System.out.println(characterRepositoryV2.count());
        Character character = new Character();
        character.setName("Hu Tao");
        System.out.println(characterRepositoryV2.countByEntity(character));
    }

    @Test
    public void testFindById(){
        System.out.println(characterRepositoryV2.findById("specialId"));
    }
}
