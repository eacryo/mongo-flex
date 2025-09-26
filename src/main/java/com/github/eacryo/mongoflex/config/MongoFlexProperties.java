package com.github.eacryo.mongoflex.config;

import com.mongodb.ConnectionString;
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

    private boolean enableMultiTenants;
    private boolean toSnakeCase;
    private String uri;

    public String getDatabaseFromUri(){
        ConnectionString connectionString = new ConnectionString(uri);
        return connectionString.getDatabase();
    }
}
