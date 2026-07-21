package com.github.eacryo.mongoflex.repository;

import com.github.eacryo.mongoflex.convertor.MongoMappingConvertor;
import com.github.eacryo.mongoflex.annotation.Aggregate;
import com.github.eacryo.mongoflex.annotation.Count;
import com.github.eacryo.mongoflex.annotation.Delete;
import com.github.eacryo.mongoflex.annotation.Find;
import com.github.eacryo.mongoflex.annotation.Update;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Supplier;

/**
 * JDK proxy handler for {@link com.github.eacryo.mongoflex.annotation.MRepository} interfaces / {@link com.github.eacryo.mongoflex.annotation.MRepository} 接口的 JDK 动态代理处理器
 * <p>
 * Dispatch logic:
 * <ol>
 *   <li>{@code @Find} / {@code @Count} / {@code @Delete} / {@code @Update} / {@code @Aggregate} — JSON template → filter Document / pipeline → baseRepository</li>
 *   <li>Method inherited from {@link MongoRepository} — delegate to baseRepository</li>
 * </ol>
 */
@Slf4j
public class MyRepositoryProxyHandler<T, ID> implements InvocationHandler {

    private final Class<?> targetInterface = MongoRepository.class;
    private final Class<?> repositoryInterface;
    private final Supplier<MongoDatabase> databaseSupplier;
    private final JsonTemplateParser jsonTemplateParser = new JsonTemplateParser();
    private final MongoMappingConvertor mongoMappingConvertor;
    private final SimpleMongoRepository<T, ID> baseRepository;

