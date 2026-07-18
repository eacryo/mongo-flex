package com.github.eacryo.mongoflex.repository;

import com.github.eacryo.mongoflex.config.MongoFlexProperties;
import com.github.eacryo.mongoflex.config.TenantConfig;
import com.github.eacryo.mongoflex.constant.MongoFlexConstant;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class DynamicMongoClient implements InitializingBean, DisposableBean {
    private static final String MONGO_CLIENT_PREFIX = "mongoClient_";

    private final Map<String, TenantEntry> entries = new ConcurrentHashMap<>();

    @Autowired
    private GenericApplicationContext genericApplicationContext;

    @Autowired
    private MongoFlexProperties mongoFlexProperties;

    public MongoClient select() {
        if (mongoFlexProperties.isEnableMultiTenants()) {
            return select(MDC.get(MongoFlexConstant.TENANT));
        }
        return select(MongoFlexConstant.DEFAULT_TENANT_WHEN_DISABLE);
    }

    public MongoClient select(String tenant) {
        if (!StringUtils.hasText(tenant)) {
            throw new IllegalArgumentException("tenant must not be null or empty / tenant 不能为 null 或空字符串");
        }
        TenantEntry entry = entries.get(tenant);
        if (entry == null) {
            throw new IllegalArgumentException(
                    "Cannot find MongoClient for tenant: " + tenant
                    + ". Available tenants: " + entries.keySet() + " / 未找到 tenant 对应的 MongoClient，可用 tenant: " + entries.keySet());
        }
        if (log.isDebugEnabled()) {
            log.debug("using MongoClient for tenant {}", tenant);
        }
        return entry.client;
    }

    public MongoDatabase selectDatabase() {
        if (mongoFlexProperties.isEnableMultiTenants()) {
            return selectDatabase(MDC.get(MongoFlexConstant.TENANT));
        }
        return selectDatabase(MongoFlexConstant.DEFAULT_TENANT_WHEN_DISABLE);
    }

    public MongoDatabase selectDatabase(String tenant) {
        if (!StringUtils.hasText(tenant)) {
            throw new IllegalArgumentException("tenant must not be null or empty / tenant 不能为 null 或空字符串");
        }
        TenantEntry entry = entries.get(tenant);
        if (entry == null) {
            throw new IllegalArgumentException(
                    "Cannot find MongoClient for tenant: " + tenant
                    + ". Available tenants: " + entries.keySet() + " / 未找到 tenant 对应的 MongoClient，可用 tenant: " + entries.keySet());
        }
        return entry.getDatabase();
    }

    public void registerTenant(TenantConfig config) {
        String name = config.getName();
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("tenant name must not be null or empty / 租户名不能为 null 或空字符串");
        }
        if (!StringUtils.hasText(config.getUri())) {
            throw new IllegalArgumentException("tenant uri must not be null or empty / 租户 URI 不能为 null 或空字符串");
        }
        TenantEntry newEntry = new TenantEntry(config.getUri());
        TenantEntry old = entries.put(name, newEntry);
        if (old != null) {
            old.close();
            log.info("Replaced MongoClient for tenant: {}", name);
        } else {
            log.info("Registered MongoClient for tenant: {}", name);
        }
    }

    public boolean removeTenant(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            return false;
        }
        TenantEntry removed = entries.remove(tenantId);
        if (removed != null) {
            removed.close();
            log.info("Removed MongoClient for tenant: {}", tenantId);
            return true;
        }
        return false;
    }

    public Set<String> listTenants() {
        return Collections.unmodifiableSet(entries.keySet());
    }

    @Override
    public void afterPropertiesSet() {
        initializeTemplates();
    }

    private void initializeTemplates() {
        if (mongoFlexProperties.isEnableMultiTenants()) initWhenEnabled();
        else initWhenDisabled();
        log.info("MongoClientFactory initialized with {} tenants", entries.size());
    }

    private void initWhenEnabled() {
        Map<String, Exception> errors = new HashMap<>();
        for (TenantConfig config : mongoFlexProperties.getTenants()) {
            String tenantId = config.getName();
            try {
                TenantEntry entry = new TenantEntry(config.getUri());
                entries.put(tenantId, entry);
                String beanName = MONGO_CLIENT_PREFIX + tenantId;
                genericApplicationContext.registerBean(beanName, MongoClient.class,
                        () -> entry.client);
                log.info("Added MongoClient for tenant: {}", tenantId);
            } catch (Exception e) {
                errors.put(tenantId, e);
                log.error("Failed to create MongoClient for tenant: {}, error: {}", tenantId, e.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            throw new RuntimeException("Failed to create MongoClient for tenants: " + errors.keySet(),
                    errors.values().iterator().next());
        }
    }

    private void initWhenDisabled() {
        String tenantId = MongoFlexConstant.DEFAULT_TENANT_WHEN_DISABLE;
        try {
            TenantEntry entry = new TenantEntry(mongoFlexProperties.getUri());
            entries.put(tenantId, entry);
            String beanName = MONGO_CLIENT_PREFIX + tenantId;
            genericApplicationContext.registerBean(beanName, MongoClient.class,
                    () -> entry.client);
            log.info("Added MongoClient for tenant: {}", tenantId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create MongoClient for tenant: " + tenantId, e);
        }
    }

    @Override
    public void destroy() {
        for (Map.Entry<String, TenantEntry> entry : entries.entrySet()) {
            try {
                entry.getValue().close();
            } catch (Exception e) {
                log.warn("Error closing MongoClient for tenant: {}", entry.getKey(), e);
            }
        }
        entries.clear();
    }

    private static class TenantEntry {
        final MongoClient client;
        final String databaseName;

        TenantEntry(String uri) {
            ConnectionString cs = new ConnectionString(uri);
            this.databaseName = cs.getDatabase();
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(cs).build();
            this.client = MongoClients.create(settings);
        }

        MongoDatabase getDatabase() {
            return client.getDatabase(databaseName);
        }

        void close() {
            client.close();
        }
    }
}
