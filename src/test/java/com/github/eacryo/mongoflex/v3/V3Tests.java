package com.github.eacryo.mongoflex.v3;

import com.github.eacryo.mongoflex.TestApplication;
import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.config.MongoFlexProperties;
import com.github.eacryo.mongoflex.v2.CharacterRepositoryV2;
import com.github.eacryo.mongoflex.v2.MyRepositoryFactory;
import com.github.eacryo.mongoflex.v2.Param;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;
import java.util.List;

@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("v2")
public class V3Tests {
    @Autowired
    private MongoFlexProperties mongoFlexProperties;

    @Test
    //并不需要这种写法
    public void testFindAll() {
        MongoClient mongoClient = MongoClients.create(mongoFlexProperties.getUri());
        MongoDatabase database = mongoClient.getDatabase("mongo_flex");

        // 创建你的 ORM 实例
        SimpleMongoORM orm = new SimpleMongoORM(database);


        List<Document> huTao = orm.findList("db.getCollection('character').find({'name':'#{name}'})",
                Pair.of("name", "huTao"), Pair.of("unusedParam", new Date()));

        huTao.forEach(System.out::println);
    }
}
