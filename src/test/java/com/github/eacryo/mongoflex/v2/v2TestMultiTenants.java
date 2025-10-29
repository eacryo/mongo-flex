package com.github.eacryo.mongoflex.v2;

import com.github.eacryo.mongoflex.TestApplication;
import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.config.MongoFlexProperties;
import com.github.eacryo.mongoflex.constant.MongoFlexConstant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("v2-multi-tenants")
public class v2TestMultiTenants {
    @Autowired
    private MongoFlexProperties mongoFlexProperties;

    @Autowired
    private DynamicMongoClient dynamicMongoClient;

    @Autowired
    private CharacterRepositoryV2 characterRepositoryV2;

    @Test
    public void testFindAll() {
        List<Character> characterList = characterRepositoryV2.findAll();
        characterList.forEach(System.out::println);
    }

    @Test
    public void testFindOneByMql() {
        Character character = characterRepositoryV2.findOneByMql();
        System.out.println(character);
    }

    @BeforeEach
    public void setup() {
        MDC.put(MongoFlexConstant.TENANT, "remote");

    }
}
