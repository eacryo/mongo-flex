package com.github.eacryo.mongoflex.query;

import com.github.eacryo.mongoflex.convertor.MongoMappingConvertor;
import com.github.eacryo.mongoflex.repository.DynamicMongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Standalone MongoDB operations entry point — not bound to any repository /
 * 独立的 MongoDB 操作入口——不绑定任何 Repository
 * <p>
 * Use {@code MongoOps} for operations that don't fit the single-collection
 * {@code MongoRepository} model, such as aggregation pipelines with
 * {@code $lookup} / {@code $group} whose output type differs from the source entity.
 * <p>
 * 当操作不适合单集合的 {@code MongoRepository} 模型时使用 {@code MongoOps}，
 * 例如输出类型与源实体不同的 {@code $lookup} / {@code $group} 聚合管道。
 *
 * <pre>{@code
 * @Autowired
 * private MongoOps mongoOps;
 *
 * List<OrderWithCustomer> results = mongoOps.aggregate(
 *     new AggregationWrapper<>(Order.class)
 *         .match(w -> w.eq(Order::getStatus, "active"))
 *         .lookup(Customer.class, Order::getCustomerId, Customer::getId, "customer"),
 *     OrderWithCustomer.class
 * );
 * }</pre>
 */
public class MongoOps {

    private final DynamicMongoClient dynamicMongoClient;
    private final MongoMappingConvertor convertor;

    public MongoOps(DynamicMongoClient dynamicMongoClient, MongoMappingConvertor convertor) {
        this.dynamicMongoClient = Objects.requireNonNull(dynamicMongoClient, "dynamicMongoClient must not be null");
        this.convertor = Objects.requireNonNull(convertor, "convertor must not be null");
    }

    /**
     * Execute an aggregation pipeline built via {@link AggregationWrapper} and map results
     * to the specified output type / 执行 {@link AggregationWrapper} 构建的聚合管道并将结果映射为指定的输出类型
     *
     * @param wrapper     the aggregation pipeline builder / 聚合管道构建器
     * @param outputClass the target type for each result document / 每条结果文档的目标类型
     * @param <T>         source entity type / 源实体类型
     * @param <R>         output type / 输出类型
     * @return list of mapped results / 映射后的结果列表
     */
    public <T, R> List<R> aggregate(AggregationWrapper<T> wrapper, Class<R> outputClass) {
        Objects.requireNonNull(wrapper, "wrapper must not be null");
        Objects.requireNonNull(outputClass, "outputClass must not be null");

        List<Bson> pipeline = wrapper.render(convertor);
        Class<T> entityClass = wrapper.getEntityClass();
        String collectionName = resolveCollectionName(entityClass);
        return executeAggregate(collectionName, pipeline, outputClass);
    }

    /**
     * Execute a raw aggregation pipeline (JSON array of stages) with {@code #{param}} replacement
     * and map results to the specified output type /
     * 执行原始聚合管道（JSON 数组 stages）并进行 {@code #{param}} 替换，将结果映射为指定的输出类型
     *
     * @param sourceClass the source entity class (for collection name resolution) / 源实体类（用于解析集合名）
     * @param pipelineJson JSON array of pipeline stages / pipeline stages 的 JSON 数组
     * @param outputClass the target type for each result document / 每条结果文档的目标类型
     * @param <T>         source entity type / 源实体类型
     * @param <R>         output type / 输出类型
     * @return list of mapped results / 映射后的结果列表
     */
    public <T, R> List<R> aggregate(Class<T> sourceClass, String pipelineJson, Class<R> outputClass) {
        Objects.requireNonNull(sourceClass, "sourceClass must not be null");
        Objects.requireNonNull(pipelineJson, "pipelineJson must not be null");
        Objects.requireNonNull(outputClass, "outputClass must not be null");

        String collectionName = resolveCollectionName(sourceClass);
        List<Bson> pipeline = parsePipelineJson(pipelineJson);
        return executeAggregate(collectionName, pipeline, outputClass);
    }

    /**
     * Execute an aggregation and return raw {@link Map} results (no entity mapping) /
     * 执行聚合查询并返回原始 {@link Map} 结果（不进行实体映射）
     */
    public <T> List<Map<String, Object>> aggregateRaw(AggregationWrapper<T> wrapper) {
        Objects.requireNonNull(wrapper, "wrapper must not be null");

        List<Bson> pipeline = wrapper.render(convertor);
        String collectionName = resolveCollectionName(wrapper.getEntityClass());
        return executeAggregateRaw(collectionName, pipeline);
    }

    /**
     * Execute a raw aggregation pipeline and return raw {@link Map} results /
     * 执行原始聚合管道并返回原始 {@link Map} 结果
     */
    public <T> List<Map<String, Object>> aggregateRaw(Class<T> sourceClass, String pipelineJson) {
        Objects.requireNonNull(sourceClass, "sourceClass must not be null");
        Objects.requireNonNull(pipelineJson, "pipelineJson must not be null");

        String collectionName = resolveCollectionName(sourceClass);
        List<Bson> pipeline = parsePipelineJson(pipelineJson);
        return executeAggregateRaw(collectionName, pipeline);
    }

    private <R> List<R> executeAggregate(String collectionName, List<Bson> pipeline, Class<R> outputClass) {
        MongoDatabase database = dynamicMongoClient.selectDatabase();
        MongoCollection<Document> collection = database.getCollection(collectionName);
        List<Document> docs = collection.aggregate(pipeline).into(new ArrayList<>());
        List<R> results = new ArrayList<>(docs.size());
        for (Document doc : docs) {
            results.add(convertor.read(doc, outputClass));
        }
        return results;
    }

    private <T> List<Map<String, Object>> executeAggregateRaw(String collectionName, List<Bson> pipeline) {
        MongoDatabase database = dynamicMongoClient.selectDatabase();
        MongoCollection<Document> collection = database.getCollection(collectionName);
        List<Document> docs = collection.aggregate(pipeline).into(new ArrayList<>());
        List<Map<String, Object>> results = new ArrayList<>(docs.size());
        for (Document doc : docs) {
            results.add(convertor.documentToMap(doc));
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    private List<Bson> parsePipelineJson(String pipelineJson) {
        List<Bson> pipeline = new ArrayList<>();
        String trimmed = pipelineJson.trim();
        if (!trimmed.startsWith("[")) {
            throw new IllegalArgumentException(
                    "pipelineJson must be a JSON array, e.g. '[{\"$match\":{...}}, {\"$group\":{...}}]'"
                    + " / pipelineJson 必须是 JSON 数组，例如 '[{\"$match\":{...}}, {\"$group\":{...}}]'");
        }
        Document parsed = Document.parse("{pipeline: " + trimmed + "}");
        List<Document> stages = (List<Document>) parsed.get("pipeline");
        if (stages != null) {
            pipeline.addAll(stages);
        }
        return pipeline;
    }

    private static <T> String resolveCollectionName(Class<T> clazz) {
        com.github.eacryo.mongoflex.annotation.CollectionName annotation =
                clazz.getAnnotation(com.github.eacryo.mongoflex.annotation.CollectionName.class);
        if (annotation == null) {
            throw new IllegalArgumentException(
                    "Class " + clazz.getName() + " must have @CollectionName annotation"
                    + " / 类 " + clazz.getName() + " 必须标注 @CollectionName 注解");
        }
        return annotation.value();
    }
}