    public MyRepositoryProxyHandler(Class<?> repositoryInterface,
                                    Supplier<MongoDatabase> databaseSupplier,
                                    MongoMappingConvertor mongoMappingConvertor,
                                    SimpleMongoRepository<T, ID> baseRepository) {
        this.repositoryInterface = Objects.requireNonNull(repositoryInterface, "repositoryInterface must not be null");
        this.databaseSupplier = Objects.requireNonNull(databaseSupplier, "databaseSupplier must not be null");
        this.mongoMappingConvertor = Objects.requireNonNull(mongoMappingConvertor, "mongoMappingConvertor must not be null");
        this.baseRepository = Objects.requireNonNull(baseRepository, "baseRepository must not be null");
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            return doInvoke(proxy, method, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private Object doInvoke(Object proxy, Method method, Object[] args) throws Throwable {
        // ── Annotation-driven query path / 注解驱动查询路径 ──
        if (method.isAnnotationPresent(Find.class)) {
            return handleFind(method, args);
        }
        if (method.isAnnotationPresent(Count.class)) {
            return handleCount(method, args);
        }
        if (method.isAnnotationPresent(Delete.class)) {
            return handleDelete(method, args);
        }
        if (method.isAnnotationPresent(Update.class)) {
            return handleUpdate(method, args);
        }
        if (method.isAnnotationPresent(Aggregate.class)) {
            return handleAggregate(method, args);
        }

        // ── Inherited from MongoRepository / 继承自 MongoRepository ──
        if (isMethodFromTargetInterface(method, targetInterface)) {
            log.info("Method {} inherit from parent interface", method.getName());
            return method.invoke(baseRepository, args);
        }

        // ── Object methods / Object 方法 ──
        if (isMethodFromTargetInterface(method, Object.class)) {
            if ("toString".equals(method.getName())) {
                return "Repository proxy for " + repositoryInterface.getName();
            }
            return method.invoke(baseRepository, args);
        }

        throw new UnsupportedOperationException("Method " + method.getName() +
                " is neither annotated with @Find/@Count/@Delete/@Update/@Aggregate nor inherited from MongoRepository.");
    }

    // ──── @Find handler / @Find 处理器 ────

    private Object handleFind(Method method, Object[] args) {
        Find find = method.getAnnotation(Find.class);
        Document filter = jsonTemplateParser.parse(find.value(), method, args);
        int skip = (int) find.skip();
        int limit = (int) find.limit();

        Type genericReturnType = method.getGenericReturnType();
        Class<?> rawReturnType = method.getReturnType();

        if (List.class.isAssignableFrom(rawReturnType)) {
            // List return type: extract element class / List 返回类型：提取元素类型
            Class<?> elementClass = extractListElementClass(genericReturnType);
            if (elementClass == Object.class || elementClass == null) {
                // Raw List or List<Object>: return as Map list / 原始 List 或 List<Object>：返回 Map 列表
                return executeRawFindList(filter, skip, limit);
            }
            return baseRepository.findListByFilter(filter, skip, limit);
        } else {
            // Single entity return type / 单实体返回类型
            if (rawReturnType == Object.class) {
                Document doc = databaseSupplier.get().getCollection(baseRepository.collectionName)
                        .find(filter).first();
                return doc != null ? mongoMappingConvertor.documentToMap(doc) : null;
            }
            return baseRepository.findOneByFilter(filter);
        }
    }

    /**
     * Execute find returning raw List (no entity mapping) /
     * 执行返回原始 List 的查询（不进行实体映射）
     */
    private List<Map<String, Object>> executeRawFindList(Document filter, int skip, int limit) {
        com.mongodb.client.FindIterable<Document> iter = databaseSupplier.get()
                .getCollection(baseRepository.collectionName).find(filter);
        if (skip > 0) iter = iter.skip(skip);
        if (limit > 0) iter = iter.limit(limit);
        List<Map<String, Object>> results = new ArrayList<>();
        for (Document doc : iter) {
            results.add(mongoMappingConvertor.documentToMap(doc));
        }
        return results;
    }

    // ──── @Count handler / @Count 处理器 ────

    private Object handleCount(Method method, Object[] args) {
        Count count = method.getAnnotation(Count.class);
        Document filter = jsonTemplateParser.parse(count.value(), method, args);
        long result = baseRepository.countByFilter(filter);
        Class<?> returnType = method.getReturnType();
        if (returnType == int.class || returnType == Integer.class) {
            return (int) result;
        }
        return result;
    }

    // ──── @Delete handler / @Delete 处理器 ────

    private Object handleDelete(Method method, Object[] args) {
        Delete delete = method.getAnnotation(Delete.class);
        Document filter = jsonTemplateParser.parse(delete.value(), method, args);
        long deleted = baseRepository.deleteByFilter(filter);
        Class<?> returnType = method.getReturnType();
        if (returnType == void.class || returnType == Void.class) {
            return null;
        }
        return deleted;
    }

    // ──── @Update handler / @Update 处理器 ────

    private Object handleUpdate(Method method, Object[] args) {
        Update update = method.getAnnotation(Update.class);
        Document filter = jsonTemplateParser.parse(update.value(), method, args);
        Document updateDoc = jsonTemplateParser.parse(update.update(), method, args);
        long modified = baseRepository.updateByFilter(filter, updateDoc, update.multi(), update.upsert());
        Class<?> returnType = method.getReturnType();
        if (returnType == void.class || returnType == Void.class) {
            return null;
        }
        return modified;
    }

    // ──── @Aggregate handler / @Aggregate 处理器 ────

    private Object handleAggregate(Method method, Object[] args) {
        Aggregate aggregate = method.getAnnotation(Aggregate.class);
        String pipelineJson = jsonTemplateParser.replacePlaceholders(aggregate.value(), method, args);
        List<Document> pipeline = parsePipelineArray(pipelineJson);

        List<Document> docs = databaseSupplier.get()
                .getCollection(baseRepository.collectionName)
                .aggregate(new ArrayList<>(pipeline))
                .into(new ArrayList<>());

        Type genericReturnType = method.getGenericReturnType();
        Class<?> rawReturnType = method.getReturnType();

        if (List.class.isAssignableFrom(rawReturnType)) {
            Class<?> elementClass = extractListElementClass(genericReturnType);
            if (elementClass == Object.class || elementClass == null) {
                List<Map<String, Object>> results = new ArrayList<>(docs.size());
                for (Document doc : docs) {
                    results.add(mongoMappingConvertor.documentToMap(doc));
                }
                return results;
            }
            List<Object> results = new ArrayList<>(docs.size());
            for (Document doc : docs) {
                results.add(mongoMappingConvertor.read(doc, elementClass));
            }
            return results;
        }

        if (docs.isEmpty()) {
            return null;
        }
        if (rawReturnType == Object.class) {
            return mongoMappingConvertor.documentToMap(docs.get(0));
        }
        return mongoMappingConvertor.read(docs.get(0), rawReturnType);
    }

    // ──── Helpers / 辅助方法 ────

    /**
     * Extract the element type from a generic List return type / 从泛型 List 返回类型中提取元素类型
     */
    private Class<?> extractListElementClass(Type genericReturnType) {
        if (genericReturnType instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) genericReturnType;
            Type[] typeArgs = pType.getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                return (Class<?>) typeArgs[0];
            }
        }
        return Object.class;
    }

    @SuppressWarnings("unchecked")
    private List<Document> parsePipelineArray(String pipelineJson) {
        String trimmed = pipelineJson.trim();
        if (!trimmed.startsWith("[")) {
            throw new IllegalArgumentException(
                    "@Aggregate value must be a JSON array, e.g. '[{\"$match\":{...}}, {\"$lookup\":{...}}]'"
                    + " / @Aggregate value 必须是 JSON 数组，例如 '[{\"$match\":{...}}, {\"$lookup\":{...}}]'");
        }
        Document wrapper = Document.parse("{_pipeline: " + trimmed + "}");
        List<Document> pipeline = (List<Document>) wrapper.get("_pipeline");
        return pipeline != null ? pipeline : Collections.emptyList();
    }

    private boolean isMethodFromTargetInterface(Method method, Class<?> targetInterface) {
        for (Method interfaceMethod : targetInterface.getMethods()) {
            if (interfaceMethod.getName().equals(method.getName()) &&
                    Arrays.equals(interfaceMethod.getParameterTypes(), method.getParameterTypes())) {
                return true;
            }
        }
        return false;
    }
}
