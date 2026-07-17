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

### C-5. ~~FindExecutor 对非 List 返回类型行为错误~~ ✅ 已知限制
**文件:** `src/main/java/com/github/eacryo/mongoflex/strategy/FindExecutor.java:37-42`
**问题:** 返回类型不是 List 时只取第一条结果回传，导致调用方拿到错误类型。
**状态:** MQL 体系已知限制，当前仅支持 `List<T>` 返回类型。`Optional`、`Stream` 等类型留待后续扩展。

### C-6. ~~@Mql 方法 null 参数 NPE（toString）~~ ✅ 已知限制
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/MyRepositoryProxyHandler.java:104`
**问题:** `args[i].toString()` 在参数为 null 时 NPE。MongoDB 查询中 null 是合法的（如 `{ field: null }`）。
**状态:** MQL 参数占位符替换的已知限制，当前需调用方避免传 null 或使用 JSON 序列化替代。

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

### C-9. ~~CollectionNameUtil.select() 无下划线时返回 tenantId 而非 collectionName~~ ✅ 已解决
**文件:** `src/main/java/com/github/eacryo/mongoflex/util/CollectionNameUtil.java:48`
**问题:** `tenantIdParts.length <= 1` 时返回 `tenantId` 作为集合名，而非预期的 `collectionName`。单参版本返回 `tenantId`（错误），双参版本返回 `collectionName`（正确），两个重载行为不一致。
**修复:** 单参版本改为 `return collectionName`，与双参版本一致。

### C-10. ~~CollectionNameUtil.select() MDC 为 null 时 NPE~~ ✅ 已解决
**文件:** `src/main/java/com/github/eacryo/mongoflex/util/CollectionNameUtil.java:44`
**问题:** `MDC.get(MongoFlexConstant.TENANT)` 为 null 时 `tenantId.split("_")` NPE。非多租户模式下无租户上下文，MDC 中无此 key。
**修复:** 两个 `select()` 重载均加 `tenantId == null || tenantId.isEmpty()` 检查，直接返回 `collectionName`。非多租户场景无需前缀。

### C-11. ~~QueryParser 正则只认单引号，不认双引号~~ ✅ 已知限制
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/QueryParser.java:9`
**问题:** `db\.getCollection\('(.*?)'\)` 只匹配单引号，合法的 MongoDB shell 双引号写法报错。
**状态:** MQL 语法解析已知限制。当前仅支持单引号风格（与 MongoDB shell 默认输出一致），双引号支持留待后续扩展。

---

## 高（High）

### H-1. ~~findOneByEntity 空实体返回随机文档 — 设计如此~~
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/SimpleMongoRepository.java`
**问题:** 实体所有字段为 null 时 `write(entity)` 生成空 `Document{}`，`find({}).first()` 返回集合自然顺序第一条文档。
**决定:** 读操作允许空条件（与 MyBatis-Plus 行为一致），仅写操作（delete/update）拒绝空条件。

### H-2. ~~count(T entity) 空实体返回总数 — 设计如此~~
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/SimpleMongoRepository.java`
**问题:** 同 H-1，空实体统计返回集合总文档数。
**决定:** 同 H-1。

### H-3. ~~IdGenerator<?> 类型不安全 ✅ 已修复~~

**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/SimpleMongoRepository.java:46`
**问题:** 无界通配符，ID 生成器产出类型与实体 ID 字段类型不匹配只在运行时报错。
**修复:** 采用**注解驱动按实体类生成器**方案替代全局泛型化：
- `@CollectionId` 新增 `generatorClass` 属性，用户在实体上直接声明生成器类
- `fillId()` 优先从注解读取 `generatorClass` 实例化（带缓存），全局 `IdGenerator<?>` bean 降级为 fallback
- 解决了原方案"全局泛型化无法区分不同实体 ID 类型"的根本问题

**涉及文件:**
- `mongo-flex-core/.../annotation/CollectionId.java` — 新增 `generatorClass` 属性
- `mongo-flex-core/.../config/IdGenerator.java` — 新增 `None` 哨兵、`@FunctionalInterface` 标注
- `mongo-flex-core/.../v2/SimpleMongoRepository.java` — 新增 `resolvePerEntityGenerator()`、`instantiateGenerator()`，含 `ConcurrentHashMap` 缓存

### H-4. ~~convertQueryId 未检查的类型转换~~ → 详见 [附录 B: Bug 2](#附录-b-simplemongoRepository-问题)
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/SimpleMongoRepository.java:310`
**问题:** `convertIdIfNecessary((ID) id)` 将 Object 强转为 ID，堆污染风险。
**状态:** 已在本文件附录 B 中详细分析。

### H-5. ~~DynamicMongoClient select() 返回错误客户端 ✅ 已修复~~

