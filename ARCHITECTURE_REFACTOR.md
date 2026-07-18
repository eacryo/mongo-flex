# mongo-flex 架构重构方案

> **状态**：待实施  
> **创建日期**：2026-07-11  
> **最后更新**：2026-07-19

---

## 一、问题诊断

当前 mongo-flex 提供三条查询路径：

| 路径 | 入口 | 查询构建 | 执行引擎 |
|------|------|---------|---------|
| Entity/SFunction | `MongoRepository` 方法 | 内联 `Document` / `Filters` | `SimpleMongoRepository` |
| Lambda 类型安全查询 | `LambdaQueryWrapper` 链式调用 | `Condition` + `Operator` → `MongoBsonRenderer` | `SimpleMongoRepository` |
| 原始 MQL 命令 | `@Mql` 注解 | 正则 → `Document.parse()` | `ExecutorProxy` → `CommandExecutor` |

三条路径之间**唯一的共享点**是 `MongoMappingConvertor`（POJO ↔ Document 转换）。除此之外：

- 没有统一的查询抽象（Query Object）
- 没有统一的执行引擎
- 没有接口继承层次
- 代理分发是硬编码 if-else，无扩展点

对比 Spring Data MongoDB 和 MyBatis Plus，它们都有一个"中枢"（`MongoTemplate` / `BaseMapper`）让所有查询方式最终汇入同一个执行引擎。mongo-flex 的三条路径各有自己的引擎，没有汇合点。

此外，`@Mql` 当前采用 MongoDB shell 语法（`db.getCollection('x').find({...}).skip(20).limit(10)`），带来了额外的问题：

- **手写 shell 语法编译器**：QueryParser 的正则解析、多参数分割、链式方法解析，每次加功能都在给一个没有 spec 文档的"伪标准"打补丁。MongoDB 官方 `mongosh` 解析器有数万行代码
- **信息冗余**：collection 名在 `@CollectionName` 和 shell 命令里各写一遍，不一致时静默查错 collection
- **`#{param}` 是 `toString()`**：Date、ObjectId、List 等复杂类型的 toString() 不是合法的 MongoDB Extended JSON，现在能过只因为测试里传的都是 String
- **两条路径行为不一致**：Lambda 路径享受 `MongoMappingConvertor` 自动字段映射、`IdType` 感知、租户自动注入；`@Mql` 路径全都没有，裸跑原始字符串

---

## 二、目标架构

```
                       QuerySpec<T> (接口)
                      /        |        \
           EntityExample  LambdaCriteria  MqlCriteria
                      \        |        /
                    QueryExecutor<T> (统一执行引擎)
                           |
                     MongoCollection<Document>


                LambdaAggregationWrapper<T> (聚合专用)
                           |
                     List<Bson> pipeline
                           |
                     collection.aggregate()
```

### 核心设计原则

1. **`QuerySpec<T>` 作为统一查询抽象** — 所有查询条件（无论是 Entity、Lambda、MQL）都实现此接口，产出 `Bson`
2. **`QueryExecutor<T>` 作为统一执行引擎** — 所有 CRUD 操作（find、count、delete、update）汇入单一执行器
3. **`@Mql` 废弃 shell 语法，改为注解属性 + 原始 JSON** — 能力保留但载体简化，`Document.parse()` 是 MongoDB Driver 原生能力，不需要自己解析
4. **`LambdaAggregationWrapper<T>` 复用 `LambdaQueryWrapper`** — `$match` 阶段直接复用 `MongoBsonRenderer`，21 个 filter 操作符零成本覆盖
5. **Repository 接口分层** — 拆分为基础 `CrudRepository` + 扩展 `MongoRepository`，用户可选声明
6. **代理分发插件化** — 用 `MethodHandler` 责任链替代硬编码 if-else

---

## 三、重构步骤

### Phase 1：统一查询抽象 `QuerySpec<T>`

**目标**：让 `LambdaQueryWrapper` 和 `@Mql` 查询产出物都实现同一个接口。

