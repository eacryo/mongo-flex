package com.github.eacryo.mongoflex.v2;

import com.github.eacryo.mongoflex.bean.Character;

import java.util.List;

@MRepository
public interface CharacterRepository extends MongoRepository<Character, String> {

    @Find("{}")
    List<Character> findAll();

    @Find("{}")
    List<Object> findAllObj();

    @Find("{name: #{name}}")
    List<Character> findListByCriteria(@Param("name") String name);

    @Find("{_id: #{id}, name: #{name}}")
    List<Character> findListByNameAndId(@Param("name") String name, @Param("id") String id);

    // FIXME: hardcoded data dependency — relies on pre-existing 'Hu Tao' document in collection
    // 硬编码数据依赖——依赖集合中已存在 'Hu Tao' 文档
    // @Find("{name: 'Hu Tao'}")
    // List<Character> findListWithoutParam();

    @Count("{}")
    long countByMql();

    @Count("{name: #{name}}")
    long countByCriteria(@Param("name") String name);

    @Find("{name: #{name}}")
    Character findOneByName(@Param("name") String name);

    // FIXME: hardcoded data dependency — relies on pre-existing document with specific _id
    // 硬编码数据依赖——依赖集合中已存在特定 _id 的文档
    // @Find("{_id: '01K43T5EFTT1QVSS8FPT6XK773', name: 'Ganyu'}")
    // Character findOneByMql();

    @Find("{}")
    List findListRaw();

    @Find(value = "{}", limit = 10)
    List<Character> findListWithLimit();

    @Find(value = "{}", skip = 5, limit = 5)
    List<Character> findListWithSkipAndLimit();
}
