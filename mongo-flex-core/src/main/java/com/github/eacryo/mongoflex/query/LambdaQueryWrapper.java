package com.github.eacryo.mongoflex.query;

import com.github.eacryo.mongoflex.convertor.MongoMappingConvertor;
import com.github.eacryo.mongoflex.util.FieldPath;
import com.github.eacryo.mongoflex.util.SFunction;
import com.github.eacryo.mongoflex.util.ReflectUtil;

import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A Lambda-based query wrapper similar to MyBatis-Plus's LambdaQueryWrapper /
 * 基于 Lambda 的查询构造器，类似 MyBatis-Plus 的 LambdaQueryWrapper。
 * <p>
 * Example usage / 使用示例:
 * <pre>{@code
 * LambdaQueryWrapper<User> w = new LambdaQueryWrapper<>(User.class);
 * w.eq(User::getUserName, "Tom");
 * }</pre>
 */
public class LambdaQueryWrapper<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LambdaQueryWrapper.class);

    private final List<Condition> conditions = new ArrayList<>();
    private Class<T> entityClass;

    public LambdaQueryWrapper() {
    }

    public LambdaQueryWrapper(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    public Class<T> getEntityClass() {
        return entityClass;
    }

    public void setEntityClass(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    /**
     * Render this wrapper as a MongoDB Bson filter via {@link MongoBsonRenderer} /
     * 通过 {@link MongoBsonRenderer} 将此 wrapper 渲染为 MongoDB Bson 过滤器
     */
    public Bson toBson(MongoMappingConvertor convertor) {
        return MongoBsonRenderer.render(this, convertor);
    }

    public <R> LambdaQueryWrapper<T> eq(SFunction<T, R> field, R value) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.EQ, value, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    public <R> LambdaQueryWrapper<T> ne(SFunction<T, R> field, R value) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.NE, value, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    public <R> LambdaQueryWrapper<T> gt(SFunction<T, R> field, R value) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.GT, value, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    public <R> LambdaQueryWrapper<T> lt(SFunction<T, R> field, R value) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.LT, value, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    public <R> LambdaQueryWrapper<T> gte(SFunction<T, R> field, R value) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.GTE, value, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    public <R> LambdaQueryWrapper<T> lte(SFunction<T, R> field, R value) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.LTE, value, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    public <R> LambdaQueryWrapper<T> regex(SFunction<T, R> field, String pattern) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.REGEX, pattern, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    public <R> LambdaQueryWrapper<T> in(SFunction<T, R> field, Collection<?> values) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.IN, values, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    public <R> LambdaQueryWrapper<T> nin(SFunction<T, R> field, Collection<?> values) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.NIN, values, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    public LambdaQueryWrapper<T> exists(SFunction<T, ?> field, boolean value) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.EXISTS, value, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    public <R> LambdaQueryWrapper<T> all(SFunction<T, R> field, Collection<?> values) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.ALL, values, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    public LambdaQueryWrapper<T> size(SFunction<T, ?> field, int value) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.SIZE, value, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    public LambdaQueryWrapper<T> elemMatch(SFunction<T, ?> field, LambdaQueryWrapper<?> subWrapper) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.ELEM_MATCH, subWrapper, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    /**
     * SQL-style LIKE: supports * and % as wildcards, automatically converted to regex.
     * Example: like(User::getName, "*Tom*") → { name: { $regex: ".*Tom.*" } }
     */
    public <R> LambdaQueryWrapper<T> like(SFunction<T, R> field, String pattern) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.LIKE, pattern, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    /**
     * SQL-style NOT LIKE: negated LIKE, same wildcard conversion.
     */
    public <R> LambdaQueryWrapper<T> notLike(SFunction<T, R> field, String pattern) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.NOT_LIKE, pattern, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    /**
     * Range query: field BETWEEN start AND end → { field: { $gte: start, $lte: end } }
     */
    public <R> LambdaQueryWrapper<T> between(SFunction<T, R> field, R start, R end) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.BETWEEN, Arrays.asList(start, end), ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    /**
     * IS NULL → { field: null }
     * Matches documents where the field is null or does not exist (equivalent to SQL IS NULL).
     * Use {@link #exists(SFunction, boolean)} with false for $exists: false semantics.
     */
    public LambdaQueryWrapper<T> isNull(SFunction<T, ?> field) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.IS_NULL, null, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    /**
     * IS NOT NULL → { field: { $ne: null } }
     * Matches documents where the field exists and has a non-null value (equivalent to SQL IS NOT NULL).
     * Use {@link #exists(SFunction, boolean)} with true for $exists: true semantics.
     */
    public LambdaQueryWrapper<T> isNotNull(SFunction<T, ?> field) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.IS_NOT_NULL, null, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    /**
     * Logical NOT: negates a sub-query.
     * No field parameter — implClass is null.
     * Example: not(w -> w.eq(User::getName, "Tom")) → { $nor: [...] }
     */
    public LambdaQueryWrapper<T> not(LambdaQueryWrapper<T> subWrapper) {
        Objects.requireNonNull(subWrapper, "subWrapper must not be null");
        conditions.add(new Condition(null, Operator.NOT, subWrapper, null));
        return this;
    }

    // ──── Nested boolean groups / 嵌套布尔分组 ────
    // These methods attach a sub-wrapper as a single logical unit, enabling arbitrary
    // boolean composition such as A AND (B OR C) — impossible with the flat or()
    // separator, which only splits the top-level condition list into OR groups.
    // 这些方法把子 wrapper 作为一个整体逻辑单元挂载，支持任意布尔组合（如 A AND (B OR C)），
    // 这是扁平的 or() 分隔符无法表达的——or() 只能把顶层条件列表切成若干 OR 组。

    /**
     * AND-nested group: attaches a sub-wrapper that is ANDed with the sibling conditions. /
     * AND 嵌套分组：挂载一个与同级条件按 AND 组合的子 wrapper。
     * <p>
     * Example / 示例：{@code w.eq(User::getStatus, "active").and(x -> x.eq(User::getAge, 18).or().eq(User::getAge, 21))}
     * → {@code {status: "active", $and: [{$or: [{age: 18}, {age: 21}]}]}} — i.e. status = active AND (age = 18 OR age = 21).
     */
    public LambdaQueryWrapper<T> and(Consumer<LambdaQueryWrapper<T>> consumer) {
        Objects.requireNonNull(consumer, "consumer must not be null");
        LambdaQueryWrapper<T> subWrapper = new LambdaQueryWrapper<>();
        consumer.accept(subWrapper);
        conditions.add(new Condition(null, Operator.AND, subWrapper, null));
        return this;
    }

    /**
     * AND-nested group from an existing wrapper — the wrapper is attached as-is and ANDed
     * with the sibling conditions. / 以现有 wrapper 构造 AND 嵌套分组——原样挂载并与同级条件按 AND 组合。
     */
    public LambdaQueryWrapper<T> and(LambdaQueryWrapper<T> wrapper) {
        Objects.requireNonNull(wrapper, "wrapper must not be null");
        conditions.add(new Condition(null, Operator.AND, wrapper, null));
        return this;
    }

    /**
     * OR-nested group: attaches a sub-wrapper that is ORed with the sibling conditions. /
     * OR 嵌套分组：挂载一个与同级条件按 OR 组合的子 wrapper。
     * <p>
     * Example / 示例：{@code w.or(x -> x.eq(User::getAge, 18).eq(User::getGender, "M"))}
     * → {@code {$or: [{age: 18, gender: "M"}]}} — i.e. (age = 18 AND gender = "M").
     * <p>
     * Note the difference from {@link #or()} / 与 {@link #or()} 的区别：{@code or()} 是顶层分隔符，
     * 把整个条件列表切成 OR 组；{@code or(consumer)} 只把子分组与同级条件 OR 起来，可任意嵌套。
     */
    public LambdaQueryWrapper<T> or(Consumer<LambdaQueryWrapper<T>> consumer) {
        Objects.requireNonNull(consumer, "consumer must not be null");
        LambdaQueryWrapper<T> subWrapper = new LambdaQueryWrapper<>();
        consumer.accept(subWrapper);
        conditions.add(new Condition(null, Operator.OR, subWrapper, null));
        return this;
    }

    /**
     * NOT-nested group: attaches a sub-wrapper that is negated ({@code $nor}) against the
     * sibling conditions. / NOT 嵌套分组：挂载一个被取反（{@code $nor}）的子 wrapper。
     * <p>
     * Example / 示例：{@code w.eq(User::getStatus, "active").not(x -> x.eq(User::getAge, 18).or().eq(User::getAge, 21))}
     * → {@code {status: "active", $nor: [{$or: [{age: 18}, {age: 21}]}]}} — status = active AND NOT (age = 18 OR age = 21).
     */
    public LambdaQueryWrapper<T> not(Consumer<LambdaQueryWrapper<T>> consumer) {
        Objects.requireNonNull(consumer, "consumer must not be null");
        LambdaQueryWrapper<T> subWrapper = new LambdaQueryWrapper<>();
        consumer.accept(subWrapper);
        conditions.add(new Condition(null, Operator.NOT, subWrapper, null));
        return this;
    }

    /**
     * Modulo: { field: { $mod: [divisor, remainder] } }
     */
    public <R> LambdaQueryWrapper<T> mod(SFunction<T, R> field, int divisor, int remainder) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.MOD, Arrays.asList(divisor, remainder), ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    /**
     * BSON type check: { field: { $type: "string" } } or { field: { $type: "int" } }
     * Common type names: "string", "int", "double", "array", "objectId", "bool", "date"
     */
    public LambdaQueryWrapper<T> type(SFunction<T, ?> field, String bsonTypeName) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.TYPE, bsonTypeName, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    /**
     * Whether the wrapper contains at least one effective (non-empty) condition, recursing
     * into nested AND/OR/NOT groups. OR separators and empty nested groups are ignored. /
     * 判断 wrapper 是否包含至少一个有效（非空）条件，会递归进入嵌套的 AND/OR/NOT 分组。
     * OR 分隔符和空的嵌套分组会被忽略。
     * <p>
     * Used by the repository layer to guard destructive operations (delete/update) against
     * full-collection execution. / 供 Repository 层用于拦截破坏性操作（delete/update）
     * 误触全集合执行。
     */
    public static boolean hasEffectiveConditions(LambdaQueryWrapper<?> wrapper) {
        for (Condition c : wrapper.getConditions()) {
            if (c.isOrSeparator()) {
                continue;
            }
            Operator op = c.operator();
            if (op == Operator.AND || op == Operator.OR || op == Operator.NOT) {
                Object value = c.value();
                if (value instanceof LambdaQueryWrapper
                        && hasEffectiveConditions((LambdaQueryWrapper<?>) value)) {
                    return true;
                }
                continue;
            }
            return true;
        }
        return false;
    }

    // ---- FieldPath overloads — nested dot-notation queries / FieldPath 重载——嵌套点号查询 ----

    /**
     * Append a condition addressed by a nested {@link FieldPath}. The Java dot path is
     * stored on the {@link Condition} and mapped segment-by-segment to the MongoDB field
     * path at render time. / 追加一个以嵌套 {@link FieldPath} 定位的条件。Java 点号路径存入
     * {@link Condition}，渲染时逐段映射为 MongoDB 字段路径。
     */
    private LambdaQueryWrapper<T> addPathCondition(FieldPath<T, ?> path, Operator operator, Object value) {
        Objects.requireNonNull(path, "path must not be null");
        conditions.add(new Condition(path.javaPath(), operator, value, path.rootImplClass()));
        return this;
    }

    /**
     * Nested-path equality, e.g. {@code eq(FieldPath.of(User::getAddress, Address::getCity), "NY")}
     * → {@code {"address.city": "NY"}}. / 嵌套路径等值查询，如
     * {@code eq(FieldPath.of(User::getAddress, Address::getCity), "NY")} → {@code {"address.city": "NY"}}。
     */
    public <R> LambdaQueryWrapper<T> eq(FieldPath<T, R> path, R value) {
        return addPathCondition(path, Operator.EQ, value);
    }

    /** Nested-path not-equal / 嵌套路径不等值查询 */
    public <R> LambdaQueryWrapper<T> ne(FieldPath<T, R> path, R value) {
        return addPathCondition(path, Operator.NE, value);
    }

    /** Nested-path greater-than / 嵌套路径大于查询 */
    public <R> LambdaQueryWrapper<T> gt(FieldPath<T, R> path, R value) {
        return addPathCondition(path, Operator.GT, value);
    }

    /** Nested-path less-than / 嵌套路径小于查询 */
    public <R> LambdaQueryWrapper<T> lt(FieldPath<T, R> path, R value) {
        return addPathCondition(path, Operator.LT, value);
    }

    /** Nested-path greater-than-or-equal / 嵌套路径大于等于查询 */
    public <R> LambdaQueryWrapper<T> gte(FieldPath<T, R> path, R value) {
        return addPathCondition(path, Operator.GTE, value);
    }

    /** Nested-path less-than-or-equal / 嵌套路径小于等于查询 */
    public <R> LambdaQueryWrapper<T> lte(FieldPath<T, R> path, R value) {
        return addPathCondition(path, Operator.LTE, value);
    }

    /** Nested-path regex match / 嵌套路径正则查询 */
    public LambdaQueryWrapper<T> regex(FieldPath<T, ?> path, String pattern) {
        return addPathCondition(path, Operator.REGEX, pattern);
    }

    /** Nested-path $in / 嵌套路径 $in 查询 */
    public LambdaQueryWrapper<T> in(FieldPath<T, ?> path, Collection<?> values) {
        return addPathCondition(path, Operator.IN, values);
    }

    /** Nested-path $nin / 嵌套路径 $nin 查询 */
    public LambdaQueryWrapper<T> nin(FieldPath<T, ?> path, Collection<?> values) {
        return addPathCondition(path, Operator.NIN, values);
    }

    /** Nested-path $exists / 嵌套路径 $exists 查询 */
    public LambdaQueryWrapper<T> exists(FieldPath<T, ?> path, boolean value) {
        return addPathCondition(path, Operator.EXISTS, value);
    }

    /** Nested-path $all / 嵌套路径 $all 查询 */
    public LambdaQueryWrapper<T> all(FieldPath<T, ?> path, Collection<?> values) {
        return addPathCondition(path, Operator.ALL, values);
    }

    /** Nested-path $size / 嵌套路径 $size 查询 */
    public LambdaQueryWrapper<T> size(FieldPath<T, ?> path, int value) {
        return addPathCondition(path, Operator.SIZE, value);
    }

    /** Nested-path $elemMatch / 嵌套路径 $elemMatch 查询 */
    public LambdaQueryWrapper<T> elemMatch(FieldPath<T, ?> path, LambdaQueryWrapper<?> subWrapper) {
        return addPathCondition(path, Operator.ELEM_MATCH, subWrapper);
    }

    /** Nested-path LIKE (wildcards * and % converted to regex) / 嵌套路径 LIKE 查询（* 和 % 通配符自动转正则） */
    public LambdaQueryWrapper<T> like(FieldPath<T, ?> path, String pattern) {
        return addPathCondition(path, Operator.LIKE, pattern);
    }

    /** Nested-path NOT LIKE / 嵌套路径 NOT LIKE 查询 */
    public LambdaQueryWrapper<T> notLike(FieldPath<T, ?> path, String pattern) {
        return addPathCondition(path, Operator.NOT_LIKE, pattern);
    }

    /** Nested-path BETWEEN (inclusive) / 嵌套路径 BETWEEN 范围查询（含边界） */
    public <R> LambdaQueryWrapper<T> between(FieldPath<T, R> path, R start, R end) {
        return addPathCondition(path, Operator.BETWEEN, Arrays.asList(start, end));
    }

    /** Nested-path IS NULL / 嵌套路径 IS NULL 查询 */
    public LambdaQueryWrapper<T> isNull(FieldPath<T, ?> path) {
        return addPathCondition(path, Operator.IS_NULL, null);
    }

    /** Nested-path IS NOT NULL / 嵌套路径 IS NOT NULL 查询 */
    public LambdaQueryWrapper<T> isNotNull(FieldPath<T, ?> path) {
        return addPathCondition(path, Operator.IS_NOT_NULL, null);
    }

    /** Nested-path $mod / 嵌套路径 $mod 查询 */
    public LambdaQueryWrapper<T> mod(FieldPath<T, ?> path, int divisor, int remainder) {
        return addPathCondition(path, Operator.MOD, Arrays.asList(divisor, remainder));
    }

    /** Nested-path $type / 嵌套路径 $type 查询 */
    public LambdaQueryWrapper<T> type(FieldPath<T, ?> path, String bsonTypeName) {
        return addPathCondition(path, Operator.TYPE, bsonTypeName);
    }

    public LambdaQueryWrapper<T> or() {
        conditions.add(new Condition());
        return this;
    }

    public LambdaQueryWrapper<T> or(LambdaQueryWrapper<T> orWrapper) {
        Objects.requireNonNull(orWrapper, "orWrapper must not be null");
        conditions.add(new Condition());
        conditions.addAll(orWrapper.getConditions());
        return this;
    }

    // ---- 排序 ----

    /**
     * 排序条目：包含 Java 字段名、字段声明类、升降序方向。
     * 由 Lambda 方法引用自动提取，字段映射可正确解析 @CollectionField 注解。
     */
    public static final class OrderBy {
        private final String javaFieldName;
        private final Class<?> implClass;
        private final boolean ascending;

        OrderBy(String javaFieldName, Class<?> implClass, boolean ascending) {
            this.javaFieldName = javaFieldName;
            this.implClass = implClass;
            this.ascending = ascending;
        }

        public String getJavaFieldName() { return javaFieldName; }
        public Class<?> getImplClass() { return implClass; }
        public boolean isAscending() { return ascending; }
    }

    private final List<OrderBy> orderBys = new ArrayList<>();

    public LambdaQueryWrapper<T> orderByAsc(SFunction<T, ?> field) {
        Objects.requireNonNull(field, "field must not be null");
        orderBys.add(new OrderBy(
                ReflectUtil.getFieldNameFromLambda(field),
                ReflectUtil.getImplClassFromLambda(field),
                true));
        return this;
    }

    public LambdaQueryWrapper<T> orderByDesc(SFunction<T, ?> field) {
        Objects.requireNonNull(field, "field must not be null");
        orderBys.add(new OrderBy(
                ReflectUtil.getFieldNameFromLambda(field),
                ReflectUtil.getImplClassFromLambda(field),
                false));
        return this;
    }

    /** Nested-path ascending sort / 嵌套路径升序排序 */
    public LambdaQueryWrapper<T> orderByAsc(FieldPath<T, ?> path) {
        Objects.requireNonNull(path, "path must not be null");
        orderBys.add(new OrderBy(path.javaPath(), path.rootImplClass(), true));
        return this;
    }

    /** Nested-path descending sort / 嵌套路径降序排序 */
    public LambdaQueryWrapper<T> orderByDesc(FieldPath<T, ?> path) {
        Objects.requireNonNull(path, "path must not be null");
        orderBys.add(new OrderBy(path.javaPath(), path.rootImplClass(), false));
        return this;
    }

    public List<OrderBy> getOrderBys() {
        return orderBys;
    }

    // ---- 字段投影 ----

    /**
     * 字段投影条目：包含 Java 字段名、字段声明类。
     * 用于限制查询仅返回指定字段（MongoDB projection），减少网络传输开销。
     */
    public static final class ProjectionField {
        private final String javaFieldName;
        private final Class<?> implClass;

        ProjectionField(String javaFieldName, Class<?> implClass) {
            this.javaFieldName = javaFieldName;
            this.implClass = implClass;
        }

        public String getJavaFieldName() { return javaFieldName; }
        public Class<?> getImplClass() { return implClass; }
    }

    private final List<ProjectionField> projections = new ArrayList<>();
    private final List<ProjectionField> excludes = new ArrayList<>();

    /**
     * 指定查询返回的字段（MongoDB projection include 模式）。
     * 只有列出的字段会被返回，MongoDB 默认仍会返回 {@code _id}。
     * 如需同时排除 {@code _id}，可链式调用 {@link #exclude(SFunction...)}：
     *
     * <pre>{@code
     * wrapper.eq(User::getStatus, "active")
     *        .include(User::getName, User::getAge);
     * // → find({status: "active"}, {name: 1, age: 1})
     *
     * wrapper.eq(User::getStatus, "active")
     *        .include(User::getName, User::getAge)
     *        .exclude(User::getId);
     * // → find({status: "active"}, {name: 1, age: 1, _id: 0})
     * }</pre>
     *
     * <p>注意：MongoDB 不允许同时使用 include 和 exclude（{@code _id} 除外）。
     * 如果先调用了 {@code include()}，再调用 {@code exclude()} 只能排除
     * 映射到 {@code _id} 的字段，否则会抛出 {@link IllegalArgumentException}。</p>
     */
    @SafeVarargs
    public final LambdaQueryWrapper<T> include(SFunction<T, ?>... fields) {
        Objects.requireNonNull(fields, "fields must not be null");
        for (SFunction<T, ?> field : fields) {
            Objects.requireNonNull(field, "field must not be null");
            projections.add(new ProjectionField(
                    ReflectUtil.getFieldNameFromLambda(field),
                    ReflectUtil.getImplClassFromLambda(field)));
        }
        return this;
    }

    public List<ProjectionField> getProjections() {
        return projections;
    }

    /** Nested-path projection include / 嵌套路径投影包含 */
    @SafeVarargs
    public final LambdaQueryWrapper<T> include(FieldPath<T, ?>... paths) {
        Objects.requireNonNull(paths, "paths must not be null");
        for (FieldPath<T, ?> path : paths) {
            Objects.requireNonNull(path, "path must not be null");
            projections.add(new ProjectionField(path.javaPath(), path.rootImplClass()));
        }
        return this;
    }

    /**
     * 指定查询排除的字段（MongoDB projection exclude 模式）。
     * 除列出的字段外，其他字段均返回（包括 {@code _id}）。
     *
     * <pre>{@code
     * wrapper.eq(User::getStatus, "active")
     *        .exclude(User::getPassword, User::getLargeData);
     * // → find({status: "active"}, {password: 0, largeData: 0})
     * }</pre>
     *
     * <p>注意：MongoDB 不允许同时使用 include 和 exclude（{@code _id} 除外）。
     * 如果先调用了 {@link #include(SFunction...)}，再调用此方法只能排除
     * 映射到 {@code _id} 的字段，否则会抛出 {@link IllegalArgumentException}。</p>
     */
    @SafeVarargs
    public final LambdaQueryWrapper<T> exclude(SFunction<T, ?>... fields) {
        Objects.requireNonNull(fields, "fields must not be null");
        for (SFunction<T, ?> field : fields) {
            Objects.requireNonNull(field, "field must not be null");
            excludes.add(new ProjectionField(
                    ReflectUtil.getFieldNameFromLambda(field),
                    ReflectUtil.getImplClassFromLambda(field)));
        }
        return this;
    }

    public List<ProjectionField> getExcludes() {
        return excludes;
    }

    /** Nested-path projection exclude / 嵌套路径投影排除 */
    @SafeVarargs
    public final LambdaQueryWrapper<T> exclude(FieldPath<T, ?>... paths) {
        Objects.requireNonNull(paths, "paths must not be null");
        for (FieldPath<T, ?> path : paths) {
            Objects.requireNonNull(path, "path must not be null");
            excludes.add(new ProjectionField(path.javaPath(), path.rootImplClass()));
        }
        return this;
    }

    // ---- 静态工厂 ----

    /**
     * 从实体对象自动构建查询条件。遍历 entity 的所有非 null 字段，每个非 null 字段
     * 自动生成 {@code eq()} 条件。与 {@link #include(SFunction...)} 等方法组合使用：
     *
     * <pre>{@code
     * User probe = new User();
     * probe.setName("Tom");
     * probe.setStatus("active");
     * List<User> result = repo.findList(
     *     LambdaQueryWrapper.fromEntity(probe)
     *         .include(User::getName, User::getAge)
     *         .orderByAsc(User::getBirthday)
     * );
     * }</pre>
     *
     * <p>null 字段会被忽略；static / transient 字段会被跳过。遍历范围包括当前类
     * 及其所有超类中声明的字段。
     *
     * <p><b>关于 {@code implClass}：</b>此方法生成的每个 {@link Condition} 都以
     * {@code entity.getClass()}（具体运行时类型）作为 {@code implClass}，而非字段实际
     * 声明的父类。这与 Lambda 路径（{@code eq(Entity::getField, val)} 中
     * {@code implClass} 指向 getter 方法实际声明的类）不同，但由于
     * {@code ClassFieldMetaData} 构造时会遍历完整类层次，字段名解析结果一致。
     * 仅当子类声明了与父类同名的字段且使用了不同的 {@code @CollectionField} 值时
     * 才会有差异——此时本方法使用子类的映射（最派生类优先生效），这更符合直觉。</p>
     */
    public static <T> LambdaQueryWrapper<T> fromEntity(T entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) entity.getClass();
        LambdaQueryWrapper<T> wrapper = new LambdaQueryWrapper<>(clazz);

        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                // 跳过 static 和 transient 字段
                int mod = field.getModifiers();
                if (java.lang.reflect.Modifier.isStatic(mod) || java.lang.reflect.Modifier.isTransient(mod)) {
                    continue;
                }
                field.setAccessible(true);
                try {
                    Object value = field.get(entity);
                    if (value != null) {
                        wrapper.conditions.add(
                                new Condition(field.getName(), Operator.EQ, value, clazz));
                    }
                } catch (IllegalAccessException e) {
                    LOGGER.debug("Cannot access field '{}' on entity of type {}: {}",
                            field.getName(), clazz.getSimpleName(), e.getMessage());
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        return wrapper;
    }
}
