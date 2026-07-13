package com.github.eacryo.mongoflex.v2;

import com.github.eacryo.mongoflex.config.MongoFlexProperties;
import com.github.eacryo.mongoflex.convertor.MongoMappingConvertor;
import com.github.eacryo.mongoflex.util.CollectionNameUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * mongo-flex auto-configuration / mongo-flex 自动配置
 */
@Configuration
@ConditionalOnClass(com.mongodb.client.MongoClient.class)
@EnableConfigurationProperties(MongoFlexProperties.class)
@Import(RepositoryRegistrar.class)
public class MongoFlexAutoConfiguration {

    @Bean
    MongoMappingConvertor mongoMappingConvertor() {
        return new MongoMappingConvertor();
    }

    @Bean
    CollectionNameUtil collectionNameUtil() {
        return new CollectionNameUtil();
    }

    @Bean
    DynamicMongoClient dynamicMongoClient() {
        return new DynamicMongoClient();
    }
}