**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/DynamicMongoClient.java:49-55`
**问题:** `NullPointerException` 用作参数校验异常不恰当，且错误信息只报 tenant 名却不列出可用 tenant 列表，排查困难。
**修复:**
- `select(String)` 将 `NullPointerException` 改为 `IllegalArgumentException`
- 拆分 null/empty 检查和 key 不存在检查，各自给出明确信息
- key 不存在时在错误信息中列出 `clients.keySet()`，一眼能看出哪个 tenant 没配

### H-6. ~~CountExecutor 返回值类型截断~~ ✅ 已知限制
**文件:** `src/main/java/com/github/eacryo/mongoflex/strategy/CountExecutor.java:21-25`
**问题:** 返回类型为 int 时 `count.intValue()` 可能静默溢出（集合超 21 亿文档时）。
**状态:** 实际场景极少达到 21 亿文档量级，且 `@Mql` 方法声明 `long` 返回类型即可规避。溢出时抛异常的支持留待后续。

### H-7. ~~@Mql 参数值直接 toString 拼入 JSON，无转义~~ ✅ 已知限制
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/MyRepositoryProxyHandler.java:104`
**问题:** `args[i].toString()` 直接拼入 JSON 字符串，值中含引号或特殊字符导致 JSON 格式错误。
**状态:** 与 C-6 同源——MQL 参数占位符替换的已知限制。特殊字符的转义需引入 JSON 序列化库，留待后续统一处理。

### H-8. ~~ClassFieldMetaData 无条件 setAccessible(true) ✅ 已修复~~

**文件:** `src/main/java/com/github/eacryo/mongoflex/convertor/ClassFieldMetaData.java:35`
**问题:** 对所有字段（包括 static final 常量）调用 `setAccessible(true)`，JPMS 下可能抛 `InaccessibleObjectException`。
**修复:** 用 `Modifier.isStatic()` 跳过 static 字段，它们不参与文档映射，也无需打破访问控制。

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

### M-4. ~~FindExecutor / FindOneExecutor 忽略 command 和 args~~ ✅ 已知限制
**文件:** `strategy/FindExecutor.java:24`，`strategy/FindOneExecutor.java:21-28`
**问题:** 不支持 limit、skip、sort、projection，@Mql 查询表达能力受限。
**状态:** MQL 执行器已知限制。当前 MQL 定位为"简单查询捷径"，复杂操作请在 MQL 语句中直接写入（如 `db.getCollection('x').find({}).limit(10)` 在 shell 侧处理），或改用 Lambda 查询 API。

### M-5. ~~@Mql QueryParser 不校验是否已注册 Executor~~ ✅ 已知限制
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/QueryParser.java:16`
**问题:** 正则捕获 9 种操作但只注册了 4 种 Executor，其他操作解析成功但执行时才报 UnsupportedOperationException。
**状态:** 已文档化——@Mql 支持 find/findOne/count/deleteOne 四种操作，其他操作未实现。扩展时需同步注册 Executor，当前 UnsupportedOperationException 的错误信息已明确指示未支持的操作类型。

### M-6. ~~MongoMappingConvertor.write(null) 返回 null~~ ✅ 已解决
**文件:** `src/main/java/com/github/eacryo/mongoflex/convertor/MongoMappingConvertor.java:88-90`
**问题:** 返回 null 而非抛明确异常，所有调用方必须自行判空。
**解决方案:** 抛 `IllegalArgumentException("entity must not be null")`。同时移除 `@Component` 注解，改为在 `MyOrmAutoConfiguration` 中通过 `@Bean` 显式注册，防止用户直接 `@Autowired` 此内部组件。

### M-7. ~~SimpleMongoRepository.findList 全量加载到内存 — 设计如此~~
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/SimpleMongoRepository.java:120`
**问题:** `into(new ArrayList<>())` 一次加载全部结果，大数据集 OOM。
**决定:** `findList` 的语义就是"查全量"，需要分页时使用 `findPage`，需要流式处理时使用 `findAll` 逐条迭代。与 MyBatis-Plus `selectList` 行为一致。

### M-8. ~~LambdaQueryWrapper 不支持 OR 逻辑~~ ✅ 已解决
**文件:** `lambda/LambdaQueryWrapper.java`
**问题:** 所有条件固定 AND 连接，无法表达 `WHERE a=1 OR b=2`。
**修复:** 添加 `or()` 和 `or(LambdaQueryWrapper<T>)` 方法。`or()` 插入 sentinel 分割条件组，渲染时组内 AND、组间 OR。

### M-9. ~~ReflectUtil.getFieldNameFromLambda 对 boolean getter 前缀处理不准确 ✅ 已修复~~

**文件:** `src/main/java/com/github/eacryo/mongoflex/util/ReflectUtil.java:31-37`
**问题:** `isActive()` 始终解析为 `active`，但实际字段名可能是 `isActive`（`is` 不是 getter 前缀时）。

**分析:** 这是 JavaBeans 规范的固有歧义——`isActive()` 可能是 `boolean active` 或 `boolean isActive` 的 getter，仅靠方法名无法区分。MyBatis 的 `PropertyNamer` 同样不解决此问题。

**修复:** 重构为与 MyBatis `PropertyNamer.methodToProperty()` 一致的规则：
- 提取 `methodToProperty()` 方法，遵循 MyBatis 的 getter→property 命名规则
- 新增 JavaBeans 大写规则：第二个字母大写时保留首字母（如 `getURL` → `URL`）
- 非 getter 风格的方法名抛 `IllegalArgumentException`（明确 fail-fast，而非静默使用原方法名）
- 在类 Javadoc 中明确记录此已知限制，并说明用 `@CollectionField` 覆盖的方式

