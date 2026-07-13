package com.github.eacryo.mongoflex.v2;

import com.github.eacryo.mongoflex.config.MongoFlexProperties;
import com.github.eacryo.mongoflex.convertor.MongoMappingConvertor;
import com.github.eacryo.mongoflex.strategy.CountExecutor;
import com.github.eacryo.mongoflex.strategy.DeleteOneExecutor;
import com.github.eacryo.mongoflex.strategy.ExecutorProxy;
import com.github.eacryo.mongoflex.strategy.FindExecutor;
import com.github.eacryo.mongoflex.strategy.FindOneExecutor;
import com.github.eacryo.mongoflex.util.CollectionNameUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * mongo-flex 自动配置。
 * <p>
 * 框架内部的组件（如 {@code MongoMappingConvertor}、各种 {@code CommandExecutor} 等）
 * 仅服务于框架自身，不应暴露给用户通过 {@code @Autowired} 直接引用。
 * 因此它们不标注 {@code @Component}，而是在此处通过 {@code @Bean} 方法或
 * 其他显式注册方式纳入 Spring 容器，从而缩小 API 表面积。
 */
@Configuration
@ConditionalOnClass(com.mongodb.client.MongoClient.class)
@EnableConfigurationProperties(MongoFlexProperties.class)
@Import(RepositoryRegistrar.class)
public class MongoFlexAutoConfiguration {

    // ──── 转换器 ────

    @Bean
    MongoMappingConvertor mongoMappingConvertor() {
        return new MongoMappingConvertor();
    }

    // ──── 工具类 ────

    @Bean
    CollectionNameUtil collectionNameUtil() {
        return new CollectionNameUtil();
    }

    // ──── 多租户客户端 ────

    @Bean
    DynamicMongoClient dynamicMongoClient() {
        return new DynamicMongoClient();
    }

    // ──── @Mql 策略组件（deprecated，保留向后兼容） ────

    @Bean
    @SuppressWarnings("deprecation")
    ExecutorProxy executorProxy() {
        return new ExecutorProxy();
    }

    @Bean
    FindExecutor findExecutor() {
        return new FindExecutor();
    }

    @Bean
    FindOneExecutor findOneExecutor() {
        return new FindOneExecutor();
    }

    @Bean
    CountExecutor countExecutor() {
        return new CountExecutor();
    }

    @Bean
    DeleteOneExecutor deleteOneExecutor() {
        return new DeleteOneExecutor();
    }
}
