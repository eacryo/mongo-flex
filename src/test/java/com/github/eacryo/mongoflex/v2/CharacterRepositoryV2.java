package com.github.eacryo.mongoflex.v2;

import com.github.eacryo.mongoflex.bean.Character;

import java.util.List;

public interface CharacterRepositoryV2 {
        // 查询所有名为 'name' 的用户
//    @Mql("{ name: ?0 }")
//    List<Character> findByName(String name);
//
//    // 查询年龄大于 'age' 的用户
//    @Mql("{ age: { $gt: ?0 } }")
//    List<Character> findByAgeGreaterThan(int age);
//
//    // 根据id查询单个用户
//    @Mql("{ _id: ?0 }")
//    Character findById(String id);

    @Mql("db.getCollection(\"character\").find({})")
    List<Character> find();
}
