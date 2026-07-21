package com.github.eacryo.mongoflex.config;

import com.github.eacryo.mongoflex.convertor.MongoMappingConvertor;
import com.github.eacryo.mongoflex.query.MongoOps;
import com.github.eacryo.mongoflex.repository.DynamicMongoClient;
import com.github.eacryo.mongoflex.repository.RepositoryRegistrar;
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
    DynamicMongoClient dynamicMongoClient() {
        return new DynamicMongoClient();
    }

    @Bean
    MongoOps mongoOps(DynamicMongoClient dynamicMongoClient, MongoMappingConvertor convertor) {
        return new MongoOps(dynamicMongoClient, convertor);
    }
}
