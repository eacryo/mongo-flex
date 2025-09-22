package com.github.eacryo.mongoflex.v2;

import com.mongodb.client.MongoDatabase;
import java.lang.reflect.Proxy;

public class MyRepositoryFactory {

    @SuppressWarnings("unchecked")
    public static <T> T getRepository(MongoDatabase database, Class<T> repositoryInterface) {
        return (T) Proxy.newProxyInstance(
            repositoryInterface.getClassLoader(),
            new Class<?>[]{repositoryInterface},
            new MyRepositoryProxyHandler<>(database, getEntityClass(repositoryInterface))
        );
    }
    
    // 辅助方法：从接口名推断出实体类名，这里简化处理，实际需要更健壮的逻辑
    private static Class<?> getEntityClass(Class<?> repositoryInterface) {
        String interfaceName = repositoryInterface.getSimpleName();
        String entityName = interfaceName.replace("Repository", "");
        try {
            return Class.forName(repositoryInterface.getPackage().getName() + "." + entityName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not find entity class for repository: " + repositoryInterface.getName(), e);
        }
    }
}
