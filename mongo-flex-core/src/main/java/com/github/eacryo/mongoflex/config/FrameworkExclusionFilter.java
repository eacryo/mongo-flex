package com.github.eacryo.mongoflex.config;

import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Prevents Spring Boot from auto-creating a {@code MongoClient} that defaults to
 * {@code localhost:27017} — mongo-flex manages its own clients via
 * {@link com.github.eacryo.mongoflex.v2.DynamicMongoClient}.
 * <p>
 * Spring Data MongoDB's {@code MongoDataAutoConfiguration} is intentionally NOT
 * excluded: if the user provides a {@code MongoClient} bean (e.g. for Spring Data),
 * that auto-configuration can coexist with mongo-flex.
 * <p>
 * 防止 Spring Boot 自动创建连接 localhost:27017 的 MongoClient——
 * mongo-flex 通过 DynamicMongoClient 自行管理客户端。
 * 故意不排除 Spring Data MongoDB 的 MongoDataAutoConfiguration：
 * 若用户提供了 MongoClient Bean，Spring Data MongoDB 可与 mongo-flex 共存。
 */
public class FrameworkExclusionFilter implements AutoConfigurationImportFilter {

    private static final Set<String> EXCLUDED_CONFIGURATIONS = new HashSet<>(Arrays.asList(
            MongoAutoConfiguration.class.getName()
    ));

    @Override
    public boolean[] match(String[] autoConfigurationClasses,
                          AutoConfigurationMetadata autoConfigurationMetadata) {
        boolean[] matches = new boolean[autoConfigurationClasses.length];
        for (int i = 0; i < autoConfigurationClasses.length; i++) {
            matches[i] = !EXCLUDED_CONFIGURATIONS.contains(autoConfigurationClasses[i]);
        }
        return matches;
    }

}