### M-10. ~~泛型解析只检查直接接口，不支持层级接口继承 ✅ 已修复~~

**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/RepositoryRegistrar.java:43-55`
**问题:** `interface UserRepo extends BaseRepo<User, String>` 且 `BaseRepo extends MongoRepository` 时泛型解析失败。
**修复:** 新增 3 个方法实现递归泛型解析：
- `resolveMongoRepositoryTypes(Type, Map)` — 沿接口和父类层级递归查找 `MongoRepository`，逐层累积 TypeVariable → 实际类型映射
- `getRawClass(Type)` — 从 Class 或 ParameterizedType 提取原始 Class
- `resolveTypeVariable(Type, Map)` — 用映射表将 TypeVariable 替换为实际类型
- `registerBeanDefinitions()` 调用 `resolveMongoRepositoryTypes()` 替代原先的扁平 for 循环

### M-11. ~~DateValueGenerator 只支持 Date 和 String ✅ 已修复~~

**文件:** `src/main/java/com/github/eacryo/mongoflex/util/DateValueGenerator.java:20-28`
**问题:** `LocalDateTime`、`LocalDate`、`Instant` 等常见类型不支持，抛 `IllegalArgumentException`。
**修复:** 内置类型表 + 用户扩展接口：
- 新增 `DateValueProvider` 接口（`@FunctionalInterface`），用户实现后注册 Spring bean 即可扩展自定义时间类型或覆盖内置行为
- 内置支持 6 种类型：`Date`、`String`、`LocalDateTime`、`LocalDate`、`Instant`、`Long`/`long`
- `generateCurrentDate(type, pattern, provider)` 遵循优先级：用户 provider → 内置类型表 → 抛异常
- `SimpleMongoRepository` 通过 `@Autowired(required = false)` 注入后传入 `DateValueGenerator`
- 向后兼容：原有的 `generateCurrentDate(type)` / `generateCurrentDate(type, pattern)` 签名保持不变

**涉及文件:**
- `mongo-flex-core/.../config/DateValueProvider.java` — 新增
- `mongo-flex-core/.../util/DateValueGenerator.java` — 重构
- `mongo-flex-core/.../v2/SimpleMongoRepository.java` — 注入 provider
- `mongo-flex-core/.../v2/RepositoryFactoryBean.java` — 装配

### M-12. ~~Insert 回填ID 时 ID 字段可能会没有 @CollectionId 注解~~ → 详见 [附录 B: Bug 1](#附录-b-simplemongoRepository-问题)

### M-13. ~~MongoDB Driver 5.x 兼容性 — 已确认兼容 ✅~~

**验证方式:** SB3 测试模块（Spring Boot 3.5.4）通过 BOM 间接依赖 `mongodb-driver-sync:5.5.1`，全部测试通过。

**结论:** mongo-flex 天然兼容 MongoDB Driver 4.x 和 5.x，无需 bridge、无需拆版本。

**原因:** mongo-flex 引用的 17 个 MongoDB Driver 类全部落在 4.x → 5.x 的稳定子集中：

| mongo-flex 使用的 API | 5.0 是否受影响 |
|---|---|
| `Filters`（eq/ne/gt/lt/regex/in/exists/or/and/nor 等 21 个操作符） | ✅ 无变更 |
| `Document` / `Bson` / `BsonValue` / `ObjectId` | ✅ 无变更 |
| `MongoCollection.find/countDocuments/updateOne/deleteOne/...` | ✅ 无变更 |
| `InsertOneResult.getInsertedId()` / `InsertManyResult.getInsertedIds()` | ✅ 无变更 |
| `ConnectionString` / `ConnectionString.getDatabase()` | ✅ 无变更（签名一致） |
| `MongoClientSettings` / `MongoClients.create()` | ✅ 无变更 |
| `Projections` / `UpdateOptions` / `DeleteResult` / `UpdateResult` | ✅ 无变更 |
| `FindIterable` | ✅ 无变更 |

**5.0 真正的破坏性变更都在 mongo-flex 不使用的区域：**

- Stream/Netty 传输层重构（`StreamFactoryFactory` → `TransportSettings`）
- `ConnectionId` 参数类型 int→long
- `SocketSettings` 超时参数 int→long
- `MapCodec`/`IterableCodec` 移除
- `geoHaystack` 索引 API 移除
- 事件监听器（`ClusterListener` 等）构造函数移除
- `ServerAddress.getSocketAddress()` 等底层方法移除
- `Parameterizable` 接口移除
- Record 注解重定位（`org.bson.codecs.record.annotations` → `org.bson.codecs.pojo.annotations`）
- 最低 MongoDB Server 版本提升至 4.0+（5.2）

这证明了 Java 8 bytecode + 只用核心稳定 API 的策略是有效的：mongo-flex 没有碰 Driver 的任何 volatile 层（传输、事件、Codec 扩展），只用的 CRUD API 是 MongoDB 最精心维护的公共面。

**测试覆盖状态:**

| 测试模块 | Driver 版本 | 覆盖路径 |
|---|---|---|
| `mongo-flex-test-spring-boot2` | 4.11.1（手动 pin） | 4.x |
| `mongo-flex-test-spring-boot3` | 5.5.1（Spring Boot BOM 带） | 5.x |

两条路径均已验证通过，`SB3` 是主要测试目标。

---

## UlidTest 问题

### U-1. encode 测试未覆盖 byte 负值边界（0x80 ~ 0xFF）
**文件:** `mongo-flex-core/src/test/java/com/github/eacryo/mongoflex/ulid/UlidTest.java:253-300`
**问题:** `encodeShouldProduceValidOutput` 和 `encodeShouldBeDeterministic` 两个测试的 random 数组只用 `0..9` 和 `i * 17 + 42` 等正小值 pattern，未覆盖 Java `byte` signed 类型的负值边界。`Ulid.encode()` 第 143 行 `random[byteIdx++] & 0xFF` 虽已正确处理 signed byte，但无测试覆盖 `0x80` ~ `0xFF` 区间。
**建议:** 补充 random 数组含 `-1`（`0xFF`）、`-128`（`0x80`）等边界值的 encode 测试用例。

### U-2. 时钟回退测试依赖反射，Java 17+ 模块化环境需 --add-opens
**文件:** `mongo-flex-core/src/test/java/com/github/eacryo/mongoflex/ulid/UlidTest.java:223-247`
**问题:** `shouldHandleClockRollback` 通过 `setAccessible(true)` 反射修改 `Ulid.lastTimestamp`，在 Java 17+ 模块化环境下（未开放反射）会抛 `InaccessibleObjectException`。当前需加 `--add-opens java.base/java.lang=ALL-UNNAMED` 或确保 `Ulid` 所在包已开放。
**建议:** 考虑提取 package-private 的测试辅助方法（如 `Ulid.setLastTimestamp(long)`）替代反射。

### U-3. concurrentGenerationShouldBeMonotonicPerThread 中 prev 变量最终值未使用
**文件:** `mongo-flex-core/src/test/java/com/github/eacryo/mongoflex/ulid/UlidTest.java:192-201`
**问题:** 每个线程内 `prev` 在循环末尾赋值为 `next`，循环结束后 `prev` 持有最后一个值但不再被使用。不影响正确性，但变量作用域可缩小（仅在循环内使用）。

### U-4. @RepeatedTest(5) × 50,000 次 = 250,000 次生成，慢环境可能耗时
**文件:** `mongo-flex-core/src/test/java/com/github/eacryo/mongoflex/ulid/UlidTest.java:306-325`
**问题:** `repeatedBulkShouldBeUniqueAndMonotonic` 标记 `@RepeatedTest(5)`，每次重复生成 50,000 ULID 并全部插入 `HashSet` + `compareTo` 检查。5 次共 250,000 次，慢环境可能数秒。加上 `@AfterEach` 调用 `resetState()` 每次重复后重置，每轮从新鲜随机开始，各轮之间无同比性（非 bug，但值得注意）。

---

## 低（Low）

### L-1. ~~FillConstant 字段非 final ✅ 已修复~~

**文件:** `src/main/java/com/github/eacryo/mongoflex/constant/FillConstant.java:4-7`
**修复:** 字段加 `final` 修饰，加私有构造器禁止实例化，加中英双语文档注释。

### L-2. ~~DynamicMongoClient 多余分号~~ ✅ 已解决
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/DynamicMongoClient.java`

