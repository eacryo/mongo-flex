package com.github.eacryo.mongoflex.config;

import com.github.eacryo.mongoflex.constant.MongoFlexConstant;
import com.mongodb.ConnectionString;
import lombok.Data;
import org.slf4j.MDC;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "mongo-flex")
public class MongoFlexProperties {

    private List<TenantConfig> tenants = new ArrayList<>();

    private boolean enableMultiTenants;
    private boolean toSnakeCase;
    private String uri;

    public String getDatabaseFromUri() {
        //未开启多租户时
        if (!enableMultiTenants) {
            ConnectionString connectionString = new ConnectionString(uri);
            return connectionString.getDatabase();
        } else {
            String tenant = MDC.get(MongoFlexConstant.TENANT);
            for (TenantConfig tenantConfig : tenants) {
                if (tenantConfig.getName().equals(tenant)) {
                    ConnectionString connectionString = new ConnectionString(tenantConfig.getUri());
                    return connectionString.getDatabase();
                }
            }
            throw new IllegalArgumentException("Tenant not found");
        }
    }
}
