package com.github.eacryo.mongoflex.bean;

import com.github.eacryo.mongoflex.config.IdGenerator;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom ID generator: "{prefix}-{timestamp}-{seq}" format. / 自定义 ID 生成器："{prefix}-{timestamp}-{seq}" 格式。
 * <p>
 * Uses millisecond timestamp + atomic sequence to guarantee uniqueness within the same millisecond. / 使用毫秒时间戳 + 原子序列号保证同一毫秒内的唯一性。
 * <p>
 * Example output / 示例输出: {@code "user-1734567890123-1"}
 */
public class TimestampIdGenerator implements IdGenerator<String> {

    private final String prefix;
    private final AtomicLong counter = new AtomicLong(0);

    /**
     * No-arg constructor, defaults to "user" prefix. / 无参构造函数，默认前缀为 "user"。
     */
    public TimestampIdGenerator() {
        this("user");
    }

    /**
     * Constructor with custom prefix. / 带自定义前缀的构造函数。
     * <p>
     * Note: the framework always calls the no-arg constructor. / 注意：框架始终调用无参构造函数。
     */
    public TimestampIdGenerator(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public String create() {
        return prefix + "-" + System.currentTimeMillis() + "-" + counter.incrementAndGet();
    }
}
