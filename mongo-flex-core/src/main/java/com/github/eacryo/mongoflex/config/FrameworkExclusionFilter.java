package com.github.eacryo.mongoflex.config;

import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

// 解决启动时默认访问 localhost 的 MongoDB 导致报错
public class FrameworkExclusionFilter implements AutoConfigurationImportFilter {

    private static final Set<String> EXCLUDED_CONFIGURATIONS = new HashSet<>(Arrays.asList(
            MongoAutoConfiguration.class.getName(),
            MongoDataAutoConfiguration.class.getName()
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
