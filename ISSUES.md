# mongo-flex 代码问题清单

## 严重（Critical）

### C-1. ~~空 LambdaQueryWrapper 执行 delete/update 会操作全集合~~ ✅ 已解决
**文件:** `src/main/java/com/github/eacryo/mongoflex/lambda/MongoBsonRenderer.java:92`
**问题:** `Filters.and(filters)` 中 filters 为空 ArrayList 时返回空 BsonDocument，匹配所有文档。`delete(new LambdaQueryWrapper<>())` 或 `update(new LambdaQueryWrapper<>(), entity)` 会删改全集合。
**解决方案:** 在 `SimpleMongoRepository` 的 `delete(LambdaQueryWrapper)` / `update(LambdaQueryWrapper, T entity)` 中检查 conditions 是否为空，为空则抛 `IllegalArgumentException`（提示使用 `deleteAll()` / `updateAll(T entity)`）。同时新增 `deleteAll()` / `updateAll(T entity)` 方法作为全量操作的显式入口。读操作（`findOne`/`findList`/`count`）不受此限制。

### C-2. ~~findOneByEntity / count / deleteByEntity 传入 null 实体 NPE~~ ✅ 已解决
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/SimpleMongoRepository.java`
**问题:** `mongoMappingConvertor.write(entity)` 在 entity 为 null 时返回 null，后续调用 `query.get("_id")` 触发 NPE。
**解决方案:** 所有 public 方法加 `Objects.requireNonNull` 校验，null 入参直接抛 `IllegalArgumentException`。

### C-3. ~~updateById / update / update(LambdaQueryWrapper) 传入 null 实体 NPE~~ ✅ 已解决
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/SimpleMongoRepository.java`
**问题:** 同 C-2，`doc.remove("_id")` 触发 NPE。
**解决方案:** 同 C-2。

### C-4. ~~FindExecutor ClassCastException — 不处理原始 List 返回类型~~ ✅ 已解决
**文件:** `src/main/java/com/github/eacryo/mongoflex/strategy/FindExecutor.java:26`
**问题:** `(ParameterizedType) genericReturnType` — 如果 @Mql 方法返回原始 `List`（不带泛型），强制转型抛 `ClassCastException`。
**修复:** 先检查 `instanceof ParameterizedType`，否则回退到 `Object.class`。

### C-5. FindExecutor 对非 List 返回类型行为错误
**文件:** `src/main/java/com/github/eacryo/mongoflex/strategy/FindExecutor.java:37-42`
**问题:** 返回类型不是 List 时只取第一条结果回传，导致调用方拿到错误类型。
**修复:** 强制仅支持 `List<T>` 或完善 `Optional`、`Stream` 等类型处理。

### C-6. @Mql 方法 null 参数 NPE（toString）
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/MyRepositoryProxyHandler.java:104`
**问题:** `args[i].toString()` 在参数为 null 时 NPE。MongoDB 查询中 null 是合法的（如 `{ field: null }`）。
**修复:** null 时拼 `"null"` 或使用 JSON 序列化。

### C-7. ~~DynamicMongoClient 初始化失败时吞异常~~ ✅ 已解决
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/DynamicMongoClient.java:78-95`
**问题:** `initWhenEnabled()` 中某个租户的 MongoClient 创建失败只记日志不抛异常，应用看似启动成功但后续请求报 NPE。
**解决方案:** 收集所有租户的错误，处理完所有租户后统一抛 `RuntimeException`（与 `initWhenDisabled` 行为一致）。同时清理了多余分号和未使用的 Logger import。

### C-8. ~~MongoBsonRenderer entityClass 为 null 时字段名不转换~~ ✅ 已解决
**文件:** `src/main/java/com/github/eacryo/mongoflex/lambda/MongoBsonRenderer.java:22-24`
**问题:** entityClass 为 null 时直接用 Java 字段名，`id` 不会映射为 `_id`，`@CollectionField` 注解失效。
**分析:**
- `SimpleMongoRepository` 所有方法在调用 `render()` 之前都会执行 `ensureEntityClass(wrapper)` 自动补齐 entityClass，走 Repository API 不会触发此问题。
- 只有绕开 `SimpleMongoRepository`、直接调用 `MongoBsonRenderer.render()` 且 wrapper 未设置 entityClass 时才会出现。
- 读路径（`MongoMappingConvertor.read()`）使用构造时固定的 `entityClass`，不受影响。
**处理:** 已在 `MongoBsonRenderer` 类上加 Javadoc 说明不直接对外使用。无需进一步修复。

