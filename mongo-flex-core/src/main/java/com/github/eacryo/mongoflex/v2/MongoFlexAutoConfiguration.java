package com.github.eacryo.mongoflex.v2;

import com.github.eacryo.mongoflex.convertor.MongoMappingConvertor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnClass(com.mongodb.client.MongoClient.class)
@Import(RepositoryRegistrar.class)
public class MongoFlexAutoConfiguration {

    /**
     * 显式注册 MongoMappingConvertor，而非通过 {@code @Component} 扫描。
     * 该组件为框架内部使用，不应对用户暴露为可自动装配的 Bean。
     */
    @Bean
    MongoMappingConvertor mongoMappingConvertor() {
        return new MongoMappingConvertor();
    }
}
