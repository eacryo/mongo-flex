package com.github.eacryo.mongoflex.v2;


import com.github.eacryo.mongoflex.annotation.CollectionName;
import com.github.eacryo.mongoflex.config.DateValueProvider;
import com.github.eacryo.mongoflex.config.IdGenerator;
import com.github.eacryo.mongoflex.config.MongoFlexProperties;
import com.github.eacryo.mongoflex.convertor.MongoMappingConvertor;
import com.github.eacryo.mongoflex.strategy.ExecutorProxy;
import com.mongodb.client.MongoDatabase;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;


import java.lang.reflect.Proxy;
import java.util.function.Supplier;

public class RepositoryFactoryBean<T, E, ID> implements FactoryBean<T> {

    // 注入 MongoDB 客户端
    @Autowired
    private DynamicMongoClient mongoClient;
    @Autowired
    private MongoMappingConvertor mongoMappingConvertor;
    @Autowired
    private MongoFlexProperties mongoFlexProperties;
    @Autowired
    private ExecutorProxy executorProxy;
    @Autowired(required = false)
    private IdGenerator<?> idGenerator;
    @Autowired(required = false)
    private DateValueProvider dateValueProvider;

    Supplier<MongoDatabase> dbSupplier = () ->
        mongoClient.select().getDatabase(mongoFlexProperties.getDatabaseFromUri());

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
        SimpleMongoRepository<E, ID> baseRepository = new SimpleMongoRepository<>(
                dbSupplier,
                this.getCollectionName(entityClass),
                entityClass, mongoMappingConvertor, idGenerator, dateValueProvider
        );
        // 使用动态代理为接口生成实现
        return (T) Proxy.newProxyInstance(
                repositoryInterface.getClassLoader(),
                new Class<?>[]{repositoryInterface},
                new MyRepositoryProxyHandler<>(
                        repositoryInterface,
                        dbSupplier,
                        mongoMappingConvertor,
                        baseRepository,
                        executorProxy)
        );
    }

    @Override
    public Class<?> getObjectType() {
        return repositoryInterface;
    }

    private String getCollectionName(Class<E> entityClass) {
        if (entityClass.isAnnotationPresent(CollectionName.class)){
            CollectionName annotation = entityClass.getAnnotation(CollectionName.class);
            return annotation.value();
        } else {
            throw new RuntimeException("EntityClass " + entityClass.getName() + " is not annotated with @CollectionName");
        }
    }
}
