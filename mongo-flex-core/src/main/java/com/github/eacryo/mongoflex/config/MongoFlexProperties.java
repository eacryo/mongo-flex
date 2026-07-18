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

    /**
     * Look up a tenant's URI from static configuration by tenant name /
     * 根据租户名从静态配置中查找 URI
     *
     * @param tenantName tenant name / 租户名
     * @return the URI if found, null otherwise / URI 或 null
     */
    public String getTenantUri(String tenantName) {
        for (TenantConfig tenantConfig : tenants) {
            if (tenantConfig.getName().equals(tenantName)) {
                return tenantConfig.getUri();
            }
        }
        return null;
    }

    /**
     * Get the database name from the MongoDB connection URI /
     * 从 MongoDB 连接 URI 获取数据库名
     *
     * @return database name / 数据库名
     */
    public String getDatabaseFromUri() {
        if (!enableMultiTenants) {
            return new ConnectionString(uri).getDatabase();
        } else {
            String tenant = MDC.get(MongoFlexConstant.TENANT);
            String tenantUri = getTenantUri(tenant);
            if (tenantUri != null) {
                return new ConnectionString(tenantUri).getDatabase();
            }
            throw new IllegalArgumentException("Tenant not found: " + tenant
                    + ", available: " + tenants.stream().map(TenantConfig::getName)
                    .reduce((a, b) -> a + ", " + b).orElse("none"));
        }
    }
}
