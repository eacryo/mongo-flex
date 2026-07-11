# mongo-flex 架构重构方案

> **状态**：待实施  
> **创建日期**：2026-07-11

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
```

### 核心设计原则

1. **`QuerySpec<T>` 作为统一查询抽象** — 所有查询条件（无论是 Entity、Lambda、MQL）都实现此接口，产出 `Bson`
2. **`QueryExecutor<T>` 作为统一执行引擎** — 所有 CRUD 操作（find、count、delete、update）汇入单一执行器
3. **Repository 接口分层** — 拆分为基础 `CrudRepository` + 扩展 `MongoRepository`，用户可选声明
4. **代理分发插件化** — 用 `MethodHandler` 责任链替代硬编码 if-else

---

## 三、重构步骤

### Phase 1：统一查询抽象 `QuerySpec<T>`

**目标**：让 `LambdaQueryWrapper` 和未来的 MQL 查询条件产出物都实现同一个接口。

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
    private final CollectionNameUtil collectionNameUtil;

    public List<T> find(QuerySpec<T> querySpec) { ... }
    public T findOne(QuerySpec<T> querySpec) { ... }
    public long count(QuerySpec<T> querySpec) { ... }
    public boolean delete(QuerySpec<T> querySpec) { ... }
    public long deleteMany(QuerySpec<T> querySpec) { ... }
    public long update(QuerySpec<T> filter, T entity) { ... }
    public long upsert(QuerySpec<T> filter, T entity) { ... }
}
```

**改动范围**：

| 文件 | 改动 |
|------|------|
| 新建 `query/QueryExecutor.java` | 统一执行引擎 |
| `SimpleMongoRepository.java` | 委托给 `QueryExecutor`，自身只做参数适配 |
| `FindExecutor.java` / `FindOneExecutor.java` 等 | 改为委托 `QueryExecutor`，@Mql 路径不再重复实现 |
| `CommandExecutor.java` 接口 | 可以废弃或改为 thin wrapper |

---

### Phase 3：@Mql 产出 `MqlQuerySpec`，融入统一抽象

**目标**：让 @Mql 注解查询也产出 `QuerySpec<T>`，汇入统一执行引擎。

```java
package com.github.eacryo.mongoflex.v2;

/**
 * @Mql 解析结果，实现 QuerySpec 以融入统一查询抽象。
 */
public class MqlQuerySpec<T> implements QuerySpec<T> {

    private final String shellCommand;
    private final Document queryDoc;       // filter（第一个参数）
    private final List<Document> extraArgs; // projection、update、options 等

    @Override
    public Bson toBson(MongoMappingConvertor convertor) {
        return queryDoc;  // Document extends Bson
    }

    @Override
    public Class<T> getEntityClass() {
        return entityClass;
    }
}
```

**改动范围**：

| 文件 | 改动 |
|------|------|
| `QueryParser.QueryCommand` | 保留，但额外生成 `MqlQuerySpec` |
| 新建 `MqlQuerySpec.java` | 实现 `QuerySpec<T>` |
| `MyRepositoryProxyHandler.java` | `@Mql` 路径改为：parse → `MqlQuerySpec` → `QueryExecutor` |

---

### Phase 4：Repository 接口分层

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

