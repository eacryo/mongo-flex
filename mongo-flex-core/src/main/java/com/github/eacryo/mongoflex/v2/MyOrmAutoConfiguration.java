package com.github.eacryo.mongoflex.v2;

import com.github.eacryo.mongoflex.convertor.MongoMappingConvertor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

// @Configuration 告诉 Spring 这是一个配置类
@Configuration
// @ConditionalOnClass 确保只有当用户引入了你的 ORM 和 MongoDB 驱动时，这个配置才生效
@ConditionalOnClass(com.mongodb.client.MongoClient.class)
// @Import 导入你的注册器，这是核心。它告诉 Spring 在这个配置类加载时，也去执行 MyOrmRegistrar
@Import(RepositoryRegistrar.class)
public class MyOrmAutoConfiguration {

    /**
     * 显式注册 MongoMappingConvertor，而非通过 {@code @Component} 扫描。
     * 该组件为框架内部使用，不应对用户暴露为可自动装配的 Bean。
     */
    @Bean
    MongoMappingConvertor mongoMappingConvertor() {
        return new MongoMappingConvertor();
    }
}
