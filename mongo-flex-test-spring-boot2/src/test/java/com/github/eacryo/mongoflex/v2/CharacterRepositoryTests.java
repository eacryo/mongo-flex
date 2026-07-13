package com.github.eacryo.mongoflex.v2;

import com.github.eacryo.mongoflex.TestApplication;
import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.util.SFunction;
import com.github.f4b6a3.ulid.UlidCreator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.github.eacryo.mongoflex.entity.PageDTO;
import com.github.eacryo.mongoflex.entity.SortOrder;
import com.github.eacryo.mongoflex.lambda.LambdaQueryWrapper;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("v2")
public class CharacterRepositoryTests {

    @Autowired
    private CharacterRepository characterRepositoryV2;

    // -- tests for repository-declared MQL methods --

    @Test
    public void testFindAll() {
        List<Character> all = characterRepositoryV2.findAll();
        System.out.println("findAll size: " + (all == null ? 0 : all.size()));
    }

    @Test
    public void testFindAllObj() {
        List<Object> allObj = characterRepositoryV2.findAllObj();
        System.out.println("findAllObj size: " + (allObj == null ? 0 : allObj.size()));
    }

    @Test
    public void testFindListByCriteria() {
        List<Character> huTao = characterRepositoryV2.findListByCriteria("Hu Tao");
        System.out.println("findListByCriteria(Hu Tao): " + huTao);
    }

    @Test
    public void testFindListByNameAndId() {
        List<Character> nameAndId = characterRepositoryV2.findListByNameAndId("Hu Tao", "specialId");
        System.out.println("findListByNameAndId: " + nameAndId);
    }

    // FIXME: hardcoded data dependency — relies on pre-existing documents in collection
    // @Test
    // public void testFindListWithoutParam() {
    //     List<Character> fixed = characterRepositoryV2.findListWithoutParam();
    //     System.out.println("findListWithoutParam: " + fixed);
    // }

    // FIXME: hardcoded data dependency — relies on pre-existing document with specific _id
    // @Test
    // public void testFindOneByMql() {
    //     Character one = characterRepositoryV2.findOneByMql();
    //     System.out.println("findOneByMql: " + one);
    // }

    // -- tests for methods inherited from MongoRepository --

    @Test
    public void testInsert() {
        Character c = new Character();
        String id = UlidCreator.getUlid().toString();
        c.setId(id);
        c.setName("InsertTest-" + id);
        c.setAddress("InsertAddress");
        c.setBirthday(new Date());

        Character inserted = characterRepositoryV2.insert(c);
        System.out.println("inserted: " + inserted);

        // cleanup
        characterRepositoryV2.deleteById(id);
    }

    @Test
    public void testFindById() {
        Character c = new Character();
        String id = UlidCreator.getUlid().toString();
        c.setId(id);
        c.setName("FindByIdTest-" + id);
        characterRepositoryV2.insert(c);

        Character fetched = characterRepositoryV2.findById(id);
        System.out.println("fetched by id: " + fetched);

        characterRepositoryV2.deleteById(id);
    }

    @Test
    public void testFindOneByEntity() {
        Character c = new Character();
        String id = UlidCreator.getUlid().toString();
        c.setId(id);
        c.setName("FindOneEntityTest-" + id);
        characterRepositoryV2.insert(c);

        Character query = new Character();
        query.setName(c.getName());
        Character one = characterRepositoryV2.findOneByEntity(query);
        System.out.println("findOneByEntity: " + one);

        characterRepositoryV2.deleteById(id);
    }

    @Test
    public void testFindListByEntity() {
        // 插入两条同 name 的记录
        String commonName = "FindListEntityTest-" + UlidCreator.getUlid().toString();
        Character c1 = new Character();
        c1.setId(UlidCreator.getUlid().toString());
        c1.setName(commonName);
        c1.setAddress("Addr1");
        characterRepositoryV2.insert(c1);

        Character c2 = new Character();
        c2.setId(UlidCreator.getUlid().toString());
        c2.setName(commonName);
        c2.setAddress("Addr2");
        characterRepositoryV2.insert(c2);

        // 按 name 查，应返回两条
        Character query = new Character();
        query.setName(commonName);
        List<Character> list = characterRepositoryV2.findListByEntity(query);
        System.out.println("findListByEntity size: " + (list != null ? list.size() : 0));
        System.out.println("findListByEntity: " + list);

        // cleanup
        characterRepositoryV2.deleteById(c1.getId());
        characterRepositoryV2.deleteById(c2.getId());
    }