### C-9. CollectionNameUtil.select() 无下划线时返回 tenantId 而非 collectionName
**文件:** `src/main/java/com/github/eacryo/mongoflex/util/CollectionNameUtil.java:48`
**问题:** `tenantIdParts.length <= 1` 时返回 `tenantId` 作为集合名，而非预期的 `collectionName`。
**修复:** 改为 `return collectionName`。

### C-10. CollectionNameUtil.select() MDC 为 null 时 NPE
**文件:** `src/main/java/com/github/eacryo/mongoflex/util/CollectionNameUtil.java:44`
**问题:** `MDC.get(MongoFlexConstant.TENANT)` 为 null 时 `tenantId.split("_")` NPE。
**修复:** 加 null 检查：`if (tenantId == null) return collectionName`。

### C-11. QueryParser 正则只认单引号，不认双引号
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/QueryParser.java:9`
**问题:** `db\.getCollection\('(.*?)'\)` 只匹配单引号，合法的 MongoDB shell 双引号写法报错。
**修复:** `db\.getCollection\(['\"](.*?)['\"]\)`。

---

## 高（High）

### H-1. findOneByEntity 空实体返回随机文档 — 设计如此
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/SimpleMongoRepository.java`
**问题:** 实体所有字段为 null 时 `write(entity)` 生成空 `Document{}`，`find({}).first()` 返回集合自然顺序第一条文档。
**决定:** 读操作允许空条件（与 MyBatis-Plus 行为一致），仅写操作（delete/update）拒绝空条件。

### H-2. count(T entity) 空实体返回总数 — 设计如此
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/SimpleMongoRepository.java`
**问题:** 同 H-1，空实体统计返回集合总文档数。
**决定:** 同 H-1。

### H-3. IdGenerator<?> 类型不安全
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/SimpleMongoRepository.java:46`
**问题:** 无界通配符，ID 生成器产出类型与实体 ID 字段类型不匹配只在运行时报错。
**修复:** 泛型化：`IdGenerator<ID>`，沿 `SimpleMongoRepository<T, ID>` 传递。

### H-4. convertQueryId 未检查的类型转换
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/SimpleMongoRepository.java:310`
**问题:** `convertIdIfNecessary((ID) id)` 将 Object 强转为 ID，堆污染风险。
**修复:** 用正确类型检查替代强转。

### H-5. DynamicMongoClient select() 返回错误客户端
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/DynamicMongoClient.java:49-55`
**问题:** 非多租户模式下选 `DEFAULT_TENANT_WHEN_DISABLE`，如果未初始化则 NPE。
**修复:** @PostConstruct 验证 clients map 非空。

### H-6. CountExecutor 返回值类型截断
**文件:** `src/main/java/com/github/eacryo/mongoflex/strategy/CountExecutor.java:21-25`
**问题:** 返回类型为 int 时 `count.intValue()` 可能静默溢出（集合超 21 亿文档时）。
**修复:** 溢出时抛 `ArithmeticException`，或只支持 `long`。

### H-7. @Mql 参数值直接 toString 拼入 JSON，无转义
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/MyRepositoryProxyHandler.java:104`
**问题:** `args[i].toString()` 直接拼入 JSON 字符串，值中含引号或特殊字符导致 JSON 格式错误。
**修复:** 用 JSON 序列化替代 `toString()`。

### H-8. ClassFieldMetaData 无条件 setAccessible(true)
**文件:** `src/main/java/com/github/eacryo/mongoflex/convertor/ClassFieldMetaData.java:35`
**问题:** 对所有字段（包括 static final 常量）调用 `setAccessible(true)`，JPMS 下可能抛 `InaccessibleObjectException`。
**修复:** 跳过 static/final 字段，或推迟到实际访问时才 setAccessible。

### H-9. ~~RepositoryRegistrar 扫描了错误的包~~ ✅ 已解决
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/RepositoryRegistrar.java:31`
**问题:** basePackage 取的是库自身的包 `com.github.eacryo.mongoflex.v2`，用户应用的 `@MRepository` 接口永远不会被扫描到。
**修复:** 通过 `AutoConfigurationPackages` 获取用户包路径，多包路径逐一扫描并集，`registry` 不支持或返回空时回退到原行为。

---

## 中（Medium）

### M-1. ~~ExecutorProxy 静态字段跨 Spring 上下文污染~~ ✅ 已解决
**文件:** `src/main/java/com/github/eacryo/mongoflex/strategy/ExecutorProxy.java:13`
**问题:** `static final Map` 在多 ApplicationContext（如测试）间共享，注册泄露。
**解决方案:** 去掉 `static`。`ExecutorProxy` 本身是 Spring 单例 Bean，同一 ApplicationContext 内所有消费者拿到的都是同一个实例，实例字段天然提供正确的隔离边界。

### M-2. ~~SimpleMongoRepository 构造函数不校验 null~~ ✅ 已解决
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/SimpleMongoRepository.java:49-57`
**问题:** 5 个构造参数都不做 null 检查，后续 NPE 难定位。
**修复:** 各参数加 `Objects.requireNonNull`。

### M-3. ~~MyRepositoryProxyHandler 构造函数不校验 null~~ ✅ 已解决
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/MyRepositoryProxyHandler.java:32-42`
**问题:** 同 M-2。
**修复:** 同 M-2。

### M-4. FindExecutor / FindOneExecutor 忽略 command 和 args
**文件:** `strategy/FindExecutor.java:24`，`strategy/FindOneExecutor.java:21-28`
**问题:** 不支持 limit、skip、sort、projection，@Mql 查询表达能力受限。
**修复:** 文档化限制或扩展解析。

### M-5. @Mql QueryParser 不校验是否已注册 Executor
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/QueryParser.java:16`
**问题:** 正则捕获 9 种操作但只注册了 4 种 Executor，其他操作解析成功但执行时才报 UnsupportedOperationException。
**修复:** 解析后校验操作是否已注册，提前报错。

### M-6. MongoMappingConvertor.write(null) 返回 null
**文件:** `src/main/java/com/github/eacryo/mongoflex/convertor/MongoMappingConvertor.java:88-90`
**问题:** 返回 null 而非抛明确异常，所有调用方必须自行判空。
**修复:** 抛 `IllegalArgumentException("entity must not be null")`。

### M-7. SimpleMongoRepository.findList 全量加载到内存
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/SimpleMongoRepository.java:120`
**问题:** `into(new ArrayList<>())` 一次加载全部结果，大数据集 OOM。
**修复:** 加分页支持，或提供流式/游标 API。

### M-8. ~~LambdaQueryWrapper 不支持 OR 逻辑~~ ✅ 已解决
**文件:** `lambda/LambdaQueryWrapper.java`
**问题:** 所有条件固定 AND 连接，无法表达 `WHERE a=1 OR b=2`。
**修复:** 添加 `or()` 和 `or(LambdaQueryWrapper<T>)` 方法。`or()` 插入 sentinel 分割条件组，渲染时组内 AND、组间 OR。

### M-9. ReflectUtil.getFieldNameFromLambda 对 boolean getter 前缀处理不准确
**文件:** `src/main/java/com/github/eacryo/mongoflex/util/ReflectUtil.java:31-37`
**问题:** `isActive()` 始终解析为 `active`，但实际字段名可能是 `isActive`（`is` 不是 getter 前缀时）。
**修复:** 加返回值类型检查，仅 `boolean`/`Boolean` 时才去 `is` 前缀。

### M-10. 泛型解析只检查直接接口，不支持层级接口继承
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/RepositoryRegistrar.java:43-55`
**问题:** `interface UserRepo extends BaseRepo<User, String>` 且 `BaseRepo extends MongoRepository` 时泛型解析失败。
**修复:** 用递归泛型解析遍历接口层级。

### M-11. DateValueGenerator 只支持 Date 和 String
**文件:** `src/main/java/com/github/eacryo/mongoflex/util/DateValueGenerator.java:20-28`
**问题:** `LocalDateTime`、`LocalDate`、`Instant` 等常见类型不支持，抛 `IllegalArgumentException`。
**修复:** 增加 Java 8+ 时间类型支持。

### M-12. Insert 回填ID 时 ID 字段可能会没有 @CollectionId 注解
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/SimpleMongoRepository.java:78`
**问题:** write/read 层隐式映射 `id → _id`，但 post-insert ID 回填只认 @CollectionId 注解，不标注不会收到回填 ID。
**修复:** 统一 ID 字段发现逻辑。

---

## 低（Low）

### L-1. FillConstant 字段非 final
**文件:** `src/main/java/com/github/eacryo/mongoflex/constant/FillConstant.java:4-7`
**修复:** 加 `final` 修饰。

### L-2. ~~DynamicMongoClient 多余分号~~ ✅ 已解决
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/DynamicMongoClient.java`

### L-3. ~~SimpleMongoRepository.buildFilterFromMap 死代码~~ ✅ 已解决
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/SimpleMongoRepository.java`
**解决方案:** 已删除。

### L-4. MyRepositoryProxyHandler.mapDocumentToEntity 死代码
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/MyRepositoryProxyHandler.java:84-93`
**修复:** 删除。

### L-5. 未使用的依赖 java-uuid-generator
**文件:** `pom.xml:105-108`
**修复:** 移除或实际使用。

### L-6. 拼写不一致 "Convertor" vs "Converter"
**文件:** `convertor/` 整个包
**修复:** 重命名为 Converter（breaking change，可先 deprecate）。

### L-7. MongoFlexConstant 允许实例化
**文件:** `src/main/java/com/github/eacryo/mongoflex/constant/MongoFlexConstant.java`
**修复:** 加 `private` 构造器。

### L-8. FillConstant 允许实例化
**文件:** `src/main/java/com/github/eacryo/mongoflex/constant/FillConstant.java`
**修复:** 加 `private` 构造器。

### L-9. PageDTO 用装箱 Long，可为 null
**文件:** `src/main/java/com/github/eacryo/mongoflex/entity/PageDTO.java:12-15`
**修复:** 改用 `long` 基础类型 + 默认值。

### L-10. PageDTO.orderBy 不支持逐字段升降序
**文件:** `src/main/java/com/github/eacryo/mongoflex/entity/PageDTO.java:16`
**修复:** 用 `List<SortOrder>`（含 field + ascending 标志）。

### L-11. DeleteOneExecutor 导入了未使用的内部 API
**文件:** `src/main/java/com/github/eacryo/mongoflex/strategy/DeleteOneExecutor.java:6`
**修复:** 删除 `import com.mongodb.internal.bulk.DeleteRequest`。

### L-12. CollectionNameUtil 有未使用的 Logger 字段
**文件:** `src/main/java/com/github/eacryo/mongoflex/util/CollectionNameUtil.java:17`
**修复:** 使用或删除。

### L-13. ~~DynamicMongoClient 未使用的 Logger/LoggerFactory import~~ ✅ 已解决

### L-14. MongoBsonRenderer ELEM_MATCH 使用了原始类型强转
**文件:** `src/main/java/com/github/eacryo/mongoflex/lambda/MongoBsonRenderer.java:81`
**修复:** 加 `@SuppressWarnings("unchecked")` 或声明正确泛型变量。

### L-15. 各 Executor 注入 ExecutorProxy 仅用于注册
**文件:** FindExecutor、FindOneExecutor、CountExecutor、DeleteOneExecutor
**修复:** 改为 ExecutorProxy 通过 `List<CommandExecutor>` 自动发现并注册。
**暂缓原因:** 当前 push 模式（Executor 自我注册）功能正确，M-1 已通过去掉 `static` 解决了跨 Context 污染问题，剩余价值仅为减少样板代码，是纯风格改进而非 bug。若要改为 pull 模式，需给 `CommandExecutor` 接口新增方法（如 `String getOperation()`）以识别每个 Executor 处理的操作类型，改动面扩大。建议在未来新增 Executor（insertOne、updateOne 等）时一次性重构，届时样板代码增多，收益更大。

### L-16. AutoConfiguration.imports 列入了非 AutoConfiguration 类
**文件:** `src/main/resources/META-INF/spring/...AutoConfiguration.imports`
**修复:** 移除 MongoFlexProperties 和 CollectionNameUtil，仅保留 MyOrmAutoConfiguration。

### L-17. Condition record 不做 null 校验
**文件:** `src/main/java/com/github/eacryo/mongoflex/lambda/Condition.java:4-8`
**修复:** 紧凑构造器中加 `requireNonNull`。

### L-18. pom.xml parent POM 被注释
**文件:** `pom.xml:5-10`
**修复:** 恢复 `spring-boot-starter-parent` 或显式管理所有 Spring 依赖版本。

### L-19. pom.xml 发布元数据为空
**文件:** `pom.xml:41-56`
**修复:** 填写 url、licenses、developers、scm 或移除。

### L-20. MongoFlexProperties.getDatabaseFromUri 每次 new ConnectionString
**文件:** `src/main/java/com/github/eacryo/mongoflex/config/MongoFlexProperties.java:24-39`
**修复:** 非多租户模式缓存结果。

---

## 统计

| 严重度 | 数量 | 主要类别 |
|--------|------|----------|
| Critical | 11 | 数据丢失、NPE、类转换异常、静默错误结果 |
| High | 9 | 类型安全、静默查询失败、异常吞没 |
| Medium | 12 | 校验缺失、OOM 风险、查询表达能力、错误信息 |
| Low | 20 | 死代码、代码风格、未用依赖、元数据缺失 |
| **合计** | **52** | |

---

> 建议按 Critical → High → Medium → Low 顺序逐项修复，同一等级内按编号顺序处理。
