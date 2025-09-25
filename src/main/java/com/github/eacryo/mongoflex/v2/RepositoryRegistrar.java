package com.github.eacryo.mongoflex.v2;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import java.io.IOException;

public class RepositoryRegistrar implements ImportBeanDefinitionRegistrar {

    private static final TypeFilter REPOSITORY_FILTER = new AnnotationTypeFilter(MRepository.class);

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        // 假设你的接口都在 "com.github.eacryo" 包下
        String basePackage = "com.github.eacryo";

        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(resolver);

        try {
            String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                                       basePackage.replace('.', '/') + "/**/*.class";
            Resource[] resources = resolver.getResources(packageSearchPath);

            for (Resource resource : resources) {
                MetadataReader reader = readerFactory.getMetadataReader(resource);
                if (reader.getClassMetadata().isInterface() && REPOSITORY_FILTER.match(reader, readerFactory)) {
                    // 找到一个被 @MRepository 注解的接口
                    Class<?> repositoryInterface = Class.forName(reader.getClassMetadata().getClassName());

                    // 创建一个 FactoryBean 的 BeanDefinition
                    RootBeanDefinition beanDefinition = new RootBeanDefinition(RepositoryFactoryBean.class);
                    beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(repositoryInterface);

                    // 注册 Bean 到 Spring 容器中
                    String beanName = repositoryInterface.getSimpleName().substring(0, 1).toLowerCase() + repositoryInterface.getSimpleName().substring(1);
                    registry.registerBeanDefinition(beanName, beanDefinition);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Error registering ORM repositories", e);
        }
    }
}