```java
package com.github.eacryo.mongoflex.query;

/**
 * 统一的查询条件抽象。
 * 所有查询路径（Lambda、MQL、Entity Example）最终都通过此接口产出 Bson filter。
 */
public interface QuerySpec<T> {

    /**
     * 将查询条件渲染为 MongoDB Bson filter。
     */
    Bson toBson(MongoMappingConvertor convertor);

    /**
     * 返回目标实体类型，用于字段名解析和结果映射。
     */
    Class<T> getEntityClass();
}
```

**改动范围**：

| 文件 | 改动 |
|------|------|
| 新建 `query/QuerySpec.java` | 接口定义 |
| `LambdaQueryWrapper.java` | 实现 `QuerySpec<T>`（内部已有 `conditions` 列表和 `entityClass`，改动小） |
| `MongoRepository.java` | `LambdaQueryWrapper<T>` 参数逐步替换为 `QuerySpec<T>` |

**注意**：`MongoRepository` 中的 `LambdaQueryWrapper` 参数先保留，新增 `QuerySpec` 版本的重载方法，标记旧方法 `@Deprecated`。

---

### Phase 2：抽取统一执行引擎 `QueryExecutor<T>`

**目标**：合并 `SimpleMongoRepository` 和 `CommandExecutor` 中的重复逻辑。

当前问题 — 两处重复的 find/findOne/count/delete 逻辑：

| 操作 | SimpleMongoRepository | CommandExecutor |
|------|----------------------|-----------------|
| find | 第 134-143 行 | `FindExecutor` 第 34-39 行 |
| findOne | 第 85-126 行 | `FindOneExecutor` 第 21-28 行 |
| count | 第 177-195 行 | `CountExecutor` 第 16 行 |
| delete | 第 268-310 行 | `DeleteOneExecutor` 第 19 行 |

```java
package com.github.eacryo.mongoflex.query;

public class QueryExecutor<T, ID> {

    private final Class<T> entityClass;
    private final Supplier<MongoDatabase> databaseSupplier;
    private final MongoMappingConvertor convertor;

    public List<T> find(QuerySpec<T> filter, Integer skip, Integer limit) { ... }
    public T findOne(QuerySpec<T> filter) { ... }
    public long count(QuerySpec<T> filter) { ... }
    public boolean delete(QuerySpec<T> filter) { ... }
    public long deleteMany(QuerySpec<T> filter) { ... }
    public long update(QuerySpec<T> filter, T entity) { ... }
    public long upsert(QuerySpec<T> filter, T entity) { ... }

    /** 执行聚合流水线 */
    public <R> List<R> aggregate(List<Bson> pipeline, Class<R> outputClass) { ... }
}
```

**改动范围**：

| 文件 | 改动 |
|------|------|
| 新建 `query/QueryExecutor.java` | 统一执行引擎 |
| `SimpleMongoRepository.java` | 委托给 `QueryExecutor`，自身只做参数适配 |
| `FindExecutor.java` / `FindOneExecutor.java` 等 | 废弃，逻辑已合并到 `QueryExecutor` |
| `CommandExecutor.java` + `ExecutorProxy.java` | 废弃 |

---

### Phase 3：@Mql 简化 —— 废弃 shell 语法，保留原始 JSON 能力

**问题**：当前 `@Mql` 要求用户写完整的 MongoDB shell 命令：

```java
@Mql("db.getCollection('character').find({'name':'#{name}'}).skip(20).limit(10)")
```

`QueryParser` 手写正则解析 collection 名、操作类型、filter JSON、链式 skip/limit。其中 collection 名与 entity 的 `@CollectionName` 冗余，操作类型可从方法签名推断，filter JSON 是 Driver 原生 `Document.parse()` 的能力。

**方案**：`@Mql` 改为注解属性，作为 LambdaQueryWrapper 无法覆盖场景的兜底出口：

```java
public @interface Mql {

    /** filter / update / pipeline JSON，支持 #{param} 占位符 */
    String value();

    /** sort JSON，可选。例 "{'name': 1, 'age': -1}" */
    String sort() default "";

    /** 分页，-1 表示不设置 */
    int skip() default -1;
    int limit() default -1;
}
```

**使用对比**：

