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
        String id = Ulid.generate();
        Character character = new Character();
        character.setId(id);
        character.setName("Furina");
        character.setAddress("Fontaine");
        character.setBirthday(new Date());
        characterRepositoryV2.insert(character);
        // cleanup / 清理
        Character del = new Character();
        del.setId(id);
        del.setDeleted(true);
        characterRepositoryV2.updateOneById(del);
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

    @Test
    public void testUpdateByAnnotation() {
        String id = Ulid.generate();
        String name = "V2UpdateTest-" + id;
        Character c = new Character();
        c.setId(id);
        c.setName(name);
        c.setLevel(1);
        c.setBirthday(new Date());
        System.out.println("before insert: id=" + id + ", name=" + name + ", level=1");
        characterRepositoryV2.insert(c);
        System.out.println("after insert: inserted");

        long modified = characterRepositoryV2.updateLevelByName(name, 60);
        System.out.println("after update: modified=" + modified + ", expected=1");

        Character fetched = characterRepositoryV2.findById(id);
        System.out.println("after update fetched: id=" + fetched.getId() + ", level=" + fetched.getLevel() + ", expected level=60");

        System.out.println("cleanup: deleteById(" + id + ")");
        Character del1 = new Character();
        del1.setId(id);
        del1.setDeleted(true);
        characterRepositoryV2.updateOneById(del1);
    }

    @Test
    public void testUpdateVoid() {
        String id = Ulid.generate();
        String name = "V2VoidUpdate-" + id;
        Character c = new Character();
        c.setId(id);
        c.setName(name);
        c.setBirthday(new Date());
        System.out.println("before insert: id=" + id + ", name=" + name + ", address=null");
        characterRepositoryV2.insert(c);
        System.out.println("after insert: inserted");

        System.out.println("before update: void updateAddressByName to 'V2 Address'");
        characterRepositoryV2.updateAddressByName(name, "V2 Address");
        System.out.println("after update: void return — done");

        Character fetched = characterRepositoryV2.findById(id);
        System.out.println("after update fetched: id=" + fetched.getId() + ", address=" + fetched.getAddress() + ", expected address='V2 Address'");

        System.out.println("cleanup: deleteById(" + id + ")");
        Character del2 = new Character();
        del2.setId(id);
        del2.setDeleted(true);
        characterRepositoryV2.updateOneById(del2);
    }
}