### L-3. ~~SimpleMongoRepository.buildFilterFromMap 死代码~~ ✅ 已解决
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/SimpleMongoRepository.java`
**解决方案:** 已删除。

### L-4. ~~MyRepositoryProxyHandler.mapDocumentToEntity 死代码~~ ✅ 已知限制
**文件:** `src/main/java/com/github/eacryo/mongoflex/v2/MyRepositoryProxyHandler.java:84-93`
**状态:** MQL 代理内部保留方法，虽当前未被调用但作为 MQL 结果映射的兜底实现，暂不删除。

### L-5. ~~冗余的 mongodb-driver-core 和 bson 依赖 ✅ 已解决~~

**文件:** `mongo-flex-core/pom.xml`

**问题:** `mongodb-driver-core` 和 `bson` 已由 `mongodb-driver-sync` 传递依赖，显式声明冗余。

**修复:** 删除两个冗余依赖，保留 `mongodb-driver-sync`（`optional` 作用域）。用户自行控制 Driver 版本，编译时 link 4.x，运行时 5.x 经 SB3 测试验证通过。

### L-6. ~~拼写不一致 "Convertor" vs "Converter"~~ ✅ 已知限制
**文件:** `convertor/` 整个包
**状态:** breaking change，重命名会影响所有外部调用方。待大版本升级时统一处理。

### L-7. ~~MongoFlexConstant 允许实例化~~ ✅ 已知限制
**文件:** `src/main/java/com/github/eacryo/mongoflex/constant/MongoFlexConstant.java`
**状态:** 常量类为配置属性占位符类，Spring 通过反射实例化，加 private 构造器反而可能干扰框架行为。保持现状。

### L-8. ~~FillConstant 允许实例化 ✅ 已修复~~

**文件:** `src/main/java/com/github/eacryo/mongoflex/constant/FillConstant.java`
**修复:** 加 `private` 构造器（与 L-1 同批次修复）。

### L-9. ~~PageDTO 用装箱 Long，可为 null~~ ✅ 已解决
**文件:** `src/main/java/com/github/eacryo/mongoflex/entity/PageDTO.java:12-15`
**修复:** 改用 `long` 基础类型 + 默认值，`totalPage` 改为计算 getter（与 MyBatis-Plus `IPage.getPages()` 一致）。

### L-10. ~~PageDTO.orderBy 不支持逐字段升降序~~ ✅ 已解决
**文件:** `src/main/java/com/github/eacryo/mongoflex/entity/PageDTO.java:16`
**修复:** 新增 `PageDTO.SortOrder` 内部类（`field` + `ascending`），替换 `List<String> orderBy + boolean orderByAsc` 组合。每个字段可独立指定升降序，与 MyBatis-Plus `OrderItem` 设计一致。

### L-11. ~~DeleteOneExecutor 导入了未使用的内部 API~~ ✅ 已知限制
**文件:** `src/main/java/com/github/eacryo/mongoflex/strategy/DeleteOneExecutor.java:6`
**状态:** MQL 执行器遗留 import，无害（编译期被优化掉）。MQL 体系整体重构时统一清理。

### L-12. ~~CollectionNameUtil 有未使用的 Logger 字段~~ ✅ 已解决
**文件:** `src/main/java/com/github/eacryo/mongoflex/util/CollectionNameUtil.java:17`
**修复:** 删除未使用的 `LOGGER` 字段及 `Logger`/`LoggerFactory` import，顺带将类头注释改为中英双语 Javadoc。

### L-13. ~~DynamicMongoClient 未使用的 Logger/LoggerFactory import~~ ✅ 已解决

### L-14. ~~MongoBsonRenderer ELEM_MATCH 使用了原始类型强转~~ ✅ 已解决
**文件:** `src/main/java/com/github/eacryo/mongoflex/lambda/MongoBsonRenderer.java:81`
**修复:** `renderGroup()` 方法加 `@SuppressWarnings({"unchecked", "rawtypes"})` + 注释说明语义安全性。ELEM_MATCH 使用字段声明的泛型元素类型，NOT 复用外层 wrapper 的 entityClass，两处强转均语义安全，仅因 Java 通配符 capture 限制无法用泛型正确表达。

### L-15. ~~各 Executor 注入 ExecutorProxy 仅用于注册~~ ✅ 已知限制
**文件:** FindExecutor、FindOneExecutor、CountExecutor、DeleteOneExecutor
**状态:** 当前 push 模式（Executor 自我注册）功能正确，M-1 已通过去掉 `static` 解决了跨 Context 污染问题。pull 模式重构（`List<CommandExecutor>` 自动发现）是纯风格改进，建议在 MQL 体系整体重构时一并处理。

### L-16. ~~AutoConfiguration.imports 列入了非 AutoConfiguration 类~~ ✅ 已解决
**文件:** `src/main/resources/META-INF/spring/...AutoConfiguration.imports`
**修复:** 已移除 `MongoFlexProperties`（`@ConfigurationProperties`）和 `CollectionNameUtil`（工具类），仅保留 `MongoFlexAutoConfiguration`（`@Configuration`）。Spring Boot 启动时会将此文件中所有类当作自动配置类处理，非配置类会导致不必要实例化或启动报错。

### L-17. ~~Condition record 不做 null 校验~~ ✅ 已解决
**文件:** `src/main/java/com/github/eacryo/mongoflex/lambda/Condition.java:4-8`
**修复:** public 构造器中 `field` 和 `operator` 加 `Objects.requireNonNull`，fail-fast 提早暴露问题。无参构造器（package-private，OR 分割哨兵用）保持不变。

### L-18. ~~pom.xml parent POM 被注释~~ ✅ 已知限制
**文件:** `pom.xml:5-10`
**状态:** 当前通过 `dependencyManagement` 显式管理 Spring 依赖版本，不依赖 parent POM 的版本继承。恢复 `spring-boot-starter-parent` 会引入不必要的依赖约定，保持现状。

### L-19. ~~pom.xml 发布元数据为空~~ ✅ 已知限制
**文件:** `pom.xml:41-56`
**状态:** 当前未发布到 Maven Central，发布元数据（url、licenses、developers、scm）在正式发布前补全即可。

### L-20. ~~MongoFlexProperties.getDatabaseFromUri 每次 new ConnectionString~~ ✅ 已解决
**文件:** `src/main/java/com/github/eacryo/mongoflex/config/MongoFlexProperties.java:24-39`
**修复:** 单租户模式首次调用后缓存数据库名（`volatile` + DCL），多租户模式按租户 URI 缓存在 `ConcurrentHashMap` 中（`computeIfAbsent`）。`new ConnectionString(uri)` 不是简单字符串分割——它会做 DNS SRV 解析（阻塞 I/O）、多轮 option 遍历、URL 解码和大量对象分配，每次数据库操作都重复这些工作是显著浪费。顺手改进了"Tenant not found"错误信息，列出可用租户列表。

---

## 统计

| 严重度 | 已解决/设计如此 | 已知限制 | 合计 |
|--------|:---:|:---:|:---:|
| Critical | 9 | 2 | 11 |
| High | 6 | 3 | 9 |
| Medium | 10 | 3 | 13 |
| Low | 15 | 5 | 20 |
| **合计** | **40** | **13** | **53** |

> ✅ 所有 Critical 问题已解决或确认为已知限制。MQL 体系限制（C-5/C-6/C-11/H-6/H-7/M-4/M-5）集中在 `strategy/` 执行器 + `QueryParser` + `MyRepositoryProxyHandler`，是相对封闭的模块，不影响 Lambda 查询和 Repository CRUD 主路径。

---

# 附录 A：Entity Inheritance Issues

## 背景

mongo-flex 当前没有类型鉴别机制（如 Spring Data MongoDB 的 `_class` 字段）。`MongoMappingConvertor.read()` 永远实例化传入的 `targetClass`，不会根据文档内容选择子类。这导致以下四个问题。

以 `Character`（父）和 `LiyueCharacter extends Character`（子）为例，假设两者存储在同一个 `character` 集合中。

---

## 问题 A-1：~~反序列化类型丢失，子类字段被丢弃（✅ 设计如此，不需要解决）~~

**设计原则（与 MyBatis-Plus 一致）：Repository 的类型参数决定了读写的数据边界。**

- `CharacterRepository` → 只负责 `Character` 的字段，返回 `Character` 实例
- `LiyueCharacterRepository` → 负责 `Character` + `LiyueCharacter` 的字段，返回 `LiyueCharacter` 实例

如果需要完整读写子类字段，应当定义专用的 `LiyueCharacterRepository extends MongoRepository<LiyueCharacter, String>`，而不是期望通过父类 Repository 拿到子类实例。这一设计选择避免了隐式类型转换的"魔法"，调用方明确知道拿到的就是声明的类型，不需要 `instanceof` 防御。

> Spring Data MongoDB 默认通过 `_class` 字段自动实例化子类，是一种不同的设计选择（隐式多态），但会增加使用复杂度和潜在的类型安全问题。mongo-flex 选择 MyBatis-Plus 式的显式类型绑定。

**根因：** `MongoMappingConvertor.read(doc, targetClass)` 中 `targetClass` 来自仓库接口声明的编译期类型（`CharacterRepository<Character>` → `Character.class`），永远实例化 `Character`。MongoDB 文档中子类特有字段（如 `title`、`is_adeptus`）在反序列化时被静默忽略。

```java
// INSERT — ✅ 写入正确
LiyueCharacter hutao = new LiyueCharacter();
hutao.setVision("Pyro");
hutao.setTitle("雪霁梅香");          // 子类字段
hutao.setAffiliation("往生堂");       // 子类字段
hutao.setMoraAmount(999_999_999L);
repo.insert(hutao);                 // 运行时类型正确，全字段序列化

