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

    @Test
    public void testOrSimple() {
        String id1 = UlidCreator.getUlid().toString();
        Character c1 = new Character();
        c1.setId(id1);
        c1.setName("OrSimple-A-" + id1);
        c1.setBirthday(new Date());
        c1.setArea("Inazuma");
        characterRepositoryV2.insert(c1);

        String id2 = UlidCreator.getUlid().toString();
        Character c2 = new Character();
        c2.setId(id2);
        c2.setName("OrSimple-B-" + id2);
        c2.setBirthday(new Date());
        c2.setArea("Inazuma");
        characterRepositoryV2.insert(c2);

        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Character::getName, c1.getName()).or().eq(Character::getName, c2.getName());
        List<Character> list = characterRepositoryV2.findList(wrapper);
        Assertions.assertNotNull(list);
        Assertions.assertTrue(list.size() >= 2);
        System.out.println("orSimple size: " + list.size());

        characterRepositoryV2.deleteById(id1);
        characterRepositoryV2.deleteById(id2);
    }

    @Test
    public void testOrChained() {
        String id1 = UlidCreator.getUlid().toString();
        Character c1 = new Character();
        c1.setId(id1);
        c1.setName("OrChain-A-" + id1);
        c1.setBirthday(new Date());
        c1.setArea("Inazuma");
        characterRepositoryV2.insert(c1);

        String id2 = UlidCreator.getUlid().toString();
        Character c2 = new Character();
        c2.setId(id2);
        c2.setName("OrChain-B-" + id2);
        c2.setBirthday(new Date());
        c2.setArea("Inazuma");
        characterRepositoryV2.insert(c2);

        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Character::getArea, "Inazuma")
               .eq(Character::getName, c1.getName())
               .or()
               .eq(Character::getArea, "Inazuma")
               .eq(Character::getName, c2.getName());
        List<Character> list = characterRepositoryV2.findList(wrapper);
        Assertions.assertNotNull(list);
        Assertions.assertTrue(list.size() >= 1);
        System.out.println("orChained: found " + list.size() + " matching either group");

        characterRepositoryV2.deleteById(id1);
        characterRepositoryV2.deleteById(id2);
    }

    @Test
    public void testOrWithNestedWrapper() {
        String idA = UlidCreator.getUlid().toString();
        Character cA = new Character();
        cA.setId(idA);
        cA.setName("OrNested-A-" + idA);
        cA.setArea("Liyue");
        cA.setBirthday(new Date());
        characterRepositoryV2.insert(cA);

        String idB = UlidCreator.getUlid().toString();
        Character cB = new Character();
        cB.setId(idB);
        cB.setName("OrNested-B-" + idB);
        cB.setArea("Mondstadt");
        cB.setBirthday(new Date());
        characterRepositoryV2.insert(cB);

        LambdaQueryWrapper<Character> orPart = new LambdaQueryWrapper<>();
        orPart.eq(Character::getName, cB.getName())
              .eq(Character::getArea, "Mondstadt");

        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Character::getName, cA.getName())
               .or(orPart);

        List<Character> list = characterRepositoryV2.findList(wrapper);
        Assertions.assertNotNull(list);
        Assertions.assertTrue(list.size() >= 1);
        System.out.println("orNestedWrapper: found " + list.size());

        characterRepositoryV2.deleteById(idA);
        characterRepositoryV2.deleteById(idB);
    }

    @Test
    public void testBackwardCompatiblePureAnd() {
        String id = UlidCreator.getUlid().toString();
        Character c = new Character();
        c.setId(id);
        c.setName("PureAnd-" + id);
        c.setArea("Sumeru");
        c.setBirthday(new Date());
        characterRepositoryV2.insert(c);

        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Character::getName, c.getName())
               .eq(Character::getArea, "Sumeru");
        List<Character> list = characterRepositoryV2.findList(wrapper);
        Assertions.assertNotNull(list);
        Assertions.assertEquals(1, list.size());
        System.out.println("pureAnd: " + list.get(0).getName());

        characterRepositoryV2.deleteById(id);
    }

    @Test
    public void testIncludeProjection() {
        // insert
        Character c = new Character();
        String id = UlidCreator.getUlid().toString();
        c.setId(id);
        c.setName("SelectProj-" + id);
        c.setArea("Fontaine");
        c.setBirthday(new Date());
        c.setDescription("projection test description");
        characterRepositoryV2.insert(c);

        // query with include: only name and area
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Character::getName, c.getName())
               .include(Character::getName, Character::getArea);
        List<Character> list = characterRepositoryV2.findList(wrapper);
        Assertions.assertNotNull(list);
        Assertions.assertEquals(1, list.size());

        Character result = list.get(0);
        System.out.println("include projection: name=" + result.getName()
                + ", area=" + result.getArea()
                + ", desc=" + result.getDescription()
                + ", id=" + result.getId());

        // included fields should be populated
        Assertions.assertEquals(c.getName(), result.getName());
        Assertions.assertEquals("Fontaine", result.getArea());
        // non-included fields should be null (not returned from MongoDB)
        Assertions.assertNull(result.getDescription());
        // _id is returned by default per MongoDB behavior
        Assertions.assertEquals(id, result.getId());

        characterRepositoryV2.deleteById(id);
    }

    @Test
    public void testIncludeProjectionFindOne() {
        Character c = new Character();
        String id = UlidCreator.getUlid().toString();
        c.setId(id);
        c.setName("SelectOne-" + id);
        c.setArea("Natlan");
        c.setBirthday(new Date());
        c.setDescription("findOne projection test");
        characterRepositoryV2.insert(c);

        // findOne with include
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Character::getName, c.getName())
               .include(Character::getName, Character::getBirthday);
        Character result = characterRepositoryV2.findOne(wrapper);
        System.out.println("include findOne: name=" + result.getName()
                + ", birthday=" + result.getBirthday()
                + ", area=" + result.getArea()
                + ", desc=" + result.getDescription());

        Assertions.assertNotNull(result);
        Assertions.assertEquals(c.getName(), result.getName());
        Assertions.assertNotNull(result.getBirthday());
        Assertions.assertNull(result.getDescription()); // not included
        Assertions.assertNull(result.getArea());         // not included

        characterRepositoryV2.deleteById(id);
    }

    @Test
    public void testFromEntity() {
        // insert test data
        Character c = new Character();
        String id = UlidCreator.getUlid().toString();
        c.setId(id);
        c.setName("FromEntity-" + id);
        c.setArea("Sumeru");
        c.setBirthday(new Date());
        c.setDescription("fromEntity test");
        characterRepositoryV2.insert(c);

        // fromEntity: non-null fields become eq() conditions
        Character probe = new Character();
        probe.setName(c.getName());
        probe.setArea("Sumeru");
        // birthday and description are null — automatically excluded

        LambdaQueryWrapper<Character> wrapper = LambdaQueryWrapper.fromEntity(probe);
        List<Character> list = characterRepositoryV2.findList(wrapper);
        System.out.println("fromEntity: found " + list.size()
                + " results for name=" + c.getName());
        for (Character ch : list) {
            System.out.println("  result: name=" + ch.getName()
                    + ", area=" + ch.getArea()
                    + ", desc=" + ch.getDescription());
        }

        Assertions.assertNotNull(list);
        Assertions.assertTrue(list.size() >= 1);
        for (Character ch : list) {
            Assertions.assertEquals(c.getName(), ch.getName());
            Assertions.assertEquals("Sumeru", ch.getArea());
        }

        characterRepositoryV2.deleteById(id);
    }

    @Test
    public void testFromEntityWithIncludeAndSort() {
        // insert test data
        String id1 = UlidCreator.getUlid().toString();
        Character c1 = new Character();
        c1.setId(id1);
        c1.setName("FE-Proj-A-" + id1);
        c1.setArea("Natlan");
        c1.setBirthday(new Date());
        c1.setDescription("desc A");
        characterRepositoryV2.insert(c1);

        String id2 = UlidCreator.getUlid().toString();
        Character c2 = new Character();
        c2.setId(id2);
        c2.setName("FE-Proj-B-" + id2);
        c2.setArea("Natlan");
        c2.setBirthday(new Date(System.currentTimeMillis() + 86400000L));
        c2.setDescription("desc B");
        characterRepositoryV2.insert(c2);

        // fromEntity + include + sort: entity 桥接到 LambdaQueryWrapper 后自由组合
        Character probe = new Character();
        probe.setArea("Natlan");

        List<Character> list = characterRepositoryV2.findList(
            LambdaQueryWrapper.fromEntity(probe)
                .include(Character::getName, Character::getArea)
                .orderByAsc(Character::getName)
        );

        System.out.println("fromEntity+include+sort: found " + list.size() + " results");
        for (Character ch : list) {
            System.out.println("  result: name=" + ch.getName()
                    + ", area=" + ch.getArea()
                    + ", desc=" + ch.getDescription());
        }

        Assertions.assertNotNull(list);
        Assertions.assertTrue(list.size() >= 2);
        for (Character ch : list) {
            Assertions.assertNotNull(ch.getName());
            Assertions.assertEquals("Natlan", ch.getArea());
            Assertions.assertNull(ch.getDescription()); // not included
        }

        characterRepositoryV2.deleteById(id1);
        characterRepositoryV2.deleteById(id2);
    }

    @Test
    public void testIncludeProjectionPagination() {
        // insert 2 records
        String id1 = UlidCreator.getUlid().toString();
        Character c1 = new Character();
        c1.setId(id1);
        c1.setName("PageProj-A-" + id1);
        c1.setArea("Inazuma");
        c1.setBirthday(new Date());
        c1.setDescription("page projection A");
        characterRepositoryV2.insert(c1);

        String id2 = UlidCreator.getUlid().toString();
        Character c2 = new Character();
        c2.setId(id2);
        c2.setName("PageProj-B-" + id2);
        c2.setArea("Inazuma");
        c2.setBirthday(new Date());
        c2.setDescription("page projection B");
        characterRepositoryV2.insert(c2);

        // paginated query with include
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Character::getArea, "Inazuma")
               .include(Character::getName, Character::getArea);

        com.github.eacryo.mongoflex.entity.PageDTO<Character> pageDTO =
                new com.github.eacryo.mongoflex.entity.PageDTO<>();
        pageDTO.setCurrentPage(1L);
        pageDTO.setPageSize(10L);
        pageDTO = characterRepositoryV2.findPage(wrapper, pageDTO);

        System.out.println("include pagination: total=" + pageDTO.getTotal()
                + ", records=" + pageDTO.getRecords().size());
        for (Character ch : pageDTO.getRecords()) {
            System.out.println("  result: name=" + ch.getName()
                    + ", area=" + ch.getArea()
                    + ", desc=" + ch.getDescription());
        }

        Assertions.assertNotNull(pageDTO.getRecords());
        Assertions.assertTrue(pageDTO.getRecords().size() >= 2);
        for (Character ch : pageDTO.getRecords()) {
            Assertions.assertNotNull(ch.getName());
            Assertions.assertEquals("Inazuma", ch.getArea());
            Assertions.assertNull(ch.getDescription()); // not included
        }

        characterRepositoryV2.deleteById(id1);
        characterRepositoryV2.deleteById(id2);
    }

    @Test
    public void testExcludeProjection() {
        // insert
        Character c = new Character();
        String id = UlidCreator.getUlid().toString();
        c.setId(id);
        c.setName("ExcludeProj-" + id);
        c.setArea("Snezhnaya");
        c.setDescription("exclude test description");
        c.setBirthday(new Date());
        characterRepositoryV2.insert(c);

        // exclude description and birthday
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Character::getName, c.getName())
               .exclude(Character::getDescription, Character::getBirthday);
        List<Character> list = characterRepositoryV2.findList(wrapper);
        Assertions.assertNotNull(list);
        Assertions.assertEquals(1, list.size());

        Character result = list.get(0);
        System.out.println("exclude projection: name=" + result.getName()
                + ", area=" + result.getArea()
                + ", desc=" + result.getDescription()
                + ", birthday=" + result.getBirthday()
                + ", id=" + result.getId());

        // normally-returned fields should be populated
        Assertions.assertEquals(c.getName(), result.getName());
        Assertions.assertEquals("Snezhnaya", result.getArea());
        Assertions.assertEquals(id, result.getId());
        // excluded fields should be null
        Assertions.assertNull(result.getDescription());
        Assertions.assertNull(result.getBirthday());

        characterRepositoryV2.deleteById(id);
    }

    @Test
    public void testExcludeProjectionFindOne() {
        Character c = new Character();
        String id = UlidCreator.getUlid().toString();
        c.setId(id);
        c.setName("ExcludeOne-" + id);
        c.setArea("Mondstadt");
        c.setDescription("findOne exclude test");
        c.setBirthday(new Date());
        characterRepositoryV2.insert(c);

        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Character::getName, c.getName())
               .exclude(Character::getDescription, Character::getArea);
        Character result = characterRepositoryV2.findOne(wrapper);
        System.out.println("exclude findOne: name=" + result.getName()
                + ", area=" + result.getArea()
                + ", desc=" + result.getDescription()
                + ", birthday=" + result.getBirthday());

        Assertions.assertNotNull(result);
        Assertions.assertEquals(c.getName(), result.getName());
        Assertions.assertNotNull(result.getBirthday()); // not excluded
        Assertions.assertNull(result.getDescription()); // excluded
        Assertions.assertNull(result.getArea());         // excluded

        characterRepositoryV2.deleteById(id);
    }

    @Test
    public void testIncludeWithExcludeId() {
        // insert
        Character c = new Character();
        String id = UlidCreator.getUlid().toString();
        c.setId(id);
        c.setName("SelectExcludeId-" + id);
        c.setArea("Liyue");
        c.setBirthday(new Date());
        c.setDescription("include + exclude _id");
        characterRepositoryV2.insert(c);

        // include name and area, but exclude _id
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Character::getName, c.getName())
               .include(Character::getName, Character::getArea)
               .exclude(Character::getId);
        List<Character> list = characterRepositoryV2.findList(wrapper);
        Assertions.assertNotNull(list);
        Assertions.assertEquals(1, list.size());

        Character result = list.get(0);
        System.out.println("include+excludeId: name=" + result.getName()
                + ", area=" + result.getArea()
                + ", id=" + result.getId()
                + ", desc=" + result.getDescription());

        // included fields should be populated
        Assertions.assertEquals(c.getName(), result.getName());
        Assertions.assertEquals("Liyue", result.getArea());
        // _id should be excluded
        Assertions.assertNull(result.getId());
        // non-included fields should be null
        Assertions.assertNull(result.getDescription());

        characterRepositoryV2.deleteById(id);
    }

    @Test
    public void testExcludeProjectionPagination() {
        // insert 2 records
        String id1 = UlidCreator.getUlid().toString();
        Character c1 = new Character();
        c1.setId(id1);
        c1.setName("PageExcl-A-" + id1);
        c1.setArea("Fontaine");
        c1.setBirthday(new Date());
        c1.setDescription("page exclude A");
        characterRepositoryV2.insert(c1);

        String id2 = UlidCreator.getUlid().toString();
        Character c2 = new Character();
        c2.setId(id2);
        c2.setName("PageExcl-B-" + id2);
        c2.setArea("Fontaine");
        c2.setBirthday(new Date());
        c2.setDescription("page exclude B");
        characterRepositoryV2.insert(c2);

        // paginated query with exclude
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Character::getArea, "Fontaine")
               .exclude(Character::getDescription, Character::getBirthday);

        com.github.eacryo.mongoflex.entity.PageDTO<Character> pageDTO =
                new com.github.eacryo.mongoflex.entity.PageDTO<>();
        pageDTO.setCurrentPage(1L);
        pageDTO.setPageSize(10L);
        pageDTO = characterRepositoryV2.findPage(wrapper, pageDTO);

        System.out.println("exclude pagination: total=" + pageDTO.getTotal()
                + ", records=" + pageDTO.getRecords().size());
        for (Character ch : pageDTO.getRecords()) {
            System.out.println("  result: name=" + ch.getName()
                    + ", area=" + ch.getArea()
                    + ", desc=" + ch.getDescription()
                    + ", birthday=" + ch.getBirthday());
        }

        Assertions.assertNotNull(pageDTO.getRecords());
        Assertions.assertTrue(pageDTO.getRecords().size() >= 2);
        for (Character ch : pageDTO.getRecords()) {
            Assertions.assertNotNull(ch.getName());
            Assertions.assertEquals("Fontaine", ch.getArea());
            Assertions.assertNull(ch.getDescription()); // excluded
            Assertions.assertNull(ch.getBirthday());    // excluded
        }

        characterRepositoryV2.deleteById(id1);
        characterRepositoryV2.deleteById(id2);
    }
}
