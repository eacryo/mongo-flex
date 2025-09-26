package com.github.eacryo.mongoflex.v2;


import com.github.eacryo.mongoflex.config.MongoFlexProperties;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;


import java.lang.reflect.Proxy;

public class RepositoryFactoryBean<T, E, ID> implements FactoryBean<T> {

    // 注入 MongoDB 客户端
    @Autowired
    private DynamicMongoClient mongoClient;
    @Autowired
    private JacksonDocumentConverter jacksonDocumentConverter;
    @Autowired
    private MongoFlexProperties mongoFlexProperties;

    private final Class<T> repositoryInterface;
    private final Class<E> entityClass;
    private final Class<ID> idClass;

    public RepositoryFactoryBean(Class<T> repositoryInterface, Class<E> entityClass, Class<ID> idClass) {
        this.repositoryInterface = repositoryInterface;
        this.entityClass = entityClass;
        this.idClass = idClass;
    }


    @Override
    @SuppressWarnings({"unchecked", "resource"})
    public T getObject() {
        //TODO: 这里应该根据注解或者配置来决定数据库和集合
        BaseRepositoryV2<E, ID> baseRepository = new BaseRepositoryV2<>(
                mongoClient.select().getDatabase(mongoFlexProperties.getDatabaseFromUri()),
                "character",
                entityClass, jacksonDocumentConverter
        );
        // 使用动态代理为接口生成实现
        return (T) Proxy.newProxyInstance(
                repositoryInterface.getClassLoader(),
                new Class<?>[]{repositoryInterface},
                //TODO：这里应当考虑多租户的情况
                new MyRepositoryProxyHandler(
                        mongoClient.select().getDatabase(mongoFlexProperties.getDatabaseFromUri()),
                        jacksonDocumentConverter,
                        baseRepository)
        );
    }

    @Override
    public Class<?> getObjectType() {
        return repositoryInterface;
    }
}