// MongoDB 文档：
// { "_id": "...", "vision": "Pyro", "title": "雪霁梅香", "affiliation": "往生堂", ... }

// READ — ❌ 子类字段丢失
Character c = repo.findById(hutao.getId());
c.getVision();       // "Pyro" ✅
c.getClass();        // Character ❌ 不是 LiyueCharacter
// title、affiliation、moraAmount 存在于 MongoDB 文档中，但反序列化时被忽略
```

**影响：**
- INSERT 和 READ 的类型不对称：存入的是 `LiyueCharacter`，读出来的是 `Character`
- 子类独有数据永久丢失（除非改用专用子类 Repository 读取）

---

## 问题 A-2：无法通过 `instanceof` 判断实际类型

**根因：** `MongoMappingConvertor.read()` 始终通过 `targetClass.getDeclaredConstructor().newInstance()` 创建实例，无论如何都是父类对象。

```java
// MongoDB 里存的是 LiyueCharacter
LiyueCharacter hutao = new LiyueCharacter();
hutao.setTitle("雪霁梅香");
repo.insert(hutao);

// 读回来
Character c = repo.findById(hutao.getId());

if (c instanceof LiyueCharacter) {   // ❌ 永远 false！
    LiyueCharacter lc = (LiyueCharacter) c;
    lc.getTitle();                    // 这段代码永远不会执行
}
```

**影响：**
- 即使将来增加了类型标识字段（如 `_class`），也无法通过 `instanceof` 做类型判断
- 同一集合无法实现多态查询

---

## 问题 A-3：~~Lambda 查询子类字段时，`@CollectionField` 映射失效（✅ 已解决）~~

### 设计分析

Repository 的类型参数和 Lambda 引用的声明类是**两件独立的事情**：

| 决定什么 | 由谁控制 | 含义 |
|---|---|---|
| 返回什么类型的对象 | `Repository<T>` 的 `T` | 读出来的对象是 `Character` 还是 `LiyueCharacter` |
| 用什么字段做查询条件 | Lambda 引用的声明类 | 字段的 `@CollectionField` 映射用哪个类的元数据解析 |

两个维度可以自由组合，三种场景都合理：

```java
// ===== 场景 A：父类 Repository + 子类字段过滤 =====
// "我只想要 Character 的结果，但我想筛出仙人的角色"
CharacterRepository repo;
LambdaQueryWrapper<Character> w = new LambdaQueryWrapper<>(Character.class);
w.eq(LiyueCharacter::getIsAdeptus, true);   // 过滤条件：is_adeptus = true
List<Character> result = repo.findList(w);   // 返回：Character 列表（不含子类字段）

