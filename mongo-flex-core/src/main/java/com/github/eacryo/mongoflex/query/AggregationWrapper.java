package com.github.eacryo.mongoflex.query;

import com.github.eacryo.mongoflex.annotation.CollectionName;
import com.github.eacryo.mongoflex.convertor.MongoMappingConvertor;
import com.github.eacryo.mongoflex.util.ReflectUtil;
import com.github.eacryo.mongoflex.util.SFunction;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Type-safe MongoDB aggregation pipeline builder / 类型安全的 MongoDB 聚合管道构建器
 * <p>
 * Builds a pipeline of {@link Bson} stages using lambda method references for
 * type-safe field name resolution (including {@code @CollectionField} mapping).
 * <p>
 * 使用 Lambda 方法引用构建 Bson pipeline，支持类型安全的字段名解析
 * （包括 {@code @CollectionField} 映射）。
 *
 * <pre>{@code
 * List<OrderWithCustomer> results = mongoOps.aggregate(
 *     new AggregationWrapper<>(Order.class)
 *         .match(w -> w.eq(Order::getStatus, "active"))
 *         .lookup(Customer.class, Order::getCustomerId, Customer::getId, "customer")
 *         .unwind("customer")
 *         .sortDesc("amount")
 *         .limit(10),
 *     OrderWithCustomer.class
 * );
 * }</pre>
 *
 * @param <T> the source entity type / 源实体类型
 */
public class AggregationWrapper<T> {

    private final Class<T> entityClass;
    private final List<Function<MongoMappingConvertor, Bson>> stages = new ArrayList<>();

    public AggregationWrapper(Class<T> entityClass) {
        this.entityClass = Objects.requireNonNull(entityClass, "entityClass must not be null");
    }

    public Class<T> getEntityClass() {
        return entityClass;
    }

    /**
     * $match stage — reuse {@link LambdaQueryWrapper} for type-safe filter conditions /
     * $match 阶段——复用 {@link LambdaQueryWrapper} 实现类型安全的过滤条件
     */
    public AggregationWrapper<T> match(Consumer<LambdaQueryWrapper<T>> consumer) {
        Objects.requireNonNull(consumer, "consumer must not be null");
        LambdaQueryWrapper<T> wrapper = new LambdaQueryWrapper<>(entityClass);
        consumer.accept(wrapper);
        stages.add(convertor -> new Document("$match", wrapper.toBson(convertor)));
        return this;
    }

    /**
     * $lookup stage — type-safe with {@code Class<F>} providing foreign collection name
     * and {@code @CollectionField} mapping metadata /
     * $lookup 阶段——通过 {@code Class<F>} 提供外键集合名和 {@code @CollectionField} 映射元数据
     *
     * @param foreignClass foreign entity class (must have @CollectionName) / 外键实体类（必须有 @CollectionName）
     * @param localField   lambda reference on T / T 上的 Lambda 引用
     * @param foreignField lambda reference on F / F 上的 Lambda 引用
     * @param as           output array field name / 输出的数组字段名
     */
    public <F> AggregationWrapper<T> lookup(
            Class<F> foreignClass,
            SFunction<T, ?> localField,
            SFunction<F, ?> foreignField,
            String as) {
        Objects.requireNonNull(foreignClass, "foreignClass must not be null");
        Objects.requireNonNull(localField, "localField must not be null");
        Objects.requireNonNull(foreignField, "foreignField must not be null");
        Objects.requireNonNull(as, "as must not be null");
        stages.add(convertor -> {
            String from = resolveCollectionName(foreignClass);
            String local = convertor.resolveMongoFieldPath(entityClass,
                    ReflectUtil.getFieldNameFromLambda(localField));
            String foreign = convertor.resolveMongoFieldPath(foreignClass,
                    ReflectUtil.getFieldNameFromLambda(foreignField));
            return new Document("$lookup", new Document()
                    .append("from", from)
                    .append("localField", local)
                    .append("foreignField", foreign)
                    .append("as", as));
        });
        return this;
    }

    /**
     * $group stage — returns a {@link GroupBuilder} for type-safe accumulator construction /
     * $group 阶段——返回 {@link GroupBuilder} 用于类型安全的累加器构建
     */
    @SafeVarargs
    public final GroupBuilder<T> group(SFunction<T, ?>... fields) {
        Objects.requireNonNull(fields, "fields must not be null");
        return new GroupBuilder<>(this, fields);
    }