// 扩展接口 — 增加 Lambda 查询能力
public interface QueryRepository<T, ID> extends CrudRepository<T, ID> {
    T findOne(QuerySpec<T> query);
    List<T> findList(QuerySpec<T> query);
    long count(QuerySpec<T> query);
    long delete(QuerySpec<T> query);
    long update(QuerySpec<T> filter, T entity);
    // ...等
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
| 新建 `QueryRepository.java` | 查询扩展接口 |
| `MongoRepository.java` | 改为 `extends QueryRepository<T, ID>` |
| `SimpleMongoRepository.java` | 改为 `implements MongoRepository<T, ID>` |
| `RepositoryRegistrar.java` | 支持三种接口类型的声明 |

---

### Phase 5：代理分发插件化

**目标**：把 `MyRepositoryProxyHandler` 中的硬编码 if-else 改为可扩展的责任链。

```java
package com.github.eacryo.mongoflex.v2;

/**
 * 方法处理器 — 每种方法类型（@Mql、Repository 继承、未来扩展）各自实现。
 */
public interface MethodHandler {
    /**
     * 返回 true 表示此 handler 能处理该方法。
     */
    boolean supports(Method method);

    /**
     * 处理方法调用。
     */
    Object invoke(Method method, Object[] args) throws Exception;
}
```

```java
// 在 MyRepositoryProxyHandler 中：
private final List<MethodHandler> handlers;

@Override
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    for (MethodHandler handler : handlers) {
        if (handler.supports(method)) {
            return handler.invoke(method, args);
        }
    }
    throw new UnsupportedOperationException("No handler for method: " + method.getName());
}
```

**改动范围**：

| 文件 | 改动 |
|------|------|
| 新建 `MethodHandler.java` | 接口定义 |
| 新建 `MqlMethodHandler.java` | 处理 `@Mql` 注解方法 |
| 新建 `RepositoryMethodHandler.java` | 处理 `MongoRepository` 继承方法 |
| `MyRepositoryProxyHandler.java` | 改为委托给 handler 列表 |
| `RepositoryFactoryBean.java` | 装配 handler 列表 |

---

### Phase 6：补充缺失能力

| 能力 | 说明 |
|------|------|
| `LambdaUpdateWrapper` | 支持 `set(Entity::getField, value)` 链式部分字段更新 |
| `QueryWrapper<T>` | 字符串字段名版本的 query wrapper（类似 MyBatis Plus） |
| `existsById()` / `exists(QuerySpec)` | 存在性判断，避免 `count > 0` |
| `insertBatch()` / `save()` | 批量插入和 upsert-on-id |
| `@Mql` 写操作 executor | `InsertOneExecutor`、`UpdateOneExecutor` 等 |
| 方法名推导查询 | `findByNameAndAge` → 自动解析为 query |

---

## 四、实施优先级

| 优先级 | Phase | 理由 |
|--------|-------|------|
| P0 | Phase 1：`QuerySpec<T>` 抽象 | 所有后续工作的基础，改动集中在 `lambda` 包和新增 `query` 包，对外部影响可控 |
| P1 | Phase 2：统一 `QueryExecutor<T>` | 消除重复代码，让 @Mql 和 Repository 路径共享执行逻辑 |
| P2 | Phase 3：`MqlQuerySpec` | 让 @Mql 融入统一抽象，三条路径首次汇合 |
| P3 | Phase 4：接口分层 | API 兼容性需要仔细设计（@Deprecated 过渡），可逐步推进 |
| P4 | Phase 5：代理插件化 | 锦上添花，当前 if-else 在功能上够用 |
| P5 | Phase 6：补充能力 | 按需实现，不阻塞架构重构 |

---

## 五、向后兼容策略

1. **所有新增的接口和类放在新包 `query/` 中**，不影响 `v2/`、`lambda/`、`strategy/` 的已有代码
2. `MongoRepository` 中的 `LambdaQueryWrapper` 参数方法保留，新增 `QuerySpec` 版本的重载，旧方法标记 `@Deprecated`
3. `SimpleMongoRepository` 内部先委托给 `QueryExecutor`，保持外部行为不变
4. `MyRepositoryProxyHandler` 的 if-else 保留，新增 handler 列表作为并行机制，逐步迁移
5. 每个 Phase 独立合入，单独验证

---

## 六、目标架构对比

| | 当前 | 目标 |
|---|---|---|
| 查询抽象 | `LambdaQueryWrapper` 具体类 | `QuerySpec<T>` 接口，多个实现 |
| 执行引擎 | `SimpleMongoRepository` + `CommandExecutor` 两套 | `QueryExecutor<T>` 一套 |
| 三条路径关系 | 各自独立 | 汇聚于 `QuerySpec` + `QueryExecutor` |
| Repository 层次 | 平铺 35 个方法 | 三层继承：`CrudRepository` → `QueryRepository` → `MongoRepository` |
| 代理分发 | 硬编码 if-else | `MethodHandler` 责任链，可扩展 |
| 代码重复 | find/count/delete 逻辑在多个文件重复 | 集中在 `QueryExecutor` |
