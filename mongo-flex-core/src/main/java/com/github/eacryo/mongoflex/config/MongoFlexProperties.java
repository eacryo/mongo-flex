package com.github.eacryo.mongoflex.config;

import com.github.eacryo.mongoflex.constant.MongoFlexConstant;
import com.mongodb.ConnectionString;
import lombok.Data;
import org.slf4j.MDC;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@ConfigurationProperties(prefix = "mongo-flex")
public class MongoFlexProperties {

    private List<TenantConfig> tenants = new ArrayList<>();

    private boolean enableMultiTenants;
    private boolean toSnakeCase;
    private String uri;

    /** Cached database name for single-tenant mode / 单租户模式缓存的数据库名 */
    private volatile String cachedDatabase;
    /** Cached database names per tenant URI, lazily populated / 按租户 URI 懒加载缓存的数据库名 */
    private final Map<String, String> tenantDatabaseCache = new ConcurrentHashMap<>();

    /**
     * Get the database name from the MongoDB connection URI / 从 MongoDB 连接 URI 获取数据库名
     * <p>
     * {@code new ConnectionString(uri)} is not trivial string splitting — it does DNS SRV
     * resolution (blocking I/O), multiple option-parse passes, URL decoding, and allocates
     * multiple objects (MongoCredential, TagSet, MongoCompressor). Since the URI is
     * immutable at runtime, results are cached to avoid repeating this work on every
     * database operation.
     * <p>
     * {@code new ConnectionString(uri)} 不是简单的字符串分割——它会做 DNS SRV 解析（阻塞 I/O）、
     * 多轮 option 遍历解析、URL 解码、以及多个对象分配。URI 在运行时不可变，因此缓存结果避免
     * 每次数据库操作都重复这些工作。
     *
     * @return database name / 数据库名
     */
    public String getDatabaseFromUri() {
        if (!enableMultiTenants) {
            // Single-tenant: cache after first call / 单租户：首次调用后缓存
            if (cachedDatabase != null) {
                return cachedDatabase;
            }
            synchronized (this) {
                if (cachedDatabase != null) {
                    return cachedDatabase;
                }
                cachedDatabase = new ConnectionString(uri).getDatabase();
                return cachedDatabase;
            }
        } else {
            // Multi-tenant: cache per tenant URI / 多租户：按租户 URI 缓存
            String tenant = MDC.get(MongoFlexConstant.TENANT);
            for (TenantConfig tenantConfig : tenants) {
                if (tenantConfig.getName().equals(tenant)) {
                    String tenantUri = tenantConfig.getUri();
                    return tenantDatabaseCache.computeIfAbsent(tenantUri,
                            k -> new ConnectionString(tenantUri).getDatabase());
                }
            }
            throw new IllegalArgumentException("Tenant not found: " + tenant
                    + ", available: " + tenants.stream().map(TenantConfig::getName)
                    .reduce((a, b) -> a + ", " + b).orElse("none"));
        }
    }
}
