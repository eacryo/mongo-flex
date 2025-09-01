package com.github.eacryo.mongoflex.lambda;

import com.github.eacryo.mongoflex.util.ReflectUtil;
import org.springframework.data.mongodb.core.query.Update;

import java.lang.reflect.Field;
import java.util.List;

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

    public static <T> Update from(T entity) {
        List<Field> fields = ReflectUtil.getAllFieldsIncludingInherited(entity.getClass());
        Update update = new Update();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(entity);
                if (value != null) {
                    update.set(field.getName(), value);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("无法访问字段: " + field.getName(), e);
            }
        }
        return update;
    }

    public <T, R> UpdateBuilder set(SFunction<T, R> func, R value) {
        update.set(ReflectUtil.getFieldName(func), value);
        return this;
    }

    public Update build() {
        return update;
    }
}
