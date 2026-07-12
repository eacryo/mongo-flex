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
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        // Create and register a RepositoryFactoryBean for each found interface / 为每一个找到的接口创建并注册一个 RepositoryFactoryBean
        for (Class<?> repositoryInterface : repositoryInterfaces) {
            // Create a RepositoryFactoryBean BeanDefinition / 创建一个 RepositoryFactoryBean 的 BeanDefinition
            RootBeanDefinition beanDefinition = new RootBeanDefinition(RepositoryFactoryBean.class);
            // Resolve entity class and ID class from the interface hierarchy / 从接口层级中解析实体类和 ID 类
            Type[] resolved = resolveMongoRepositoryTypes(repositoryInterface, new HashMap<>());
            Class<?> entityClass = null;
            Class<?> idClass = null;
            if (resolved != null) {
                entityClass = (Class<?>) resolved[0];
                idClass = (Class<?>) resolved[1];
                log.info("Entity type: {}, ID type: {}", entityClass.getTypeName(), idClass.getTypeName());
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

    /**
     * Recursively walk the interface hierarchy to find {@link MongoRepository} and resolve its type arguments. / 递归遍历接口层级，查找 {@link MongoRepository} 并解析其泛型参数。
     * <p>
     * Supports multi-level inheritance chains like: / 支持多级继承链，如：
     * <pre>{@code
     * interface BaseRepo<T, ID> extends MongoRepository<T, ID> {}
     * interface UserRepo extends BaseRepo<User, String> {}
     * }</pre>
     * <p>
     * The {@code typeVarMap} maps type variables declared at intermediate interfaces to the actual types
     * bound by the child interface, e.g. {@code T → User.class, ID → String.class}. / {@code typeVarMap} 将中间接口声明的类型变量映射到子接口绑定的实际类型。
     *
     * @param type      the current type to inspect (interface or class) / 当前要检查的类型（接口或类）
     * @param typeVarMap accumulated type variable → actual type mappings from parent levels / 从父级累积的类型变量 → 实际类型映射
     * @return resolved [entityType, idType] array, or null if MongoRepository is not found in this branch / 解析出的 [entityType, idType] 数组，如果该分支中未找到 MongoRepository 则返回 null
     */
    private static Type[] resolveMongoRepositoryTypes(Type type, Map<TypeVariable<?>, Type> typeVarMap) {
        Class<?> rawClass = getRawClass(type);

        // Build type variable → actual type mapping at this level / 构建当前层级的类型变量 → 实际类型映射
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            TypeVariable<?>[] typeParams = rawClass.getTypeParameters();
            Type[] actualArgs = pt.getActualTypeArguments();
            // Copy to avoid polluting parent map, then overlay current level / 复制避免污染父级映射，再覆盖当前层级
            typeVarMap = new HashMap<>(typeVarMap);
            for (int i = 0; i < typeParams.length; i++) {
                typeVarMap.put(typeParams[i], actualArgs[i]);
            }
        }

        // Check direct interfaces / 检查直接接口
        for (Type iface : rawClass.getGenericInterfaces()) {
            Class<?> ifaceRaw = getRawClass(iface);
            if (ifaceRaw == MongoRepository.class) {
                // Found it — resolve type variables using accumulated mapping / 找到了——用累积的映射解析类型变量
                if (iface instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) iface;
                    return new Type[]{
                            resolveTypeVariable(pt.getActualTypeArguments()[0], typeVarMap),
                            resolveTypeVariable(pt.getActualTypeArguments()[1], typeVarMap)
                    };
                }
                // Raw MongoRepository (no type args) — shouldn't happen but guard it / 原始 MongoRepository（无泛型参数）——不应该出现但做保护
                return null;
            }
            // Recurse into parent interface / 递归进入父接口
            Type[] result = resolveMongoRepositoryTypes(iface, typeVarMap);
            if (result != null) {
                return result;
            }
        }

        // Check superclass (for class-based inheritance chains) / 检查父类（支持基于类的继承链）
        Type superclass = rawClass.getGenericSuperclass();
        if (superclass != null && superclass != Object.class) {
            return resolveMongoRepositoryTypes(superclass, typeVarMap);
        }

        return null;
    }

    /**
     * Get the raw Class from a Type. / 从 Type 中获取原始 Class。
     */
    private static Class<?> getRawClass(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        }
        throw new IllegalArgumentException("Cannot resolve raw class from type: " + type
                + " / 无法从类型中解析原始类: " + type);
    }

    /**
     * Resolve a TypeVariable to its actual type using the accumulated mapping. / 使用累积的映射将 TypeVariable 解析为实际类型。
     * If the type is not a TypeVariable, return it as-is (already resolved). / 如果类型不是 TypeVariable，直接返回（已经解析完毕）。
     */
    private static Type resolveTypeVariable(Type type, Map<TypeVariable<?>, Type> typeVarMap) {
        if (type instanceof TypeVariable) {
            Type resolved = typeVarMap.get(type);
            // The resolved type might itself be a TypeVariable (nested generics), recurse / 解析出的类型可能仍是 TypeVariable（嵌套泛型），递归解析
            return resolved != null ? resolveTypeVariable(resolved, typeVarMap) : type;
        }
        return type;
    }
}
