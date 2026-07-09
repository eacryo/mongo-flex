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

    @Mql("db.getCollection('character').find({'name':'Hu Tao'})")
    List<Character> findListWithoutParam();

    @Mql("db.getCollection('character').count({})")
    long countByMql();

    @Mql("db.getCollection('character').count({'name':'#{name}'})")
    long countByCriteria(@Param("name") String name);

    @Mql("db.getCollection('character').findOne({'name':'#{name}'})")
    Character findOneByName(@Param("name") String name);

    @Mql("db.getCollection('character').findOne({'_id':'01K43T5EFTT1QVSS8FPT6XK773','name':'Ganyu'})")
    Character findOneByMql();

    @Mql("db.getCollection('character').find({})")
    List findListRaw();
}