```java
// 旧写法（废弃）
@Mql("db.getCollection('character').find({'name':'#{name}'}).skip(20).limit(10)")
List<Character> findByName(@Param("name") String name);

// 新写法
@Mql(value = "{'name':'#{name}'}", skip = 20, limit = 10)
List<Character> findByName(@Param("name") String name);

// 聚合 —— 直接写 pipeline JSON
@Mql("[{$match: {status: 'active'}}, {$group: {_id: '$region', count: {$sum: 1}}}]")
List<Map<String, Object>> regionStats();

// 更新 —— 操作类型从返回类型和方法名推断
@Mql("{'$set': {'status': 'inactive'}}")
long deactivateUsers();
```

**`#{param}` 替换改为类型安全**：

```java
// 旧：args[i].toString() —— Date 会炸
// 新：通过 MongoMappingConvertor 转换
Object converted = convertor.convertValue(args[i], targetBsonType);
```

**改动范围**：

| 文件 | 改动 |
|------|------|
| `Mql.java` | 新增 `sort`/`skip`/`limit` 属性 |
| `QueryParser.java` | **删除** shell 命令解析逻辑（正则、`splitTopLevelArguments`、`parseChainInt`、`QueryCommand`），退化为只做 `#{param}` 替换 + `Document.parse()` |
| `QueryCommand.java` | 可以独立出来，不再作为内部类 |
| `MyRepositoryProxyHandler.java` | @Mql 路径改为：读注解属性 → `#{param}` 替换 → `Document.parse(filter)` → 构建 `MqlQuerySpec` → `QueryExecutor` |
| `strategy/CommandExecutor.java` | 废弃，被 `QueryExecutor` 替代 |
| `strategy/ExecutorProxy.java` | 废弃 |
| `strategy/FindExecutor.java` 等 4 个 | 删除，逻辑合并到 `QueryExecutor` |

---

### Phase 4：LambdaAggregationWrapper —— 聚合/联表查询

**目标**：提供类型安全的聚合流水线构建器，覆盖 80% 高频场景（分组统计 + 简单联表），其余复杂场景由 `@Mql` 原始 JSON 兜底。

**核心洞察**：聚合的 `$match` 阶段就是一个 filter，直接复用 `MongoBsonRenderer.render()`，21 个 operator 零成本可用。

```
LambdaAggregationWrapper  ──┬── .match(LambdaQueryWrapper)  ──→  复用 MongoBsonRenderer
                            ├── .group(field).sum().count()  ──→  PipelineStage
                            ├── .lookup(from, local, foreign, as) ──→  PipelineStage
                            ├── .sort() / .limit() / .skip()       ──→  PipelineStage
                            └── .render()  ──→  List<Bson>  ──→  collection.aggregate()
```

#### 4a. 分组统计（最高频场景）

```java
// "每个客户的订单总额和订单数"
List<CustomerStats> stats = repo.aggregate(
    new LambdaAggregationWrapper<Order>()
        .match(w -> w.eq(Order::getStatus, "active"))        // 复用 LambdaQueryWrapper
        .group(Order::getCustomerId)                          // type-safe 字段引用
            .sum("totalAmount", Order::getAmount)             // $sum
            .count("orderCount")                              // $count
            .avg("avgAmount", Order::getAmount)               // $avg
            .max("maxAmount", Order::getAmount)               // $max
        .sort(false, "totalAmount")                           // 降序
        .limit(10)
        .as(CustomerStats.class)
);
```

对比手写 pipeline JSON：
```json
[
  {"$match": {"status": "active"}},
  {"$group": {
    "_id": "$customerId",
    "totalAmount": {"$sum": "$amount"},
    "orderCount": {"$sum": 1}}
  },
  {"$sort": {"totalAmount": -1}},
  {"$limit": 10}
]
```

#### 4b. 联表查询（$lookup 简化）

```java
// "订单连带客户信息"
List<OrderWithCustomer> results = repo.aggregate(
    new LambdaAggregationWrapper<Order>()
        .match(w -> w.gt(Order::getAmount, 100))
        .lookup("customers", Order::getCustomerId, "_id", "customer")
        .unwind("customer", true)
        .as(OrderWithCustomer.class)
);
```

对比手写 `$lookup`：
```json
{
  "$lookup": {
    "from": "customers",
    "localField": "customerId",
    "foreignField": "_id",
    "as": "customer"
  }
}
```

