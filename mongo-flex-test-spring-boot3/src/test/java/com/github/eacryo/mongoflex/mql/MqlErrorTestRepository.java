package com.github.eacryo.mongoflex.mql;

import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.v2.Find;
import com.github.eacryo.mongoflex.v2.MRepository;
import com.github.eacryo.mongoflex.v2.MongoRepository;
import com.github.eacryo.mongoflex.v2.Param;

import java.util.List;

@MRepository
public interface MqlErrorTestRepository extends MongoRepository<Character, String> {

    /**
     * Malformed JSON template — Document.parse will throw / 非法 JSON 模板——Document.parse 会抛异常
     */
    @Find("this is not valid json")
    List<Character> malformedCommand();

    /**
     * Valid query with nullable param / 合法查询，参数可为 null
     */
    @Find("{name: #{name}}")
    List<Character> withNullParam(@Param("name") String name);
}
