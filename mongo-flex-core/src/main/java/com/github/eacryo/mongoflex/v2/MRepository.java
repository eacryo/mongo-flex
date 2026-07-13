package com.github.eacryo.mongoflex.v2;

import org.springframework.stereotype.Repository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个接口为 mongo-flex Repository。
 * <p>
 * 标注了此注解的接口会在启动时被自动扫描到，框架为其生成 JDK 动态代理实现，
 * 注册为 Spring Bean 后可通过 {@code @Autowired} 注入使用。
 * <p>
 * 未标注此注解的接口不会被框架发现，注入时将抛出 {@code NoSuchBeanDefinitionException}。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @MRepository
 * public interface CharacterRepository extends MongoRepository<Character, String> {
 *
 *     @Find("{name: #{name}}")
 *     List<Character> findByName(@Param("name") String name);
 * }
 * }</pre>
 *
 * <p>
 * 此注解同时被 {@link Repository @Repository} 元注解标记，
 * 因此 Spring 会为其应用持久层异常翻译。
 *
 * @see MongoRepository
 * @see Mql
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repository
public @interface MRepository {
}
