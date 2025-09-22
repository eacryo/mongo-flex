package com.github.eacryo.mongoflex.v2;

import com.github.eacryo.mongoflex.TestApplication;
import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.config.MongoFlexProperties;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("v2")
public class V2QueryTests {

    @Autowired
    private MongoFlexProperties mongoFlexProperties;

    @Test
    public void loadContext() {

    }

    @Test
    public void findAllByAnnotation() {
        MongoClient mongoClient = MongoClients.create(mongoFlexProperties.getUri());
        //TODO:先写死
        MongoDatabase database = mongoClient.getDatabase("mongo_flex");

        CharacterRepositoryV2 characterRepositoryV2 = MyRepositoryFactory.getRepository(database, CharacterRepositoryV2.class, Character.class);

        // 调用代理方法，它会执行我们实现的逻辑
        List<Character> characterList = characterRepositoryV2.findAll();
        characterList.forEach(System.out::println);
    }
}