#### 4c. 核心实现

```java
public class LambdaAggregationWrapper<T> {
    private final Class<T> entityClass;
    private final List<PipelineStage> stages = new ArrayList<>();
    private Class<?> outputClass;

    // $match —— 核心复用点，LambdaQueryWrapper 的 21 个 operator 全部可用
    public LambdaAggregationWrapper<T> match(Consumer<LambdaQueryWrapper<T>> consumer) {
        LambdaQueryWrapper<T> wrapper = new LambdaQueryWrapper<>(entityClass);
        consumer.accept(wrapper);
        stages.add(new MatchStage(wrapper));
        return this;
    }

    // $group —— 类型安全的字段引用
    public GroupBuilder<T> group(SFunction<T, ?>... fields) { ... }

    // $lookup —— localField 支持 type-safe SFunction
    public LambdaAggregationWrapper<T> lookup(
            String from, SFunction<T, ?> localField,
            String foreignField, String as) { ... }

    // $sort / $limit / $skip
    public LambdaAggregationWrapper<T> sort(boolean asc, String field) { ... }
    public LambdaAggregationWrapper<T> limit(int n) { ... }
    public LambdaAggregationWrapper<T> skip(int n) { ... }

    // $unwind
    public LambdaAggregationWrapper<T> unwind(String field, boolean preserveNull) { ... }

    // $project
    @SafeVarargs
    public final LambdaAggregationWrapper<T> include(SFunction<T, ?>... fields) { ... }
    @SafeVarargs
    public final LambdaAggregationWrapper<T> exclude(SFunction<T, ?>... fields) { ... }

    // 输出类型
    public LambdaAggregationWrapper<T> as(Class<?> outputClass) { ... }

    // 渲染为 Bson pipeline
    List<Bson> render(MongoMappingConvertor convertor) {
        return stages.stream()
            .map(s -> s.toBson(convertor, entityClass))
            .collect(Collectors.toList());
    }
}
```

#### 4d. 接入 MongoRepository

```java
public interface MongoRepository<T, ID> {
    // 已有方法...

    /** 执行聚合查询 */
    <R> List<R> aggregate(LambdaAggregationWrapper<T> pipeline);

    /** 执行聚合，返回原始 Document（适用于不映射到 POJO 的结果） */
    List<Map<String, Object>> aggregateRaw(LambdaAggregationWrapper<T> pipeline);
}
```

#### 4e. 与 @Mql 原始 JSON 的分工

| 场景 | 使用 |
|------|------|
| group/sum/count/avg 统计 | `LambdaAggregationWrapper` — type-safe |
| 单表 `$lookup` 联表 | `LambdaAggregationWrapper` — 字段引用自动映射 |
| 多表嵌套 `$lookup` + 复杂条件 | `LambdaAggregationWrapper` 或 `@Mql` |
| `$bucket` / `$facet` / `$merge` 等高级 stage | `@Mql` — 直接写 JSON pipeline |
| `$text` / `$search` / `$geoNear` 等特殊 stage | `@Mql` — 直接写 JSON pipeline |

---

### Phase 5：Repository 接口分层

**目标**：拆出最小 API 接口，降低用户入门门槛。

```java
// 基础接口 — 最精简的 CRUD
public interface CrudRepository<T, ID> {
    T insert(T entity);
    T findById(ID id);
    List<T> findAll();
    long count();
    long deleteById(ID id);
    // 约 8-10 个方法
}

// 扩展接口 — 增加 Lambda 查询 + 聚合能力
public interface QueryRepository<T, ID> extends CrudRepository<T, ID> {
    T findOne(QuerySpec<T> query);
    List<T> findList(QuerySpec<T> query);
    long count(QuerySpec<T> query);
    long delete(QuerySpec<T> query);
    long update(QuerySpec<T> filter, T entity);
    <R> List<R> aggregate(LambdaAggregationWrapper<T> pipeline);
}

// 完整接口 — 当前 MongoRepository 的完整能力
public interface MongoRepository<T, ID> extends QueryRepository<T, ID> {
    // findOneByEntity, findOne(SFunction), findPage, upsert, etc.
    // 保留当前所有方法作为完整版
}
```

