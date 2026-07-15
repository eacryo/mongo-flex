package com.github.eacryo.mongoflex.query;

import com.github.eacryo.mongoflex.convertor.MongoMappingConvertor;
import com.github.eacryo.mongoflex.query.QuerySpec;
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

/**
 * A Lambda-based query wrapper similar to MyBatis-Plus's LambdaQueryWrapper.
 * Implements {@link QuerySpec} so it can be used anywhere a query specification is expected /
 * 基于 Lambda 的查询构造器，类似 MyBatis-Plus 的 LambdaQueryWrapper。
 * 实现 {@link QuerySpec}，可作为统一查询抽象在任何查询路径中使用。
 * <p>
 * Example usage / 使用示例:
 * <pre>{@code
 * LambdaQueryWrapper<User> w = new LambdaQueryWrapper<>(User.class);
 * w.eq(User::getUserName, "Tom");
 * }</pre>
 */
public class LambdaQueryWrapper<T> implements QuerySpec<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LambdaQueryWrapper.class);

    private final List<Condition> conditions = new ArrayList<>();
    private Class<T> entityClass;

    public LambdaQueryWrapper() {
    }

    public LambdaQueryWrapper(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
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
    @Override
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
     * IS NULL → { field: { $exists: false } }
     */
    public LambdaQueryWrapper<T> isNull(SFunction<T, ?> field) {
        Objects.requireNonNull(field, "field must not be null");
        String javaField = ReflectUtil.getFieldNameFromLambda(field);
        conditions.add(new Condition(javaField, Operator.IS_NULL, null, ReflectUtil.getImplClassFromLambda(field)));
        return this;
    }

    /**
     * IS NOT NULL → { field: { $exists: true } }
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

    // ---- 静态工厂 ----

    /**
     * 从实体对象自动构建查询条件。遍历 entity 的所有非 null 字段，每个非 null 字段
     * 自动生成 {@code eq()} 条件。与 {@link #include(Object...)} 等方法组合使用：
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