    /**
     * $sort ascending / $sort 升序
     *
     * @param fields MongoDB field names (after @CollectionField mapping) / MongoDB 字段名（@CollectionField 映射后的名字）
     */
    public AggregationWrapper<T> sortAsc(String... fields) {
        Objects.requireNonNull(fields, "fields must not be null");
        Document sortDoc = new Document();
        for (String f : fields) {
            sortDoc.append(f, 1);
        }
        stages.add(c -> new Document("$sort", sortDoc));
        return this;
    }

    /**
     * $sort descending / $sort 降序
     */
    public AggregationWrapper<T> sortDesc(String... fields) {
        Objects.requireNonNull(fields, "fields must not be null");
        Document sortDoc = new Document();
        for (String f : fields) {
            sortDoc.append(f, -1);
        }
        stages.add(c -> new Document("$sort", sortDoc));
        return this;
    }

    /** $limit stage */
    public AggregationWrapper<T> limit(int n) {
        stages.add(c -> new Document("$limit", n));
        return this;
    }

    /** $skip stage */
    public AggregationWrapper<T> skip(int n) {
        stages.add(c -> new Document("$skip", n));
        return this;
    }

    /**
     * $unwind stage / $unwind 阶段
     *
     * @param field the array field name (without $ prefix) / 数组字段名（不带 $ 前缀）
     */
    public AggregationWrapper<T> unwind(String field) {
        Objects.requireNonNull(field, "field must not be null");
        stages.add(c -> new Document("$unwind", "$" + field));
        return this;
    }

    /**
     * $unwind with preserveNullAndEmptyArrays option / 带 preserveNullAndEmptyArrays 选项的 $unwind
     */
    public AggregationWrapper<T> unwind(String field, boolean preserveNullAndEmptyArrays) {
        Objects.requireNonNull(field, "field must not be null");
        stages.add(c -> new Document("$unwind", new Document()
                .append("path", "$" + field)
                .append("preserveNullAndEmptyArrays", preserveNullAndEmptyArrays)));
        return this;
    }

    /**
     * $project include — type-safe field references /
     * $project 包含——类型安全的字段引用
     */
    @SafeVarargs
    public final AggregationWrapper<T> include(SFunction<T, ?>... fields) {
        Objects.requireNonNull(fields, "fields must not be null");
        stages.add(convertor -> {
            Document proj = new Document();
            for (SFunction<T, ?> f : fields) {
                String mongoField = convertor.resolveMongoFieldPath(entityClass,
                        ReflectUtil.getFieldNameFromLambda(f));
                proj.append(mongoField, 1);
            }
            return new Document("$project", proj);
        });
        return this;
    }

    /**
     * $project exclude — type-safe field references /
     * $project 排除——类型安全的字段引用
     */
    @SafeVarargs
    public final AggregationWrapper<T> exclude(SFunction<T, ?>... fields) {
        Objects.requireNonNull(fields, "fields must not be null");
        stages.add(convertor -> {
            Document proj = new Document();
            for (SFunction<T, ?> f : fields) {
                String mongoField = convertor.resolveMongoFieldPath(entityClass,
                        ReflectUtil.getFieldNameFromLambda(f));
                proj.append(mongoField, 0);
            }
            return new Document("$project", proj);
        });
        return this;
    }

    /**
     * Render all stages into a {@code List<Bson>} pipeline /
     * 渲染所有 stage 为 {@code List<Bson>} pipeline
     */
    public List<Bson> render(MongoMappingConvertor convertor) {
        Objects.requireNonNull(convertor, "convertor must not be null");
        List<Bson> pipeline = new ArrayList<>(stages.size());
        for (Function<MongoMappingConvertor, Bson> stage : stages) {
            pipeline.add(stage.apply(convertor));
        }
        return pipeline;
    }

    boolean isStagesEmpty() {
        return stages.isEmpty();
    }

    /** Package-private: add a deferred stage from GroupBuilder */
    void addStage(Function<MongoMappingConvertor, Bson> stage) {
        stages.add(stage);
    }

    private static <T> String resolveCollectionName(Class<T> clazz) {
        CollectionName annotation = clazz.getAnnotation(CollectionName.class);
        if (annotation == null) {
            throw new IllegalArgumentException(
                    "Foreign class " + clazz.getName() + " must have @CollectionName annotation"
                    + " / 外键类 " + clazz.getName() + " 必须标注 @CollectionName 注解");
        }
        return annotation.value();
    }
}
