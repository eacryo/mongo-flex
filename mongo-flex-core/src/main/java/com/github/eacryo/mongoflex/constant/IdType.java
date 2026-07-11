package com.github.eacryo.mongoflex.constant;

/**
 * ID 生成策略。
 * <p>
 * 与 {@link com.github.eacryo.mongoflex.annotation.CollectionId @CollectionId} 配合使用，
 * 决定实体插入时如何生成 {@code _id} 字段的值。
 *
 * <h3>各模式说明</h3>
 * <table>
 *   <tr><th>模式</th><th>行为</th><th>Java 类型</th></tr>
 *   <tr><td>{@link #NONE}</td><td>MongoDB 原生 ObjectId，由驱动自动生成</td><td>{@code String}（hex 格式，24 字符）</td></tr>
 *   <tr><td>{@link #ULID}</td><td>26 位字典序可排序唯一 ID，推荐用于新项目</td><td>{@code String}</td></tr>
 *   <tr><td>{@link #UUID}</td><td>标准 UUID（v4 随机）</td><td>{@code String}</td></tr>
 *   <tr><td>{@link #INPUT}</td><td>由用户提供的 {@link com.github.eacryo.mongoflex.config.IdGenerator IdGenerator} 生成</td><td>取决于生成器</td></tr>
 * </table>
 *
 * @see com.github.eacryo.mongoflex.annotation.CollectionId
 */
public enum IdType {
    /** MongoDB 原生 ObjectId，由驱动自动生成。Java 侧存储为 24 字符 hex 字符串。 */
    NONE(0),
    /** 26 位字典序可排序唯一 ID（ULID），推荐用于分布式系统中的有序主键。 */
    ULID(1),
    /** 标准 UUID v4 随机字符串，36 字符带连字符。 */
    UUID(2),
    /** 由用户实现 {@code IdGenerator} 接口自定义生成逻辑。 */
    INPUT(3);

    private final int key;

    IdType(int key) {
        this.key = key;
    }

    public int getKey() {
        return this.key;
    }
}
