package com.github.eacryo.mongoflex.v2;

import com.github.eacryo.mongoflex.TestApplication;
import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.lambda.LambdaQueryWrapper;
import com.github.f4b6a3.ulid.UlidCreator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;
import java.util.List;

@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("v2")
public class LambdaQueryWrapperTest {

    @Autowired
    private CharacterRepository characterRepositoryV2;

    @Test
    public void testFindOneByWrapper() {
        Character c = new Character();
        String id = UlidCreator.getUlid().toString();
        c.setId(id);
        c.setName("FindOneWrapper-" + id);
        c.setArea("Inazuma");
        c.setBirthday(new Date());
        characterRepositoryV2.insert(c);

        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Character::getName, c.getName());
        Character result = characterRepositoryV2.findOne(wrapper);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(c.getName(), result.getName());
        System.out.println("findOne(wrapper): " + result);

        characterRepositoryV2.deleteById(id);
    }

    @Test
    public void testFindListByWrapper() {
        Character c1 = new Character();
        String id1 = UlidCreator.getUlid().toString();
        c1.setId(id1);
        c1.setName("FindListWrapper-A-" + id1);
        c1.setArea("Liyue");
        c1.setBirthday(new Date());
        characterRepositoryV2.insert(c1);

        Character c2 = new Character();
        String id2 = UlidCreator.getUlid().toString();
        c2.setId(id2);
        c2.setName("FindListWrapper-B-" + id2);
        c2.setArea("Liyue");
        c2.setBirthday(new Date());
        characterRepositoryV2.insert(c2);

        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Character::getArea, "Liyue")
               .ne(Character::getName, c1.getName());
        List<Character> list = characterRepositoryV2.findList(wrapper);
        Assertions.assertNotNull(list);
        Assertions.assertFalse(list.isEmpty());
        System.out.println("findList(wrapper) size: " + list.size());

        characterRepositoryV2.deleteById(id1);
        characterRepositoryV2.deleteById(id2);
    }

    @Test
    public void testCountByWrapper() {
        Character c = new Character();
        String id = UlidCreator.getUlid().toString();
        c.setId(id);
        c.setName("CountWrapper-" + id);
        c.setArea("Sumeru");
        c.setBirthday(new Date());
        characterRepositoryV2.insert(c);

        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Character::getName, c.getName());
        long count = characterRepositoryV2.count(wrapper);
        Assertions.assertTrue(count >= 1);
        System.out.println("count(wrapper): " + count);

        characterRepositoryV2.deleteById(id);
    }

    @Test
    public void testUpdateByWrapper() {
        Character c = new Character();
        String id = UlidCreator.getUlid().toString();
        c.setId(id);
        c.setName("UpdateWrapper-" + id);
        c.setAddress("Fontaine");
        c.setBirthday(new Date());
        characterRepositoryV2.insert(c);

        c.setAddress("Snezhnaya");
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Character::getName, c.getName());
        long updated = characterRepositoryV2.update(wrapper, c);
        Assertions.assertTrue(updated > 0);
        System.out.println("update(wrapper) result: " + updated);

        characterRepositoryV2.deleteById(id);
    }

    @Test
    public void testDeleteByWrapper() {
        Character c = new Character();
        String id = UlidCreator.getUlid().toString();
        c.setId(id);
        c.setName("DeleteWrapper-" + id);
        c.setBirthday(new Date());
        c.setArea("Natlan");
        characterRepositoryV2.insert(c);

        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Character::getName, c.getName());
        long deleted = characterRepositoryV2.delete(wrapper);
        Assertions.assertTrue(deleted > 0);
        System.out.println("delete(wrapper) result: " + deleted);
    }

}