**改动范围**：

| 文件 | 改动 |
|------|------|
| 新建 `CrudRepository.java` | 基础接口 |
| 新建 `QueryRepository.java` | 查询 + 聚合扩展接口 |
| `MongoRepository.java` | 改为 `extends QueryRepository<T, ID>` |
| `SimpleMongoRepository.java` | 改为 `implements MongoRepository<T, ID>` |
| `RepositoryRegistrar.java` | 支持三种接口类型的声明 |

---

### Phase 6：代理分发插件化

**目标**：把 `MyRepositoryProxyHandler` 中的硬编码 if-else 改为可扩展的责任链。

```java
/**
 * 方法处理器 — 每种方法类型（@Mql、Repository 继承、未来扩展）各自实现。
 */
public interface MethodHandler {
    boolean supports(Method method);
    Object invoke(Method method, Object[] args) throws Exception;
}
```

改动范围：

| 文件 | 改动 |
|------|------|
| 新建 `MethodHandler.java` | 接口定义 |
| 新建 `MqlMethodHandler.java` | 处理 `@Mql` 注解方法 |
| 新建 `RepositoryMethodHandler.java` | 处理 `MongoRepository` 继承方法 |
| `MyRepositoryProxyHandler.java` | 改为委托给 handler 列表 |
| `RepositoryFactoryBean.java` | 装配 handler 列表 |

---

### Phase 7：补充缺失能力

| 能力 | 说明 |
|------|------|
| `LambdaUpdateWrapper` | 支持 `set(Entity::getField, value)` 链式部分字段更新 |
| `QueryWrapper<T>` | 字符串字段名版本的 query wrapper（类似 MyBatis Plus） |
| `existsById()` / `exists(QuerySpec)` | 存在性判断，避免 `count > 0` |
| `insertBatch()` / `save()` | 批量插入和 upsert-on-id |
| `@Mql` 写操作 | `InsertOneExecutor`、`UpdateOneExecutor` 等（Phase 2 合并到 QueryExecutor 后统一实现） |
| 方法名推导查询 | `findByNameAndAge` → 自动解析为 query |

---

## 四、实施优先级

| 优先级 | Phase | 理由 |
|--------|-------|------|
| P0 | Phase 1：`QuerySpec<T>` 抽象 | 所有后续工作的基础 |
| P1 | Phase 2：统一 `QueryExecutor<T>` | 消除重复，废弃 `strategy/` 包，让所有路径共享执行逻辑 |
| P2 | Phase 3：@Mql 简化 | 砍掉 QueryParser shell 解析器，消除冗余和安全问题 |
| P3 | Phase 4：LambdaAggregationWrapper | 聚合/联表查询的类型安全支持 |
| P4 | Phase 5：接口分层 | 渐进式 @Deprecated 过渡 |
| P5 | Phase 6：代理插件化 | 锦上添花，当前 if-else 够用 |
| P6 | Phase 7：补充能力 | 按需实现，不阻塞架构重构 |

---

## 五、向后兼容策略

1. **所有新增的接口和类放在新包 `query/` 中**，不影响 `v2/`、`lambda/` 的已有代码
2. `MongoRepository` 中的 `LambdaQueryWrapper` 参数方法保留，新增 `QuerySpec` 版本的重载，旧方法标记 `@Deprecated`
3. `@Mql` 旧语法共存一个过渡版本：如果 `value()` 以 `db.getCollection` 开头，按旧逻辑解析（输出 deprecation warning）；否则按新注解属性解析。过渡期后拆除旧逻辑
4. `SimpleMongoRepository` 内部先委托给 `QueryExecutor`，保持外部行为不变
5. `MyRepositoryProxyHandler` 的 if-else 保留，新增 handler 列表作为并行机制，逐步迁移
6. 每个 Phase 独立合入，单独验证

---

## 六、@Mql 新旧对比

