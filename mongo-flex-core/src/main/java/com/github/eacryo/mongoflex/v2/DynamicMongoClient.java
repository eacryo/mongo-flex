package com.github.eacryo.mongoflex.v2;

import com.github.eacryo.mongoflex.config.MongoFlexProperties;
import com.github.eacryo.mongoflex.config.TenantConfig;
import com.github.eacryo.mongoflex.constant.MongoFlexConstant;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 在启动时根据多租户信息注入多个 MongoClient 实例，并注册到 Spring 容器中。
 */
@Slf4j
public class DynamicMongoClient implements InitializingBean {
    private static final String MONGO_CLIENT_PREFIX = "mongoClient_";

    private Map<String, MongoClient> clients = new HashMap<>();

    @Autowired
    private GenericApplicationContext genericApplicationContext;

    @Autowired
    private MongoFlexProperties mongoFlexProperties;

    //无参构造
    public DynamicMongoClient() {
    }

    //final标记在方法如参上，明示这个参数不能被改变
    public DynamicMongoClient(final Map<String, MongoClient> clients) {
        this.clients = clients;
    }

    public MongoClient select() {
        if (mongoFlexProperties.isEnableMultiTenants()) {
            return select(MDC.get(MongoFlexConstant.TENANT));
        }
        return select(MongoFlexConstant.DEFAULT_TENANT_WHEN_DISABLE);
    }

    public MongoClient select(String tenant) {
        if (!StringUtils.hasText(tenant) || !clients.containsKey(tenant)) {
            throw new NullPointerException("cannot found MongoClient for tenant: " + tenant);
        }
        if (log.isDebugEnabled()) {
            log.debug("using MongoClient for tenant {}", tenant);
        }
        return clients.get(tenant);
    }

    @Override
    public void afterPropertiesSet() {
        initializeTemplates();
    }

    private void initializeTemplates() {
        if (mongoFlexProperties.isEnableMultiTenants()) initWhenEnabled();
        else initWhenDisabled();
        log.info("MongoClientFactory initialized with {} tenants", clients.size());
    }


    private void initWhenEnabled() {
        Map<String, Exception> errors = new HashMap<>();
        for (TenantConfig config : mongoFlexProperties.getTenants()) {
            String tenantId = config.getName() +
                    (Objects.nonNull(config.getTablePrefix()) ? "_" + config.getTablePrefix() : "");
            String beanName = MONGO_CLIENT_PREFIX + tenantId;
            try {
                MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                        .applyConnectionString(new ConnectionString(config.getUri())).build();
                genericApplicationContext.registerBean(beanName, MongoClient.class,
                        () -> MongoClients.create(mongoClientSettings));
                MongoClient mongoClient = genericApplicationContext.getBean(beanName, MongoClient.class);
                clients.put(tenantId, mongoClient);
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
        //只注入一个MongoClient
        String tenantId = MongoFlexConstant.DEFAULT_TENANT_WHEN_DISABLE;
        String beanName = MONGO_CLIENT_PREFIX + tenantId;
        try {
            MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(mongoFlexProperties.getUri())).build();
            // 注册Bean
            genericApplicationContext.registerBean(beanName, MongoClient.class,
                    () -> MongoClients.create(mongoClientSettings));
            // 从 Spring 容器中获取对应的 MongoTemplate Bean
            MongoClient mongoClient = genericApplicationContext.getBean(beanName, MongoClient.class);
            clients.put(tenantId, mongoClient);
            log.info("Added MongoClient for tenant: {}", tenantId);
        } catch (Exception e) {
            // 不再吞掉异常，直接抛出以让应用尽早失败并显示完整堆栈
            throw new RuntimeException("Failed to create MongoClient for tenant: " + tenantId, e);
        }
    }

}
