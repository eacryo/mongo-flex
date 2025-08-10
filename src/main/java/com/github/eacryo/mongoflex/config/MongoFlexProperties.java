package com.github.eacryo.mongoflex.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "mongo-flex")
@Component
public class MongoFlexProperties {
    
    private List<TenantConfig> tenants = new ArrayList<>();

    private Boolean enableMultiTenants;

}
