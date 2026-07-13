package com.github.eacryo.mongoflex.v2;

import com.github.eacryo.mongoflex.Constant;
import com.github.eacryo.mongoflex.bean.Character;

import java.util.List;

@MRepository
public interface CharacterRepository extends MongoRepository<Character, String> {

    @Mql("db.getCollection('" + Constant.COLLECTION_NAME + "').find({})")
    List<Character> findAll();

    @Mql("db.getCollection('character').find({})")
    List<Object> findAllObj();

    @Mql("db.getCollection('character').find({'name':'#{name}'})")
    List<Character> findListByCriteria(@Param("name") String name);

    @Mql("db.getCollection('character').find({'_id':'#{id}','name':'#{name}'})")
    List<Character> findListByNameAndId(@Param("name") String name, @Param("id") String id);

    // FIXME: hardcoded data dependency — relies on pre-existing 'Hu Tao' document in collection
    // 硬编码数据依赖——依赖集合中已存在 'Hu Tao' 文档
    // @Mql("db.getCollection('character').find({'name':'Hu Tao'})")
    // List<Character> findListWithoutParam();

    @Mql("db.getCollection('character').count({})")
    long countByMql();

    @Mql("db.getCollection('character').count({'name':'#{name}'})")
    long countByCriteria(@Param("name") String name);

    @Mql("db.getCollection('character').findOne({'name':'#{name}'})")
    Character findOneByName(@Param("name") String name);

    // FIXME: hardcoded data dependency — relies on pre-existing document with specific _id
    // 硬编码数据依赖——依赖集合中已存在特定 _id 的文档
    // @Mql("db.getCollection('character').findOne({'_id':'01K43T5EFTT1QVSS8FPT6XK773','name':'Ganyu'})")
    // Character findOneByMql();

    @Mql("db.getCollection('character').find({})")
    List findListRaw();

    @Mql("db.getCollection('character').find({}).limit(10)")
    List<Character> findListWithLimit();

    @Mql("db.getCollection('character').find({}).skip(5).limit(5)")
    List<Character> findListWithSkipAndLimit();
}

