package com.github.eacryo.mongoflex.config;

import com.github.eacryo.mongoflex.constant.MongoFlexConstant;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


@Component
public class MongoTemplateFactory {

    //不清楚用这个好在哪里，从mybatis-plus的代码中看到的
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoTemplateFactory.class);

    private static final String MONGO_TEMPLATE_PREFIX = "mongoTemplate_";

    private Map<String, MongoTemplate> templates = new HashMap<>();

    @Autowired
    private GenericApplicationContext genericApplicationContext;

    @Autowired
    private MongoFlexConfig properties;

    //无参构造
    public MongoTemplateFactory() {
    }

    //final标记在方法如参上，明示这个参数不能被改变
    public MongoTemplateFactory(final Map<String, MongoTemplate> templates) {
        this.templates = templates;
    }

    public MongoTemplate select() {
        //多租户有两种方案,一种是使用不同的数据库.另一种是相同的数据库但不同的表
        //这里需要兼容两种方案
        //请求头拆分成两部分tenant和tenantTablePrefix,前者不可为空,后者可为空
        return select(MDC.get(MongoFlexConstant.TENANT));
    }

    //StringUtils.isEmpty()已经废弃，使用StringUtils中的hasLength(String)或者hasText(String) 方法来替换
    public MongoTemplate select(String tenant) {
        if (!StringUtils.hasText(tenant) || !templates.containsKey(tenant)) {
            throw new NullPointerException("cannot found MongoTemplate for tenant: " + tenant);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("using mongoTemplate for tenant {}", tenant);
        }
        return templates.get(tenant);
    }

    @PostConstruct
    public void initializeTemplates() {
        // 从配置中获取所有租户的 MongoTemplate
        for (TenantConfig config : properties.getTenants()) {
            String tenantId = config.getName() +
                    (Objects.nonNull(config.getTablePrefix()) ? "_" + config.getTablePrefix() : "");
            String beanName = MONGO_TEMPLATE_PREFIX + tenantId;
            try {
                // 注册Bean
                genericApplicationContext.registerBean(beanName, MongoTemplate.class,
                        () -> new MongoTemplate(new SimpleMongoClientDatabaseFactory(config.getUri())));
                // 从 Spring 容器中获取对应的 MongoTemplate Bean
                MongoTemplate mongoTemplate = genericApplicationContext.getBean(beanName, MongoTemplate.class);
                templates.put(tenantId, mongoTemplate);
                LOGGER.info("Added MongoTemplate for tenant: {}", tenantId);
            } catch (Exception e) {
                LOGGER.error("Failed to get MongoTemplate for tenant: {}, error: {}", tenantId, e.getMessage());
            }
        }
        LOGGER.info("DynamicMongoTemplate initialized with {} tenants", templates.size());
    }
}