| | 旧（shell 语法） | 新（注解属性 + 原始 JSON） |
|---|---|---|
| 语法 | `db.getCollection('x').find({...}).skip(20)` | `@Mql(value = "{...}", skip = 20)` |
| collection 来源 | shell 命令字符串里手写 | entity 的 `@CollectionName` |
| 操作类型来源 | 正则从 shell 命令提取 | 方法返回类型推断 |
| JSON 解析 | QueryParser 正则 + 手写分割 | `Document.parse()` — Driver 原生 |
| skip/limit/sort | 正则解析链式方法 | 注解属性 |
| `#{param}` 替换 | `args[i].toString()` | `MongoMappingConvertor` 类型安全转换 |
| 字段名映射 | 无（用户手写 MongoDB 名） | 可走 `MongoMappingConvertor` |
| 租户过滤 | 无（安全漏洞） | 可走 `QueryExecutor` 统一注入 |
| 代码量 | QueryParser 100+ 行 + strategy/ 包 200+ 行 | 注解属性几个字段 + `Document.parse()` 一行 |

---

## 七、目标架构对比

| | 当前 | 目标 |
|---|---|---|
| 查询抽象 | `LambdaQueryWrapper` 具体类 | `QuerySpec<T>` 接口，多个实现 |
| 执行引擎 | `SimpleMongoRepository` + `CommandExecutor` 两套 | `QueryExecutor<T>` 一套 |
| @Mql | shell 命令，手写解析器 | 注解属性 + `Document.parse()` |
| 聚合 | 无 | `LambdaAggregationWrapper`（复用 `$match`）+ `@Mql` 原始 JSON 兜底 |
| 三条路径关系 | 各自独立 | 汇聚于 `QuerySpec` + `QueryExecutor` |
| Repository 层次 | 平铺 35 个方法 | 三层继承：`CrudRepository` → `QueryRepository` → `MongoRepository` |
| 代理分发 | 硬编码 if-else | `MethodHandler` 责任链，可扩展 |
| 代码重复 | find/count/delete 逻辑在多个文件重复 | 集中在 `QueryExecutor` |
| strategy/ 包 | @Mql 专属执行器 | 废弃，逻辑合并到 `QueryExecutor` |

---

## 八、架构决策分析：BSON vs Mongo Command 与三条路径统一

> 2026-07-15 新增。本章节记录对当前架构的深层分析，为 Phase 1-3 的实施方案提供决策依据。

### 8.1 BSON or Mongo Command？

**MQL 查询的底层是 BSON，不是 Mongo Command。**

2026-07-11 的 `@Find`/`@Count`/`@Delete` 重构已经用 `JsonTemplateParser` + `Document.parse()` 替代了老的 `QueryParser` shell 命令解析器。这意味着：

```java
// 当前实现（正确）—— BSON 路径
@Find("{name: #{name}, level: #{level}}")
// → JsonTemplateParser → Document.parse(json) → Document（实现 Bson）
// → collection.find(document) → FindIterable<Document>
```

而不是：
```java
// 错误路径（已废弃）—— Mongo Command 路径
// → QueryParser 正则解析 "db.getCollection('x').find({...})"
// → 提取命令字符串 → db.runCommand(...) ← 这不是 CRUD 的正确方式
```

**为什么 BSON 是正确的选择：**

| 维度 | BSON（`MongoCollection` API） | Mongo Command（`db.runCommand()`） |
|---|---|---|
| 协议层 | Driver 封装层，标准的 CRUD 操作 | 原始 wire protocol，用于 admin/诊断操作 |
| 查询构建 | `Filters.eq()`、`Document.parse()`、`Bson` 接口 | 手写 `{ "find": "coll", "filter": {...} }` 命令文档 |
| 返回类型 | 类型安全的 `FindIterable<Document>`、`long` | 原始 `Document`，需手写结果解析 |
| 连接管理 | Driver 自动处理连接池、重试、读写分离 | 需自行管理 |
| 用途 | CRUD 查询的正确方式 | `serverStatus`、`buildInfo`、`createIndexes`、`mapReduce` 等管理命令 |
| 代表方法 | `collection.find()`、`collection.aggregate()` | `database.runCommand(new Document("serverStatus", 1))` |

MongoDB Java Driver 的设计本身就把这两层分开了：`MongoCollection` 负责 CRUD，`MongoDatabase.runCommand()` 负责管理命令。mongo-flex 作为一个 ORM/数据访问工具，正确且唯一的交互层应该是 `MongoCollection` + `Bson`。

