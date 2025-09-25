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
public class V2AnnotationQueryTests {

    @Autowired
    private MongoFlexProperties mongoFlexProperties;

    @Autowired
    private DynamicMongoClient dynamicMongoClient;

    @Test
    public void loadContext() {

    }

    @Test
    public void testFindAll() {
        //MongoClient mongoClient = MongoClients.create(mongoFlexProperties.getUri());
        //TODO:先写死
        //这里会提示 'MongoClient' used without 'try'-with-resources statement ，但这个提示是错误的
        //想要关闭这个提示，可以在方法上标注 @SuppressWarnings("resource")
        MongoDatabase database = dynamicMongoClient.select().getDatabase("mongo_flex");

        CharacterRepositoryV2 characterRepositoryV2 = MyRepositoryFactory.getRepository(database, CharacterRepositoryV2.class, Character.class);

        // 调用代理方法，它会执行我们实现的逻辑
        List<Character> characterList = characterRepositoryV2.findAll();
        characterList.forEach(System.out::println);
    }

    @Test
    public void testFindByCriteria() {
        MongoClient mongoClient = MongoClients.create(mongoFlexProperties.getUri());
        //TODO:先写死
        MongoDatabase database = mongoClient.getDatabase("mongo_flex");

        CharacterRepositoryV2 characterRepositoryV2 = MyRepositoryFactory.getRepository(database, CharacterRepositoryV2.class, Character.class);

        // 调用代理方法，它会执行我们实现的逻辑
        List<Character> characterList = characterRepositoryV2.findListByCriteria("Hu Tao");
        characterList.forEach(System.out::println);
        List<Character> anotherList = characterRepositoryV2.findListByCriteria("Ganyu");
        anotherList.forEach(System.out::println);
        //给一个确定的参数
        characterRepositoryV2.findListWithoutParam().forEach(System.out::println);
    }

    @Test
    public void testFindListByNameAndId() {
        MongoClient mongoClient = MongoClients.create(mongoFlexProperties.getUri());
        //TODO:先写死
        MongoDatabase database = mongoClient.getDatabase("mongo_flex");

        CharacterRepositoryV2 characterRepositoryV2 = MyRepositoryFactory.getRepository(database, CharacterRepositoryV2.class, Character.class);
        characterRepositoryV2.findListByNameAndId("Hu Tao", "specialId").forEach(System.out::println);
    }
}
