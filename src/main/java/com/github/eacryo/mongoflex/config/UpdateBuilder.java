package com.github.eacryo.mongoflex.config;

import com.github.eacryo.mongoflex.util.ReflectUtil;
import org.springframework.data.mongodb.core.query.Update;

public class UpdateBuilder {

    // 内部Update对象
    private final Update update;

    /**
     * 私有构造函数
     */
    private UpdateBuilder() {
        this.update = new Update();
    }

    /**
     * 静态工厂方法 - 创建新的UpdateBuilder
     */
    public static UpdateBuilder builder() {
        return new UpdateBuilder();
    }

    public <T, R> UpdateBuilder set(SFunction<T, R> func, R value) {
        update.set(ReflectUtil.getFieldName(func), value);
        return this;
    }

    public Update build() {
        return update;
    }
}