### 8.2 三条路径的统一现状

当前三条路径已经在外层收敛于 `Bson` → `MongoCollection`：

```
@Find("{name: #{name}}")         entity                          LambdaQueryWrapper
        │                           │                                     │
  JsonTemplateParser          MongoMappingConvertor              MongoBsonRenderer
   #{} 替换 + JSON 拼接        字段 → Document                     Condition → Filters.*
        │                           │                                     │
        ▼                           ▼                                     ▼
  Document.parse(json)       new Document(k, v)         Filters.eq/ne/gt/.../and/or
        │                           │                                     │
        ▼                           ▼                                     ▼
      Document ─────────────────── Document ──────────────── Filters 结果
        │                           │                                     │
        └───────────────────────────┴─────────────────────────────────────┘
                                        │
                                        ▼
                               Bson（三者都实现）
                                        │
                                        ▼
                        collection.find(Bson)
                        collection.countDocuments(Bson)
                        collection.deleteOne(Bson)
                        collection.updateOne(Bson, ...)
```

**关键洞察：输入不同，中间层必然不同，外层已经统一。**

| 层 | MQL | Entity | Lambda | 能否统一？ |
|---|---|---|---|---|
| 输入 | JSON 字符串 | 实体对象 | 方法引用 | ❌ 不可统一（本质不同） |
| 中间 | `Document.parse()` | `new Document(k,v)` | `Filters.*` | ❌ 不必统一（各有最优路径） |
| 外层 | `Bson` | `Bson` | `Bson` | ✅ **已经统一** |
| 执行 | `MongoCollection` | `MongoCollection` | `MongoCollection` | ✅ **已经统一** |

这说明 `QuerySpec<T>` 抽象（Phase 1）绝非空中楼阁——它只是对**已有事实的形式化表达**。`MongoBsonRenderer.render()` 返回的是 `Bson`，`Document.parse()` 返回的是 `Document`（实现 `Bson`），`JsonTemplateParser` 产出的也是 `Document`。它们都已经可以是 `QuerySpec.toBson()` 的不同实现了。

### 8.3 为什么不把 MQL 也转成 Filters？

理论上可以让 `JsonTemplateParser` 把解析后的 `Document` 反向拆解为 `Filters.eq()` / `Filters.gt()` 等，但这是**过度设计**：

1. **`Document.parse()` 是 MongoDB Driver 原生能力**——Driver 内置的 JSON → BSON 转换器，不需要自己造轮子
2. **JSON filter 的语法空间远大于 `Filters` API**——`$expr`、`$jsonSchema`、`$text`、`$geoWithin` 等操作符在 `Filters` 里没有对应方法。要全覆盖等于在 `Filters` 上加一个 query parser，这正是已删除的 `QueryParser` 做过的错事
3. **`Document` 已经实现了 `Bson`**——`Document.parse()` 的产物可以直接传给 `collection.find()`，不需要中间转换

**唯一需要统一的是执行层**——这正是 Phase 2 做的事。当前 `@Find`/`@Count`/`@Delete` 走 `MyRepositoryProxyHandler` → `SimpleMongoRepository` 的 `findByFilter`/`countByFilter`/`deleteByFilter`，已经不经过 `CommandExecutor` 了。老的 `strategy/` 包已经是死代码。

### 8.4 与 Spring Data MongoDB 的对比

Spring Data MongoDB 的统一层是 `MongoTemplate`：

```java
// Spring Data 的所有查询路径最终都到这里
mongoTemplate.find(query, entityClass, collectionName);
mongoTemplate.count(query, entityClass, collectionName);
```

`Query` 对象持有 `Document` filter + sort + skip/limit，`MongoTemplate` 统一执行。mongo-flex 的 `QuerySpec<T>` + `QueryExecutor<T>` 本质上是同一个模式——只是 `QuerySpec` 比 Spring Data 的 `Query` 更抽象（Spring Data 的 `Query` 只能包 `Document`，不支持直接包 `Filters`），而 `Bson` 接口天然就是比 `Document` 更宽泛的抽象。