    @Test
    public void testFindPageByEntity() {
        String commonName = "FindPageTest-" + UlidCreator.getUlid().toString();
        // 插入 5 条记录，address 用于区分和排序
        for (int i = 0; i < 5; i++) {
            Character c = new Character();
            c.setId(UlidCreator.getUlid().toString());
            c.setName(commonName);
            c.setAddress("PageAddr-" + i);
            characterRepositoryV2.insert(c);
        }

        // 分页查询：按 name 查，每页 2 条，第 1 页，按 address 升序
        Character query = new Character();
        query.setName(commonName);
        PageDTO<Character> pageDTO = new PageDTO<>();
        pageDTO.setCurrentPage(1L);
        pageDTO.setPageSize(2L);
        pageDTO.setOrderBy(Arrays.asList(new SortOrder<>("address", true)));

        PageDTO<Character> result = characterRepositoryV2.findPageByEntity(query, pageDTO);
        System.out.println("findPageByEntity: total=" + result.getTotal()
                + ", totalPage=" + result.getTotalPage()
                + ", records.size=" + (result.getRecords() != null ? result.getRecords().size() : 0));

        // cleanup：按 name 批量删
        characterRepositoryV2.delete(Character::getName, commonName);
    }

    @Test
    public void testFindPageWithLambdaSort() {
        String commonName = "FindPageLambdaSort-" + UlidCreator.getUlid().toString();
        for (int i = 0; i < 5; i++) {
            Character c = new Character();
            c.setId(UlidCreator.getUlid().toString());
            c.setName(commonName);
            c.setAddress("LambdaAddr-" + i);
            characterRepositoryV2.insert(c);
        }

        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<Character>()
                .eq(Character::getName, commonName)
                .orderByDesc(Character::getAddress);

        PageDTO<Character> pageDTO = new PageDTO<>();
        pageDTO.setCurrentPage(1L);
        pageDTO.setPageSize(3L);

        PageDTO<Character> result = characterRepositoryV2.findPage(wrapper, pageDTO);
        System.out.println("findPage with Lambda sort: total=" + result.getTotal()
                + ", totalPage=" + result.getTotalPage()
                + ", records.size=" + (result.getRecords() != null ? result.getRecords().size() : 0));

        characterRepositoryV2.delete(Character::getName, commonName);
    }

    @Test
    public void testFindOneByFunction() {
        Character c = new Character();
        String id = UlidCreator.getUlid().toString();
        c.setId(id);
        c.setName("FindOneByFuncTest-" + id);
        characterRepositoryV2.insert(c);

        Character one = characterRepositoryV2.findOne(Character::getName, c.getName());
        System.out.println("findOne(field): " + one);

        characterRepositoryV2.deleteById(id);
    }

    @Test
    public void testCountVariants() {
        long c1 = characterRepositoryV2.count();
        System.out.println("count(): " + c1);

        Character c = new Character();
        c.setName("CountEntityTest");
        long c2 = characterRepositoryV2.countByEntity(c);
        System.out.println("count(entity): " + c2);

        long c3 = characterRepositoryV2.count(Character::getName, "CountEntityTest");
        System.out.println("count(field,value): " + c3);

        System.out.println("countByMql(): " + characterRepositoryV2.countByMql());
    }

    @Test
    public void testUpdateById() {
        Character c = new Character();
        String id = UlidCreator.getUlid().toString();
        c.setId(id);
        c.setName("UpdateByIdTest-" + id);
        c.setAddress("Before");
        characterRepositoryV2.insert(c);

        c.setAddress("After");
        long updated = characterRepositoryV2.updateOneById(c);
        System.out.println("updateById result: " + updated);

        Character fetched = characterRepositoryV2.findById(id);
        System.out.println("post-update fetched: " + fetched);

        characterRepositoryV2.deleteById(id);
    }

    @Test
    public void testUpdateByField() {
        Character c = new Character();
        String id = UlidCreator.getUlid().toString();
        c.setId(id);
        c.setName("UpdateByFieldTest-" + id);
        c.setAddress("Addr");
        characterRepositoryV2.insert(c);

        long updated = characterRepositoryV2.updateMany(Character::getName, c.getName(), c);
        System.out.println("update(field) result: " + updated);

        characterRepositoryV2.deleteById(id);
    }

    @Test
    public void testDeleteById() {
        Character c = new Character();
        String id = UlidCreator.getUlid().toString();
        c.setId(id);
        c.setName("DeleteByIdTest-" + id);
        characterRepositoryV2.insert(c);

        long del = characterRepositoryV2.deleteById(id);
        System.out.println("deleteById result: " + del);
    }

    @Test
    public void testDeleteByEntity() {
        Character c = new Character();
        String id = UlidCreator.getUlid().toString();
        c.setId(id);
        c.setName("DeleteByEntityTest-" + id);
        characterRepositoryV2.insert(c);

        long del = characterRepositoryV2.deleteByEntity(c);
        System.out.println("deleteByEntity result: " + del);
    }

    @Test
    public void testDeleteByField() {
        Character c = new Character();
        String id = UlidCreator.getUlid().toString();
        c.setId(id);
        c.setName("DeleteByFieldTest-" + id);
        characterRepositoryV2.insert(c);

        long del = characterRepositoryV2.delete(Character::getName, c.getName());
        System.out.println("delete(field) result: " + del);
    }
}
