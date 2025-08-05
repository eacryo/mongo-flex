package com.github.eacryo.mongoflex.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "spring.data.mongodb")
@Component
public class MultiTenantMongoProperties {
    
    private List<TenantMongoConfig> tenants = new ArrayList<>();

    

    public static class TenantMongoConfig {
        private String name;
        private String tablePrefix;
        private String uri;
        
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTablePrefix() {
            return tablePrefix;
        }

        public void setTablePrefix(String tablePrefix) {
            this.tablePrefix = tablePrefix;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }
    }



    public List<TenantMongoConfig> getTenants() {
        return tenants;
    }



    public void setTenants(List<TenantMongoConfig> tenants) {
        this.tenants = tenants;
    }
}
