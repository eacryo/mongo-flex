package com.github.eacryo.mongoflex.v2;


import org.bson.Document;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;


import java.lang.reflect.Proxy;

public class MyOrmFactoryBean<T, E> implements FactoryBean<T> {

    // 注入 MongoDB 客户端
    @Autowired
    private DynamicMongoClient mongoClient;

    private final Class<T> repositoryInterface;

    public MyOrmFactoryBean(Class<T> repositoryInterface) {
        this.repositoryInterface = repositoryInterface;
    }


    @Override
    @SuppressWarnings("unchecked")
    public T getObject() {
        // 使用动态代理为接口生成实现
        return (T) Proxy.newProxyInstance(
                repositoryInterface.getClassLoader(),
                new Class<?>[]{repositoryInterface},
                //TODO：这里数据库和类型都应该传入，不要写死
                new MyRepositoryProxyHandler(mongoClient.select().getDatabase("mongo_flex"), Object.class)
        );
    }

    @Override
    public Class<?> getObjectType() {
        return repositoryInterface;
    }
}
