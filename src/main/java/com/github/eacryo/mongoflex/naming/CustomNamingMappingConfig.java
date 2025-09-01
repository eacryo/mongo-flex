package com.github.eacryo.mongoflex.naming;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@Configuration
public class CustomNamingMappingConfig {

    @Bean
    public MongoMappingContext mongoMappingContext() {
        MongoMappingContext context = new MongoMappingContext();
        // 设置自定义的字段命名策略
        context.setFieldNamingStrategy(new CustomNamingStrategy());
        return context;
    }
}
