package com.github.eacryo.mongoflex.v2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Slf4j
public class RepositoryRegistrar implements ImportBeanDefinitionRegistrar {

    private static final AnnotationTypeFilter annotationFilter = new AnnotationTypeFilter(MRepository.class);

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        Set<Class<?>> repositoryInterfaces = new HashSet<>();

        if (registry instanceof ConfigurableListableBeanFactory) {
            List<String> packages = AutoConfigurationPackages.get((ConfigurableListableBeanFactory) registry);
            if (!packages.isEmpty()) {
                for (String pkg : packages) {
                    repositoryInterfaces.addAll(scanRepositoryInterfaces(pkg));
                }
            }
        }

        if (repositoryInterfaces.isEmpty()) {
            String basePackage = ClassUtils.getPackageName(importingClassMetadata.getClassName());
            repositoryInterfaces = scanRepositoryInterfaces(basePackage);
        }

        // 为每一个找到的接口创建并注册一个 RepositoryFactoryBean
        for (Class<?> repositoryInterface : repositoryInterfaces) {
            // 创建一个 RepositoryFactoryBean 的 BeanDefinition
            RootBeanDefinition beanDefinition = new RootBeanDefinition(RepositoryFactoryBean.class);
            Class<?> entityClass = null;
            Class<?> idClass = null;
            //获取父接口的信息
            for (Type type : repositoryInterface.getGenericInterfaces()) {
                if (type instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) type;
                    Type rawType = pt.getRawType();
                    if (pt.getRawType().getTypeName().equals(MongoRepository.class.getName())){
                        Type entityType = pt.getActualTypeArguments()[0];
                        Type idType = pt.getActualTypeArguments()[1];
                        entityClass = (Class<?>) entityType;
                        idClass = (Class<?>) idType;
                        log.info("Entity type: {}, ID type: {}", entityType.getTypeName(), idType.getTypeName());
                    }
                }
            }
            // 将接口类作为构造函数参数
            beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(repositoryInterface);
            beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(entityClass);
            beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(idClass);
            // 生成一个 Bean 名称，通常是接口名首字母小写
            String beanName = repositoryInterface.getSimpleName().substring(0, 1).toLowerCase() + repositoryInterface.getSimpleName().substring(1);

            // 检查 Bean 名称是否已存在，避免冲突
            if (!registry.containsBeanDefinition(beanName)) {
                registry.registerBeanDefinition(beanName, beanDefinition);
            }
        }
    }

    private Set<Class<?>> scanRepositoryInterfaces(String basePackage) {
        Set<Class<?>> repositoryInterfaces = new HashSet<>();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(resolver);

        try {
            // 构建扫描路径，查找所有类文件
            String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                                       basePackage.replace('.', '/') + "/**/*.class";

            // 遍历所有资源（类文件）
            for (Resource resource : resolver.getResources(packageSearchPath)) {
                if (resource.isReadable()) {
                    MetadataReader reader = readerFactory.getMetadataReader(resource);

                    // 检查类是否是接口，并且是否被 @MRepository 注解
                    if (reader.getClassMetadata().isInterface() &&
                        annotationFilter.match(reader, readerFactory)) {

                        // 将找到的接口添加到集合中
                        repositoryInterfaces.add(Class.forName(reader.getClassMetadata().getClassName()));
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            // 应该抛出运行时异常，阻止应用启动
            throw new RuntimeException("Error scanning for repository interfaces.", e);
        }
        return repositoryInterfaces;
    }
}
