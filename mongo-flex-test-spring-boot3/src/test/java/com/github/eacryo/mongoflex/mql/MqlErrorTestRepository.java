package com.github.eacryo.mongoflex.mql;

import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.v2.MRepository;
import com.github.eacryo.mongoflex.v2.Mql;
import com.github.eacryo.mongoflex.v2.Param;

import java.util.List;

@MRepository
public interface MqlErrorTestRepository {

    @Mql("db.getCollection('character').insertOne({'name':'#{name}'})")
    String unsupportedOperation(@Param("name") String name);

    @Mql("this is not a valid mql command")
    List<Character> malformedCommand();

    @Mql("db.getCollection('character').find({'name':'#{name}'})")
    List<Character> withNullParam(@Param("name") String name);
}

