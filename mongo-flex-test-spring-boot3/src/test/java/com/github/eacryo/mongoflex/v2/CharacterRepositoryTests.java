package com.github.eacryo.mongoflex.v2;

import com.github.eacryo.mongoflex.TestApplication;
import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.util.SFunction;
import com.github.eacryo.mongoflex.ulid.Ulid;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.github.eacryo.mongoflex.entity.PageDTO;
import com.github.eacryo.mongoflex.entity.SortOrder;
import com.github.eacryo.mongoflex.query.LambdaQueryWrapper;

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
        String id = Ulid.generate();
        c.setId(id);
        c.setName("InsertTest-" + id);
        c.setAddress("InsertAddress");
        c.setBirthday(new Date());

        Character inserted = characterRepositoryV2.insert(c);
        System.out.println("inserted: " + inserted);

        // cleanup
        Character del = new Character();
        del.setId(id);
        del.setDeleted(true);
        characterRepositoryV2.updateOneById(del);
    }

    @Test
    public void testInsertMany() {
        // Create multiple entities / 创建多个实体
        Character c1 = new Character();
        String id1 = Ulid.generate();
        c1.setId(id1);
        c1.setName("InsertManyTest-" + id1);
        c1.setAddress("InsertManyAddress1");
        c1.setBirthday(new Date());

        Character c2 = new Character();
        String id2 = Ulid.generate();
        c2.setId(id2);
        c2.setName("InsertManyTest-" + id2);
        c2.setAddress("InsertManyAddress2");
        c2.setBirthday(new Date());

        Character c3 = new Character();
        String id3 = Ulid.generate();
        c3.setId(id3);
        c3.setName("InsertManyTest-" + id3);
        c3.setAddress("InsertManyAddress3");
        c3.setBirthday(new Date());

        // Batch insert / 批量插入
        List<Character> inserted = characterRepositoryV2.insertMany(Arrays.asList(c1, c2, c3));
        System.out.println("insertMany count: " + (inserted == null ? 0 : inserted.size()));
        for (Character c : inserted) {
            System.out.println("  inserted: id=" + c.getId() + ", name=" + c.getName());
        }

        // Verify each inserted entity can be found / 验证每个插入的实体都能查到
        for (Character c : inserted) {
            Character fetched = characterRepositoryV2.findById(c.getId());
            assert fetched != null : "Should find inserted entity / 应该能查到插入的实体: " + c.getId();
            assert c.getName().equals(fetched.getName()) : "Name should match / 名称应该匹配";
        }

        // Cleanup / 清理
        for (Character c : inserted) {
            Character del = new Character();
            del.setId(c.getId());
            del.setDeleted(true);
            characterRepositoryV2.updateOneById(del);
        }
    }

    @Test
    public void testFindById() {
        Character c = new Character();
        String id = Ulid.generate();
        c.setId(id);
        c.setName("FindByIdTest-" + id);
        characterRepositoryV2.insert(c);

        Character fetched = characterRepositoryV2.findById(id);
        System.out.println("fetched by id: " + fetched);

        Character del = new Character();
        del.setId(id);
        del.setDeleted(true);
        characterRepositoryV2.updateOneById(del);
    }

    @Test
    public void testFindOneByEntity() {
        Character c = new Character();
        String id = Ulid.generate();
        c.setId(id);
        c.setName("FindOneEntityTest-" + id);
        characterRepositoryV2.insert(c);

        Character query = new Character();
        query.setName(c.getName());
        Character one = characterRepositoryV2.findOneByEntity(query);
        System.out.println("findOneByEntity: " + one);

        Character del = new Character();
        del.setId(id);
        del.setDeleted(true);
        characterRepositoryV2.updateOneById(del);
    }

    @Test
    public void testFindListByEntity() {
        // 插入两条同 name 的记录
        String commonName = "FindListEntityTest-" + Ulid.generate();
        Character c1 = new Character();
        c1.setId(Ulid.generate());
        c1.setName(commonName);
        c1.setAddress("Addr1");
        characterRepositoryV2.insert(c1);

        Character c2 = new Character();
        c2.setId(Ulid.generate());
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
        for (String cid : Arrays.asList(c1.getId(), c2.getId())) {
            Character del = new Character();
            del.setId(cid);
            del.setDeleted(true);
            characterRepositoryV2.updateOneById(del);
        }
    }

    @Test
    public void testFindPageByEntity() {
        String commonName = "FindPageTest-" + Ulid.generate();
        // 插入 5 条记录，address 用于区分和排序
        for (int i = 0; i < 5; i++) {
            Character c = new Character();
            c.setId(Ulid.generate());
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

        // 验证总数和分页
        System.out.println("total: " + result.getTotal() + ", totalPage: " + result.getTotalPage() + ", records: " + result.getRecords());
        System.out.println("records size: " + result.getRecords().size());
        System.out.println("first record address: " + result.getRecords().get(0).getAddress());
        System.out.println("second record address: " + result.getRecords().get(1).getAddress());
        // total 应该 >= 5（可能有其他测试残留的同名数据）
        // 但由于使用了唯一 commonName，total 应该恰好为 5
        // records 大小应 <= pageSize

        // cleanup：按 name 批量删
        characterRepositoryV2.deleteMany(Character::getName, commonName);
    }

    @Test
    public void testFindPageWithLambdaSort() {
        String commonName = "FindPageLambdaSort-" + Ulid.generate();
        // 插入 5 条记录，address 用于区分
        for (int i = 0; i < 5; i++) {
            Character c = new Character();
            c.setId(Ulid.generate());
            c.setName(commonName);
            c.setAddress("LambdaAddr-" + i);
            characterRepositoryV2.insert(c);
        }

        // Lambda 类型安全排序：按 address 降序
        PageDTO<Character> pageDTO = new PageDTO<>();
        pageDTO.setCurrentPage(1L);
        pageDTO.setPageSize(3L);

        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<Character>()
                .eq(Character::getName, commonName)
                .orderByDesc(Character::getAddress);

        PageDTO<Character> result = characterRepositoryV2.findPage(wrapper, pageDTO);
        System.out.println("findPage with Lambda sort: total=" + result.getTotal()
                + ", totalPage=" + result.getTotalPage()
                + ", records.size=" + (result.getRecords() != null ? result.getRecords().size() : 0));

        // 验证分页
        System.out.println("records size: " + result.getRecords().size());
        if (result.getRecords().size() >= 2) {
            String first = result.getRecords().get(0).getAddress();
            String second = result.getRecords().get(1).getAddress();
            System.out.println("first: " + first + ", second: " + second);
            // 降序排列：LambdaAddr-4 > LambdaAddr-3 > ...
        }

        // cleanup
        characterRepositoryV2.deleteMany(Character::getName, commonName);
    }

    @Test
    public void testFindOneByFunction() {
        Character c = new Character();
        String id = Ulid.generate();
        c.setId(id);
        c.setName("FindOneByFuncTest-" + id);
        characterRepositoryV2.insert(c);

        Character one = characterRepositoryV2.findOne(Character::getName, c.getName());
        System.out.println("findOne(field): " + one);

        Character del = new Character();
        del.setId(id);
        del.setDeleted(true);
        characterRepositoryV2.updateOneById(del);
    }

    @Test
    public void testCountVariants() {
        long c1 = characterRepositoryV2.count();
        System.out.println("count(): " + c1);

        Character c = new Character();
        c.setName("CountEntityTest");
        long c2 = characterRepositoryV2.countByEntity(c);
        System.out.println("countByEntity(entity): " + c2);

        long c3 = characterRepositoryV2.count(Character::getName, "CountEntityTest");
        System.out.println("count(field,value): " + c3);

        System.out.println("countByMql(): " + characterRepositoryV2.countByMql());
    }

    @Test
    public void testUpdateById() {
        Character c = new Character();
        String id = Ulid.generate();
        c.setId(id);
        c.setName("UpdateByIdTest-" + id);
        c.setAddress("Before");
        characterRepositoryV2.insert(c);

        c.setAddress("After");
        long updated = characterRepositoryV2.updateOneById(c);
        System.out.println("updateById result: " + updated);

        Character fetched = characterRepositoryV2.findById(id);
        System.out.println("post-update fetched: " + fetched);

        Character del = new Character();
        del.setId(id);
        del.setDeleted(true);
        characterRepositoryV2.updateOneById(del);
    }

    @Test
    public void testUpdateByField() {
        Character c = new Character();
        String id = Ulid.generate();
        c.setId(id);
        c.setName("UpdateByFieldTest-" + id);
        c.setAddress("Addr");
        characterRepositoryV2.insert(c);

        long updated = characterRepositoryV2.updateMany(Character::getName, c.getName(), c);
        System.out.println("update(field) result: " + updated);

        Character del = new Character();
        del.setId(id);
        del.setDeleted(true);
        characterRepositoryV2.updateOneById(del);
    }

    @Test
    public void testUpsert() {
        String id = Ulid.generate();
        // 先确认这条记录不存在
        Character fetched = characterRepositoryV2.findById(id);
        System.out.println("pre-upsert findById: " + fetched);

        // upsert：不存在则插入
        Character c = new Character();
        c.setId(id);
        c.setName("UpsertTest-" + id);
        c.setAddress("Upserted");
        long result1 = characterRepositoryV2.updateOneById(c, true);
        System.out.println("upsertById (insert) result: " + result1);

        // 验证已插入
        Character afterInsert = characterRepositoryV2.findById(id);
        System.out.println("after upsertById: " + afterInsert);

        // 再次 upsert 同 _id：应更新
        c.setAddress("Upserted-Updated");
        long result2 = characterRepositoryV2.updateOneById(c, true);
        System.out.println("upsertById (update) result: " + result2);

        Character afterUpdate = characterRepositoryV2.findById(id);
        System.out.println("after second upsertById: " + afterUpdate);

        // cleanup
        Character del = new Character();
        del.setId(id);
        del.setDeleted(true);
        characterRepositoryV2.updateOneById(del);
    }

    @Test
    public void testDeleteById() {
        Character c = new Character();
        String id = Ulid.generate();
        c.setId(id);
        c.setName("DeleteByIdTest-" + id);
        characterRepositoryV2.insert(c);

        long del = characterRepositoryV2.deleteOneById(id);
        System.out.println("deleteById result: " + del);
    }

    @Test
    public void testDeleteByEntity() {
        Character c = new Character();
        String id = Ulid.generate();
        c.setId(id);
        c.setName("DeleteByEntityTest-" + id);
        characterRepositoryV2.insert(c);

        long del = characterRepositoryV2.deleteByEntity(c);
        System.out.println("deleteByEntity result: " + del);
    }

    @Test
    public void testDeleteByField() {
        Character c = new Character();
        String id = Ulid.generate();
        c.setId(id);
        c.setName("DeleteByFieldTest-" + id);
        characterRepositoryV2.insert(c);

        long del = characterRepositoryV2.deleteMany(Character::getName, c.getName());
        System.out.println("delete(field) result: " + del);
    }
}
