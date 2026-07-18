package com.github.eacryo.mongoflex.repository;

import com.github.eacryo.mongoflex.config.MongoFlexProperties;
import com.github.eacryo.mongoflex.config.TenantConfig;
import com.github.eacryo.mongoflex.constant.MongoFlexConstant;
import com.mongodb.client.MongoDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DynamicMongoClientTest {

    private DynamicMongoClient client;
    private MongoFlexProperties properties;

    @BeforeEach
    void setUp() throws Exception {
        properties = new MongoFlexProperties();
        properties.setEnableMultiTenants(true);

        client = new DynamicMongoClient();
        setField(client, "mongoFlexProperties", properties);
    }

    @AfterEach
    void tearDown() {
        client.destroy();
        MDC.clear();
    }

    @Test
    void registerTenantShouldAddTenant() {
        TenantConfig config = tenantConfig("test", "mongodb://localhost:27017/test_db");

        client.registerTenant(config);

        Set<String> tenants = client.listTenants();
        assertTrue(tenants.contains("test"));
        assertEquals(1, tenants.size());
    }

    @Test
    void registerTenantShouldReplaceExisting() {
        TenantConfig config1 = tenantConfig("test", "mongodb://localhost:27017/db1");
        TenantConfig config2 = tenantConfig("test", "mongodb://localhost:27017/db2");

        client.registerTenant(config1);
        client.registerTenant(config2);

        assertEquals(1, client.listTenants().size());

        MongoDatabase db = client.selectDatabase("test");
        assertEquals("db2", db.getName());
    }

    @Test
    void removeTenantShouldRemoveTenant() {
        client.registerTenant(tenantConfig("test", "mongodb://localhost:27017/test_db"));

        boolean removed = client.removeTenant("test");

        assertTrue(removed);
        assertTrue(client.listTenants().isEmpty());
    }

    @Test
    void removeTenantNonExistentShouldReturnFalse() {
        boolean removed = client.removeTenant("nonexistent");
        assertFalse(removed);
    }

    @Test
    void removeTenantNullShouldReturnFalse() {
        assertFalse(client.removeTenant(null));
        assertFalse(client.removeTenant(""));
    }

    @Test
    void listTenantsShouldReturnAll() {
        client.registerTenant(tenantConfig("a", "mongodb://localhost:27017/db_a"));
        client.registerTenant(tenantConfig("b", "mongodb://localhost:27017/db_b"));

        Set<String> tenants = client.listTenants();
        assertEquals(2, tenants.size());
        assertTrue(tenants.contains("a"));
        assertTrue(tenants.contains("b"));
    }

    @Test
    void selectShouldFindRegisteredTenant() {
        client.registerTenant(tenantConfig("test", "mongodb://localhost:27017/test_db"));

        assertNotNull(client.select("test"));
    }

    @Test
    void selectUnknownTenantShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> client.select("unknown"));
    }

    @Test
    void selectNullShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> client.select(null));
        assertThrows(IllegalArgumentException.class, () -> client.select(""));
    }

    @Test
    void selectDatabaseShouldReturnCorrectDatabase() {
        client.registerTenant(tenantConfig("test", "mongodb://localhost:27017/test_db"));

        MongoDatabase db = client.selectDatabase("test");
        assertNotNull(db);
        assertEquals("test_db", db.getName());
    }

    @Test
    void selectDatabaseUnknownTenantShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> client.selectDatabase("unknown"));
    }

    @Test
    void registerTenantNullNameShouldThrow() {
        TenantConfig config = new TenantConfig();
        config.setUri("mongodb://localhost:27017/db");

        assertThrows(IllegalArgumentException.class, () -> client.registerTenant(config));
    }

    @Test
    void registerTenantEmptyNameShouldThrow() {
        TenantConfig config = new TenantConfig();
        config.setName("");
        config.setUri("mongodb://localhost:27017/db");

        assertThrows(IllegalArgumentException.class, () -> client.registerTenant(config));
    }

    @Test
    void registerTenantNullUriShouldThrow() {
        TenantConfig config = new TenantConfig();
        config.setName("test");

        assertThrows(IllegalArgumentException.class, () -> client.registerTenant(config));
    }

    @Test
    void registerTenantEmptyUriShouldThrow() {
        TenantConfig config = new TenantConfig();
        config.setName("test");
        config.setUri("");

        assertThrows(IllegalArgumentException.class, () -> client.registerTenant(config));
    }

    @Test
    void destroyShouldCloseAllClients() {
        client.registerTenant(tenantConfig("a", "mongodb://localhost:27017/db_a"));
        client.registerTenant(tenantConfig("b", "mongodb://localhost:27017/db_b"));
        assertEquals(2, client.listTenants().size());

        client.destroy();

        assertTrue(client.listTenants().isEmpty());
    }

    @Test
    void selectWithMdcShouldUseCurrentTenant() {
        client.registerTenant(tenantConfig("remote", "mongodb://localhost:27017/remote_db"));

        MDC.put(MongoFlexConstant.TENANT, "remote");
        MongoDatabase db = client.selectDatabase();

        assertNotNull(db);
        assertEquals("remote_db", db.getName());
    }

    @Test
    void selectWithMdcWhenNoTenantSetShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> client.select());
    }

    @Test
    void registerAndRemoveLifecycleShouldWork() {
        assertTrue(client.listTenants().isEmpty());

        client.registerTenant(tenantConfig("t1", "mongodb://localhost:27017/db1"));
        assertEquals(1, client.listTenants().size());
        assertNotNull(client.select("t1"));

        client.registerTenant(tenantConfig("t2", "mongodb://localhost:27017/db2"));
        assertEquals(2, client.listTenants().size());

        client.removeTenant("t1");
        assertEquals(1, client.listTenants().size());
        assertFalse(client.listTenants().contains("t1"));
        assertTrue(client.listTenants().contains("t2"));

        client.removeTenant("t2");
        assertTrue(client.listTenants().isEmpty());
    }

    @Test
    void selectDatabaseAfterRemoveShouldThrow() {
        client.registerTenant(tenantConfig("test", "mongodb://localhost:27017/test_db"));
        client.removeTenant("test");

        assertThrows(IllegalArgumentException.class, () -> client.selectDatabase("test"));
    }

    private static TenantConfig tenantConfig(String name, String uri) {
        TenantConfig config = new TenantConfig();
        config.setName(name);
        config.setUri(uri);
        return config;
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = DynamicMongoClient.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