// ===== 场景 B：子类 Repository + 父类字段过滤 =====
// "我要完整的 LiyueCharacter，但用通用的 vision 字段过滤"
LiyueCharacterRepository repo;
LambdaQueryWrapper<LiyueCharacter> w = new LambdaQueryWrapper<>(LiyueCharacter.class);
w.eq(Character::getVision, "Pyro");         // 过滤条件：vision = "Pyro"
List<LiyueCharacter> result = repo.findList(w);  // 返回：完整的 LiyueCharacter 列表

// ===== 场景 C：子类 Repository + 子类字段过滤 =====
// "我要完整的 LiyueCharacter，用子类特有字段过滤"
LiyueCharacterRepository repo;
LambdaQueryWrapper<LiyueCharacter> w = new LambdaQueryWrapper<>(LiyueCharacter.class);
w.eq(LiyueCharacter::getIsAdeptus, true);   // 过滤条件：is_adeptus = true
List<LiyueCharacter> result = repo.findList(w);  // 返回：完整的 LiyueCharacter 列表
```

场景 B 和 C 本来就能正确工作（`entityClass = LiyueCharacter.class`，`ClassFieldMetaData` 遍历完整层级）。**真正有问题的是场景 A**——Lambda 引用的是子类字段，但 `entityClass` 是父类。

### 根因

`MongoBsonRenderer` 调用 `convertor.resolveMongoFieldName(entityClass, javaFieldName)`，用 `entityClass`（来自 `LambdaQueryWrapper.getEntityClass()`）去解析字段映射。对于场景 A，`entityClass = Character.class`，`ClassFieldMetaData(Character.class)` 不包含 `LiyueCharacter` 的 `isAdeptus` 字段及其 `@CollectionField("is_adeptus")` 映射，导致：

```java
// resolveMongoFieldName(Character.class, "isAdeptus")
//   → Character 的 ClassFieldMetaData 里没有 "isAdeptus"
//   → 直接返回 "isAdeptus" 作为 MongoDB 字段名（fallback 为 Java 字段名）
//   → 生成的查询：{ "isAdeptus": true }
//   → 实际数据库字段名：{ "is_adeptus": true }  ← @CollectionField 映射被忽略
//   → 查询静默返回空
```

### 解决方案

Lambda 表达式本身携带了字段声明类的信息——`SerializedLambda.getImplClass()`。`LiyueCharacter::getIsAdeptus` 返回 `LiyueCharacter.class`。只需让 `MongoBsonRenderer` 用每个字段自己的 `implClass` 去解析映射，而不是统一用 `entityClass`：

1. `ReflectUtil` 新增 `getImplClassFromLambda(SFunction)` —— 从 lambda 提取声明类
2. `Condition` 增加 `implClass` 字段 —— 记录每个条件的字段声明类
3. `LambdaQueryWrapper` —— 每个条件构造时传入 `implClass`
4. `MongoBsonRenderer.renderGroup()` —— 字段名解析用 `c.implClass() ?? entityClass`

---

## 问题 A-4：~~`@Mql` 查询同样无法反序列化为子类（✅ 设计如此，不需要解决）~~

**根因：** `FindExecutor` / `FindOneExecutor` 通过 `method.getReturnType()` 或 `method.getGenericReturnType()` 获取目标类型，即方法签名中声明的返回类型。

```java
// CharacterRepository 中声明：
@Mql("db.getCollection('character').find({})")
List<Character> findAll();

