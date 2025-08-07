package com.github.eacryo.mongoflex.config;

import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.Arrays;

//解决启动时默认访问localhost的mongodb导致报错
@ConditionalOnProperty(
    prefix = "mongo-flex",      // 属性前缀（可选）
    name = "enable-multi-tenants",           // 属性名
    havingValue = "true",       // 预期值（可选）
    matchIfMissing = false      // 属性缺失时是否匹配（可选，默认false）
)
public class FrameworkExclusionFilter implements AutoConfigurationImportFilter {

    private static final String[] EXCLUDED_CONFIGURATIONS = {
        "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration"
    };

    @Override
    public boolean[] match(String[] autoConfigurationClasses,
                          AutoConfigurationMetadata autoConfigurationMetadata) {
        boolean[] matches = new boolean[autoConfigurationClasses.length];
        for (int i = 0; i < autoConfigurationClasses.length; i++) {
            matches[i] = shouldInclude(autoConfigurationClasses[i]);
        }
        return matches;
    }

    private boolean shouldInclude(String className) {
        return !Arrays.asList(EXCLUDED_CONFIGURATIONS).contains(className);
    }
}
