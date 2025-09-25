package com.github.eacryo.mongoflex.v2;

import com.github.eacryo.mongoflex.config.MongoFlexProperties;
import com.github.eacryo.mongoflex.config.TenantConfig;
import com.github.eacryo.mongoflex.constant.MongoFlexConstant;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 在启动时根据多租户信息注入多个 MongoClient 实例，并注册到 Spring 容器中。
 */
@Component
public class DynamicMongoClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicMongoClient.class);
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
        ;
        return select(MongoFlexConstant.DEFAULT_TENANT_WHEN_DISABLE);
    }

    public MongoClient select(String tenant) {
        if (!StringUtils.hasText(tenant) || !clients.containsKey(tenant)) {
            throw new NullPointerException("cannot found MongoClient for tenant: " + tenant);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("using MongoClient for tenant {}", tenant);
        }
        return clients.get(tenant);
    }

    @PostConstruct
    //标注为private也能获取到
    private void initializeTemplates() {
        if (mongoFlexProperties.isEnableMultiTenants()) initWhenEnabled();
        else initWhenDisabled();
        LOGGER.info("MongoClientFactory initialized with {} tenants", clients.size());
    }


    private void initWhenEnabled() {
        // 从配置中获取所有租户的 MongoClient 信息
        for (TenantConfig config : mongoFlexProperties.getTenants()) {
            String tenantId = config.getName() +
                    (Objects.nonNull(config.getTablePrefix()) ? "_" + config.getTablePrefix() : "");
            String beanName = MONGO_CLIENT_PREFIX + tenantId;
            try {
                // 注册Bean
                genericApplicationContext.registerBean(beanName, MongoClient.class,
                        () -> MongoClients.create(config.getUri()));
                // 从 Spring 容器中获取对应的 MongoTemplate Bean
                MongoClient mongoClient = genericApplicationContext.getBean(beanName, MongoClient.class);
                clients.put(tenantId, mongoClient);
                LOGGER.info("Added MongoClient for tenant: {}", tenantId);
            } catch (Exception e) {
                LOGGER.error("Failed to get MongoClient for tenant: {}, error: {}", tenantId, e.getMessage());
            }
        }
    }

    private void initWhenDisabled() {
        //只注入一个MongoTemplate
        String tenantId = MongoFlexConstant.DEFAULT_TENANT_WHEN_DISABLE;
        String beanName = MONGO_CLIENT_PREFIX + tenantId;
        try {
            //这里应当自己注入MongoClient
                // 注册Bean
                genericApplicationContext.registerBean(beanName, MongoClient.class,
                        () -> MongoClients.create(mongoFlexProperties.getUri()));
                // 从 Spring 容器中获取对应的 MongoTemplate Bean
                MongoClient mongoClient = genericApplicationContext.getBean(beanName, MongoClient.class);
                clients.put(tenantId, mongoClient);
            LOGGER.info("Added MongoClient for tenant: {}", tenantId);
        } catch (Exception e) {
            LOGGER.error("Failed to get MongoClient for tenant: {}, error: {}", tenantId, e.getMessage());
        }
    }

}
