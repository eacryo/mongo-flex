package com.github.eacryo.mongoflex.repository;

import java.lang.reflect.Method;

/**
 * Pluggable method handler for {@link com.github.eacryo.mongoflex.annotation.MRepository} proxy dispatch /
 * 可插拔的方法处理器，用于 {@link com.github.eacryo.mongoflex.annotation.MRepository} 代理分发。
 * <p>
 * Each handler type (annotation-driven, repository-inherited, future extensions)
 * implements this interface. The proxy handler iterates through a list of
 * handlers, calling the first one whose {@link #supports} returns true /
 * 每种处理器类型（注解驱动、Repository 继承、未来扩展）实现此接口。
 * 代理处理器遍历 handler 列表，调用第一个 {@link #supports} 返回 true 的 handler。
 * <p>
 * Currently the proxy dispatch uses inline if-else; this interface provides
 * the extension point for future plugin-based architecture per Phase 6 /
 * 当前代理分发使用内联 if-else；此接口为 Phase 6 的可插拔架构提供扩展点。
 */
public interface MethodHandler {

    /**
     * Whether this handler can process the given method /
     * 此 handler 是否能处理给定方法
     *
     * @param method the intercepted method / 被拦截的方法
     * @return true if this handler should process the method / 是否处理此方法
     */
    boolean supports(Method method);

    /**
     * Execute the method and return the result /
     * 执行方法并返回结果
     *
     * @param method the intercepted method / 被拦截的方法
     * @param args   method arguments / 方法参数
     * @return the return value of the method / 方法返回值
     * @throws Exception on execution errors / 执行异常
     */
    Object invoke(Method method, Object[] args) throws Exception;
}