// FindExecutor 反序列化时：
// listElementClass = Character.class（从方法签名的泛型参数获取）
// mongoMappingConvertor.read(doc, Character.class)
// → 永远返回 Character 实例，子类字段丢失
```

即使 MongoDB 文档中包含完整的 `LiyueCharacter` 数据，`@Mql` 查询返回的也是按照方法声明类型反序列化的结果。

**影响：**
- `@Mql` 和 `LambdaQueryWrapper` 两条查询路径存在相同的问题
- 如果方法返回 `List<Object>` 或 `List`，虽然会走到 fallback 逻辑（`FindExecutor` 中 `Object.class`），但同样不会用到子类

---

## 附录 A 总结

| 问题 | 关键类/方法 | 根本原因 |
|---|---|---|
| A-1. 子类字段丢失 ✅ | `MongoMappingConvertor.read()` | 永远实例化编译期类型，不读运行时类型（设计如此） |
| A-2. instanceof 失效 | `MongoMappingConvertor.read()` | `newInstance()` 创建的是父类 |
| A-3. 子类字段映射断裂 ✅ | `MongoBsonRenderer.renderGroup()` → `resolveMongoFieldName()` | 已修复：优先用 lambda 的 `implClass` 解析 |
| A-4. @Mql 同样问题 ✅ | `FindExecutor` / `FindOneExecutor` | 设计如此：返回类型决定数据边界 |

**核心矛盾：** INSERT 用运行时类型（能正确处理子类），READ 用编译期类型（丢失子类信息）。要实现多态支持，需要引入类型鉴别机制，让 READ 也能感知原始类型。

---

# 附录 B：SimpleMongoRepository 问题

## Bug B-1（已修复 ✅）：`@CollectionId` 注解在非 `id` 命名字段上不映射到 `_id`

**文件:** `mongo-flex-core/src/main/java/com/github/eacryo/mongoflex/convertor/ClassFieldMetaData.java`

**问题:** `ClassFieldMetaData` 只在 Java 字段名恰好是 `"id"` 时才将字段映射到 MongoDB 的 `_id`。如果用户将 `@CollectionId` 注解放在非 `id` 命名的字段上（如 `private String userId`），该字段的 MongoDB 字段名仍然是 `"userId"` 而非 `"_id"`。

```java
// 原逻辑：仅凭字段名字面量判断
if (JAVA_ID_FIELD.equals(mongoFieldName)) {
    mongoFieldName = MONGO_ID_FIELD;
}
```

**根因:** `@CollectionId` 注解仅在 `ClassFieldMetaData` 中用于标识哪个字段是 ID 字段（`foundCollectionIdField`），但未参与字段名映射逻辑。`_id` 映射仅依赖字段名是否为 `"id"`。

**修复方案:** 参照 Spring Data MongoDB 的 `@Id` 行为——以注解为准，而非字段名。调整后的映射优先级：

```
1. @CollectionId 注解  →  强制映射到 _id（最高优先级）
2. @CollectionField    →  使用注解中指定的字段名
3. Java 字段名 = "id"  →  隐式映射到 _id
4. 其他               →  使用 Java 字段名
```

**测试验证:** 新增 `CustomIdEntity`（字段名 `userId` 标注 `@CollectionId`），覆盖 insert → findById → updateById → findOneByEntity → deleteById 完整链路，所有 5 个测试通过。

---

## Bug B-2（已修复 ✅）：`ObjectId.isValid()` 启发式转换可能误判 String 类型的 `_id`

**文件:** `mongo-flex-core/src/main/java/com/github/eacryo/mongoflex/v2/SimpleMongoRepository.java`

**问题:** 原逻辑使用 `ObjectId.isValid()` 猜测一个 String 是否应转为 ObjectId，纯粹基于字符串格式。虽然 ULID（26 字符）和 UUID（36 字符）天然不会误判，但如果用户使用 `IdType.INPUT` + 自定义 `IdGenerator` 生成 24 位 hex 字符串作为 ID，会被错误转换为 ObjectId，导致 MongoDB 中 String 和 ObjectId 类型不匹配，查询静默返回空。

```java
// 原逻辑：凭字符串格式猜测
if (id instanceof String && ObjectId.isValid((String) id)) {
    return new ObjectId((String) id);
}
```

**修复方案:** 利用框架已有的 `IdType` 元数据做精确判断——仅在 `IdType.OBJECT_ID` 下才转换，因为此时 MongoDB 生成的是 ObjectId，Java 侧以 hex String 存储，查询时必须转回。

| IdType | 存储的实际类型 | 是否需要 String→ObjectId |
|--------|-------------|------------------------|
| `OBJECT_ID` | ObjectId（MongoDB 生成） | ✅ 是 |
| `ULID` | String | ❌ 否 |
| `UUID` | String | ❌ 否 |
| `INPUT` | 取决于生成器 | ❌ 否 |

**测试验证:** `ObjectIdConversionTest` — 4 个测试覆盖 `IdType.OBJECT_ID` 转换和 `IdType.ULID` 不转换两个方向。

---

## ~~问题 B-3（低，代码质量，❌ 不需要解决）：`doc.remove("_id")` + `$set` 模式重复 4 次~~

**文件:** `mongo-flex-core/src/main/java/com/github/eacryo/mongoflex/v2/SimpleMongoRepository.java`

以下四处包含完全相同的逻辑：

| 行号 | 方法 |
|------|------|
| 177 | `updateById` |
| 195 | `update(SFunction, value, entity)` |
| 211 | `update(LambdaQueryWrapper, entity)` |
| 223 | `updateAll` |

```java
// 重复 4 次的模式
doc.remove("_id");
Document updateDoc = new Document("$set", doc);
```

**修复:** 提取为私有方法，如 `private UpdateResult executeUpdate(Bson filter, Document updateBody)`。

---

## 问题 B-4（已修复 ✅）：`fillDate` 异常捕获过于宽泛

**文件:** `mongo-flex-core/src/main/java/com/github/eacryo/mongoflex/v2/SimpleMongoRepository.java:348`

```java
} catch (Exception e){
    throw new RuntimeException("Failed to set date fields", e);
}
```

`catch (Exception e)` 覆盖范围过宽，连 NPE、`SecurityException` 等也会被吞掉并重新包装。虽然原始异常作为 cause 被保留不影响排查，但失去了异常类型的区分度。

**修复:** 只 catch `IllegalAccessException`；`DateValueGenerator.generateCurrentDate()` 的 `IllegalArgumentException`（不支持的日期类型）应让其自然传播，调用方有更明确的异常类型可以处理。