### 8.5 MongoDB Driver 版本兼容的架构影响

（详见 `ISSUES.md` M-13）

mongo-flex 引用 17 个 MongoDB Driver 类，其中 14 个在 4.x 和 5.x 之间 API 稳定。仅 3 个不兼容：

- `ConnectionString.getDatabase()` — 连接初始化
- `InsertOneResult.getInsertedId()` → `BsonValue` — 插入回填 ID
- `InsertManyResult.getInsertedIds()` — 批量插入回填 ID

这三处都需要 bridge 适配，建议放在 `QueryExecutor`（Phase 2）中统一处理，不污染 `QuerySpec` 抽象层。V4 适配器直接调用（零开销），V5 适配器纯反射（不 import 5.x 类）。

---

## 九、嵌套字段查询（点号路径）—— 已实施

> 2026-07-19 实施。设计参考 Spring Data MongoDB：统一基座是"点号字符串路径 + 逐段字段名映射"（对标 `QueryMapper`），类型安全入口对标 `TypedPropertyPath.of().then()`。

### 9.1 核心机制

`Condition.field` 从"单段 Java 字段名"扩展为"Java 点号路径"（如 `address.city`），`Condition`/`Operator`/渲染骨架零改动。

| 组件 | 改动 |
|------|------|
| `MongoMappingConvertor.resolveMongoFieldName` | **重命名为 `resolveMongoFieldPath`**：按 `.` 拆段，逐段查 `ClassFieldMetaData`（每段独立应用 `@CollectionField`/`@CollectionId`/隐式 `id→_id`），段间用 `FieldMapping.fieldType` 推进到嵌套类型；`List` 段穿透泛型元素类型（MongoDB 数组点号语义）；基础/系统类型不可继续导航；未知段起原样保留。单段输入行为与旧版完全一致 |
| `getFieldGenericElementType` | 同步支持点号路径（elemMatch 嵌套 List 场景） |
| 新建 `util/FieldPath<T, R>` | 链式方法引用：`FieldPath.of(User::getAddress).then(Address::getCity)`，两级/三级便捷工厂 `of(f1, f2[, f3])`；不可变；构造时即解析 `javaPath` + `rootImplClass` |
| `LambdaQueryWrapper` | 20 个 filter 操作符 + `orderByAsc/Desc` + `include/exclude` 各增加 `FieldPath` 重载，统一走私有 `addPathCondition()` |

### 9.2 三条路径的嵌套语义

| 路径 | 嵌套支持 | 说明 |
|------|---------|------|
| Lambda（`FieldPath`） | ✅ 逐字段点号匹配 | 每段自动应用 `@CollectionField` 映射，如 `region.mainCity` → `home_region.main_city` |
| `@Find`/`@Count`/`@Delete` 原始 JSON | ✅ 点号键原生直通 | `Document.parse()` 原生能力，用户手写 MongoDB 字段名 |
| `*ByEntity` 实体查询 | ⚠️ **单层语义（有意保留）** | 嵌套对象字段按**精确子文档**匹配（全部字段+顺序一致才命中），不展平为点号。需要逐字段嵌套匹配请改用 FieldPath 或 @Find 点号 JSON |

### 9.3 破坏性变更

- `resolveMongoFieldName` → `resolveMongoFieldPath` 直接重命名，不保留旧方法（内部调用点 5 处已同步）。
- 操作符重载导致裸 `null` 字段参数产生编译歧义（如 `eq(null, v)`），需显式转型 `(SFunction<T,R>) null`——仅影响传字面量 null 的调用（正常业务代码不受影响）。

### 9.4 测试

- core 纯单元测试（无需 MongoDB）：`ResolveMongoFieldPathTest`（单段回归/多段映射/List 穿透/未知段 fallback）、`FieldPathTest`（解析/不可变/相等性）、`NestedFieldPathRenderTest`（渲染 Bson 断言，含 elemMatch/or 分组/排序投影）
- boot3 集成测试：`NestedFieldQueryTest`（`Character.region` → `home_region` 子文档；FieldPath eq/gt/between/exists/分页排序、`@Find` 点号 JSON、写读回环、byEntity 精确子文档语义固化）
