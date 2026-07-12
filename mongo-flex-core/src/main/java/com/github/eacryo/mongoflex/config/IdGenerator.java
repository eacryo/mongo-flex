package com.github.eacryo.mongoflex.config;

/**
 * Custom ID generator interface. / 自定义 ID 生成器接口。
 * <p>
 * Users implement this interface to provide custom ID generation logic,
 * then reference the implementation class in {@code @CollectionId(generatorClass = ...)}. / 用户实现此接口提供自定义 ID 生成逻辑，然后在 {@code @CollectionId(generatorClass = ...)} 中引用实现类。
 *
 * @param <T> the type of the generated ID / 生成的 ID 的类型
 */
@FunctionalInterface
public interface IdGenerator<T> {
    /**
     * Generate a new unique ID. / 生成一个新的唯一 ID。
     *
     * @return a new ID / 新的 ID
     */
    T create();

    /**
     * Sentinel type indicating no custom generator class is specified in annotation. / 哨兵类型，表示注解中未指定自定义生成器类。
     * Do not use this class directly. / 不要直接使用此类。
     */
    final class None implements IdGenerator<Object> {
        private None() {}
        @Override
        public Object create() {
            throw new UnsupportedOperationException("None is a sentinel, not a real generator / None 是哨兵，不是真正的生成器");
        }
    }
}
