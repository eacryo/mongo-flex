module com.github.eacryo.mongoflex {
    // 只导出需要对外暴露的包
    exports com.github.eacryo.mongoflex.annotation;
    exports com.github.eacryo.mongoflex.aspect;
    exports com.github.eacryo.mongoflex.config;
    exports com.github.eacryo.mongoflex.constant;
    exports com.github.eacryo.mongoflex.entity;
    // 不导出util包，ReflectUtil就不会被外部模块访问
    // exports com.github.eacryo.mongoflex.util;  // 不要写这一行

    // 依赖的其他模块
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.data.mongodb;
    requires org.slf4j;
    requires spring.core;
    requires org.aspectj.weaver;
    requires com.github.f4b6a3.ulid;
    requires spring.data.commons;
    requires jakarta.annotation;
    requires spring.beans;
    requires spring.boot;
    // ... 其他依赖
}
