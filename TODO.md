# TODO

待实现功能清单。

---

## 已完成 ✅

### PageDTO 分页 ✅

分页查询逻辑已实现。`MongoRepository` 新增 `findPage`（LambdaQueryWrapper 条件分页）和 `findPageByEntity`（实体条件分页），底层基于 MongoDB 的 `skip()` / `limit()` + `countDocuments()`。

**涉及文件:**
- `mongo-flex-core/.../entity/PageDTO.java`
- `mongo-flex-core/.../v2/MongoRepository.java`
- `mongo-flex-core/.../v2/SimpleMongoRepository.java`

### 排序（sort / orderBy）✅

LambdaQueryWrapper 已支持 `orderByAsc/Desc(SFunction)` 类型安全排序（含 @CollectionField 映射），Repository 的 `findPage` 已集成。原有 `@Mql FindExecutor` 不支持的问题已随 `strategy/` 包废弃而消除。

**涉及文件:**
- `mongo-flex-core/.../lambda/LambdaQueryWrapper.java` ✅
- `mongo-flex-core/.../v2/SimpleMongoRepository.java` ✅

### 投影 / 字段筛选（projection）✅

LambdaQueryWrapper 已支持 `include(SFunction...)` / `exclude(SFunction...)` 指定查询返回字段（MongoDB projection），`SimpleMongoRepository.applyProjection()` 已集成 include/exclude 及混合模式（include + exclude _id）。

**涉及文件:**
- `mongo-flex-core/.../lambda/LambdaQueryWrapper.java` ✅
- `mongo-flex-core/.../v2/SimpleMongoRepository.java` ✅

### LambdaQueryWrapper 查询修饰字段 ✅

- `orderByAsc/Desc` ✅ 已完成
- `projection` (include/exclude) ✅ 已完成
- `limit` / `skip` — 通过 `PageDTO` 参数传入 `findPage`，无需在 wrapper 上重复

**涉及文件:**
- `mongo-flex-core/.../lambda/LambdaQueryWrapper.java`
- `mongo-flex-core/.../lambda/MongoBsonRenderer.java`

### upsert 支持 ✅

`updateOneById`、`updateOne(SFunction)`、`updateOne(Wrapper)`、`updateMany` 等方法均已支持 `boolean upsert` 参数。插入新文档时同时填充 @CreateDate 和 @UpdateDate；更新已有文档时填充 @UpdateDate。

**涉及文件:**
- `mongo-flex-core/.../v2/MongoRepository.java` ✅
- `mongo-flex-core/.../v2/SimpleMongoRepository.java` ✅

### 批量插入（insertMany）✅

已新增 `insertMany(List<T> entities)` 方法，内部调用 MongoDB 的 `insertMany` 批量插入。支持自动填充 ID（ULID/UUID/自定义）和日期字段，并在 IdType.OBJECT_ID 模式下回填 MongoDB 生成的 ObjectId。

**涉及文件:**
- `mongo-flex-core/.../v2/MongoRepository.java` ✅
- `mongo-flex-core/.../v2/SimpleMongoRepository.java` ✅

---

## 已废弃（不再适用）

以下条目因 `@Mql` 已废弃、`QueryParser` 已删除、`strategy/` 包已移除而不再适用：

- ~~@Mql 未实现的 Executor（insertOne/updateOne/updateMany/deleteMany/aggregate）~~ — `@Mql` 废弃，由 `@Find`/`@Count`/`@Delete` + `SimpleMongoRepository` 统一执行
- ~~@Mql 不接受 LambdaQueryWrapper 参数~~ — `@Mql` 废弃，不再适用

---

## 待实现

### LambdaQueryWrapper 缺少 MongoDB 操作符 🔄 部分完成

已补齐 `not`/`nor`、`mod`、`type`。仍缺少：

| 操作符 | 说明 | 状态 |
|------|------|------|
| `not` / `nor` | 逻辑取反 / 全不匹配 | ✅ 已完成 |
| `mod` / `type` | 取模 / 类型匹配 | ✅ 已完成 |
| `near` / `nearSphere` | 地理位置查询 | ❌ 待实现 |
| `text` | 全文搜索 | ❌ 待实现 |
| `expr` | 表达式 | ❌ 待实现 |

**涉及文件:**
- `mongo-flex-core/.../lambda/LambdaQueryWrapper.java`
- `mongo-flex-core/.../lambda/Operator.java`
- `mongo-flex-core/.../lambda/MongoBsonRenderer.java`

### 更新 README.md / README-zh.md 🔄 部分完成

最近提交已同步 rename delete → deleteOne/deleteMany 及 update/upsert 重构的文档。待后续架构重构完成后全面更新 Quick Start 示例。

**涉及文件:**
- `README.md`
- `README-zh.md`

---

## 代码审查发现的问题（2026-07-13 全量扫描）

以下问题按严重程度从高到低排列。

---

### 🔴 Critical Bugs

#### BUG-1: LIKE/NOT_LIKE 通配符转换未转义正则特殊字符

`MongoBsonRenderer.java` 第 147-157 行将 `*` 和 `%` 替换为 `.*`，但未先转义输入中的正则特殊字符（`.` `(` `)` `+` `?` `[` `]` `{` `}` `^` `$` `|` `\`）。输入 `"example.com"` 会匹配 `exampleXcom` 等非预期结果。应该先用 `Pattern.quote()` 转义再替换通配符。

**涉及文件:** `mongo-flex-core/.../lambda/MongoBsonRenderer.java`

#### BUG-2: IS_NULL 使用 `$exists: false`，语义不同于 SQL NULL

`MongoBsonRenderer.java` 第 172-173 行 `Filters.exists(field, false)` 只匹配字段不存在的文档，不匹配字段存在但值为 null 的文档。标准 SQL `IS NULL` 等价 MongoDB 查询应是 `{ field: null }` 或 `{ field: { $type: 10 } }`。

**涉及文件:** `mongo-flex-core/.../lambda/MongoBsonRenderer.java`

#### BUG-3: `convertBsonValueToJavaType` 中 List 类型检查写反

`MongoMappingConvertor.java` 第 224 行：
```java
if (bsonValue instanceof List && targetClass.isAssignableFrom(List.class))
```
应为 `List.class.isAssignableFrom(targetClass)`。当字段声明为 `ArrayList<String>`（而非 `List<String>`）时条件为 false，嵌套元素不会被递归转换。

**涉及文件:** `mongo-flex-core/.../convertor/MongoMappingConvertor.java`

#### BUG-4: Map 类型字段读取时尝试实例化接口导致崩溃

`MongoMappingConvertor.java` 第 218 行 `read((Document) bsonValue, targetClass)` 当 `targetClass` 为 `Map.class` 时，`getDeclaredConstructor().newInstance()` 抛出 `NoSuchMethodException`，异常被泛型 catch 吞掉后包装为 RuntimeException。

**涉及文件:** `mongo-flex-core/.../convertor/MongoMappingConvertor.java`

#### BUG-5: DynamicMongoClient 中 MongoClient 实例永不关闭（资源泄漏）

`DynamicMongoClient.java` 第 87、111 行通过 `MongoClients.create()` 创建的连接从未关闭。类未实现 `DisposableBean` 或 `@PreDestroy`，Context 刷新/关闭时泄漏 TCP 连接和线程池资源。

**涉及文件:** `mongo-flex-core/.../v2/DynamicMongoClient.java`

#### BUG-6: RepositoryRegistrar 中 null 类型静默传入 RepositoryFactoryBean 导致 NPE

`RepositoryRegistrar.java` 第 57-63 行：当 `resolveMongoRepositoryTypes()` 返回 null 时，`entityClass` 和 `idClass` 保持 null，传入 `RepositoryFactoryBean` 后在 `getCollectionName()` 中 `entityClass.isAnnotationPresent(...)` 抛出 NPE。

**涉及文件:** `mongo-flex-core/.../v2/RepositoryRegistrar.java`

#### BUG-7: RepositoryRegistrar 中不安全的 `(Class<?>) resolved[0]` 转换

`RepositoryRegistrar.java` 第 60-61 行：`resolved[0]` 类型为 `Type`，无条件强转为 `Class<?>`。当泛型解析结果为 `ParameterizedType`（如 `List<String>`）或 `TypeVariable` 时将抛出 `ClassCastException`。

**涉及文件:** `mongo-flex-core/.../v2/RepositoryRegistrar.java`

#### BUG-24: `doUpdateOneById` 硬编码 `ObjectId.isValid()` 绕过 IdType 检查

`SimpleMongoRepository.java` 第 341 行在 `doUpdateOneById` 中直接用 `ObjectId.isValid((String) id)` 判断是否转为 ObjectId，绕过了 `shouldConvertToObjectId()`（需检查 `@CollectionId(IdType.XXX)`）。如果实体 ID 类型不是 `OBJECT_ID`（如 ULID/UUID），且 ID 值恰好是 24 位合法 hex 字符串，会被错误转为 ObjectId 导致 `_id` 类型不匹配、查询静默返回空。

已有的 `convertIdIfNecessary()`（第 676 行）和 `buildFilterFromLambda()`（第 772 行）已正确调用 `shouldConvertToObjectId()`，但 `doUpdateOneById` 是遗漏的代码路径——第 341 行仍使用原始的 `ObjectId.isValid()` 启发式判断。另外 `insertMany()` 第 127 行 `bsonValue.asObjectId()` 同样未做 IdType 检查，但在 OBJECT_ID 模式下该路径仅在 ID 已被 MongoDB 生成 ObjectId 时才会执行到（IdType 为其他模式时 ID 已通过 `fillId()` 预填充，不会走到回填逻辑），当前行为恰巧正确但脆弱。

**涉及文件:** `mongo-flex-core/.../v2/SimpleMongoRepository.java`

---

### 🟠 中等严重度 — 空值安全

#### BUG-8: LambdaQueryWrapper 多个方法缺少空值检查

以下方法的参数未做 `Objects.requireNonNull`，传入 null 时渲染阶段抛出 NPE：

| 方法 | 参数 | 问题 |
|------|------|------|
| `regex(SFunction, String)` | pattern | `c.value().toString()` NPE |
| `like(SFunction, String)` | pattern | `c.value().toString()` NPE |
| `notLike(SFunction, String)` | pattern | `c.value().toString()` NPE |
| `type(SFunction, String)` | typeName | `c.value().toString()` NPE |
| `elemMatch(SFunction, LambdaQueryWrapper)` | subWrapper | `subWrapper.getConditions()` NPE |
| `in/nin/all(SFunction, Collection)` | collection | `Filters.in(field, null)` 无效 BSON |
| `between(SFunction, R, R)` | start/end | null 值传入 `Filters.gte/lte` |

**涉及文件:** `mongo-flex-core/.../lambda/LambdaQueryWrapper.java`

#### BUG-9: MongoBsonRenderer switch 无 default 分支

`MongoBsonRenderer.java` 第 83-201 行的 `switch(c.operator())` 没有 `default` 分支。新增 `Operator` 枚举值但忘记加 case 时，条件被静默跳过无任何警告。

**涉及文件:** `mongo-flex-core/.../lambda/MongoBsonRenderer.java`

#### BUG-10: CollectionNameUtil / DateValueGenerator / ReflectUtil 缺少空值检查

- `CollectionNameUtil.getByObj(null)` / `getByClass(null)` → 直接 NPE
- `DateValueGenerator.generateCurrentDate(fieldType=null, ...)` → `fieldType.getName()` NPE
- `DateValueGenerator.getFormatter(null)` → `DateTimeFormatter.ofPattern(null)` NPE
- `ReflectUtil.getFieldNameFromLambda(null)` → `func.getClass()` NPE

**涉及文件:** `mongo-flex-core/.../util/CollectionNameUtil.java`, `DateValueGenerator.java`, `ReflectUtil.java`

#### BUG-11: JsonTemplateParser.toJsonValue 非 String/Number/Boolean 类型转为 toString() 非法 JSON

`JsonTemplateParser.java` 第 66 行将非 String/Number/Boolean 类型的值调用 `toString()` 后加双引号。`Date` 的 `toString()` 输出 `"Mon Jul 13 10:00:00 CST 2026"`，不是合法的 MongoDB Extended JSON 日期格式。

**涉及文件:** `mongo-flex-core/.../v2/JsonTemplateParser.java`

---

### 🟠 中等严重度 — 设计与正确性

#### BUG-12: MongoFlexAutoConfiguration 缺少 @ConditionalOnMissingBean

三个 `@Bean` 方法均无 `@ConditionalOnMissingBean`。用户在项目中定义自己的 `MongoMappingConvertor` 或 `DynamicMongoClient` 时，Spring 会抛 `BeanDefinitionOverrideException`。

**涉及文件:** `mongo-flex-core/.../v2/MongoFlexAutoConfiguration.java`

#### BUG-13: RepositoryRegistrar 跨包同名接口静默丢弃

`RepositoryRegistrar.java` 第 69-72 行：beanName 仅用接口简单类名计算。不同包中的同名 `@MRepository` 接口，第二个被 `!registry.containsBeanDefinition(beanName)` 静默跳过，无任何日志。

**涉及文件:** `mongo-flex-core/.../v2/RepositoryRegistrar.java`

#### BUG-14: DynamicMongoClient 多租户初始化失败时仅报告首个错误

`DynamicMongoClient.java` 第 96-99 行：多个租户初始化失败时，`errors` map 收集了所有错误，但抛出异常时只取 `errors.values().iterator().next()`，其余错误仅记日志。应使用 `suppressed exceptions` 包含所有错误。

**涉及文件:** `mongo-flex-core/.../v2/DynamicMongoClient.java`

#### BUG-15: DynamicMongoClient 使用非线程安全的 HashMap

`DynamicMongoClient.java` 第 28 行 `clients` 字段为 `HashMap`（非线程安全）。虽初始化后只读，但未用 `final` 修饰且未包装为 `unmodifiableMap`，缺少内存可见性保证。

**涉及文件:** `mongo-flex-core/.../v2/DynamicMongoClient.java`

#### BUG-16: RepositoryFactoryBean 中 idClass 字段存储但从未使用

`RepositoryFactoryBean.java` 第 34 行 `private final Class<ID> idClass;` 赋值后从未被读取。可移除以简化构造函数。

**涉及文件:** `mongo-flex-core/.../v2/RepositoryFactoryBean.java`

#### BUG-17: TenantConfig 无验证注解

`TenantConfig.java` 中 `name`、`uri` 字段无 `@NotBlank` / `@NotEmpty` 验证。null 值会在运行时导致隐蔽 NPE。

**涉及文件:** `mongo-flex-core/.../config/TenantConfig.java`

#### BUG-18: MongoMappingConvertor 数组/Set 类型未处理

`MongoMappingConvertor.processFieldValue()` 无数组（`int[]`、`String[]`）或 `Set`/`Collection` 的处理分支。数组会落入嵌套 POJO 路径，反射迭代无字段产生空 Map，静默丢数据。

**涉及文件:** `mongo-flex-core/.../convertor/MongoMappingConvertor.java`

#### BUG-19: LambdaQueryWrapper.or() 作用域不直观

`or(subWrapper)` 之后的 `.eq()` 调用追加到与 subWrapper 条件同一 OR 组。用户期望 `w.eq("a",1).or(sub).eq("b",2)` 中 `eq("b",2)` 是 AND 条件，但实际它属于第二 OR 分支。与 SQL OR 作用域惯例不符。

**涉及文件:** `mongo-flex-core/.../lambda/LambdaQueryWrapper.java`

#### BUG-20: Condition OR 分隔符无类型区分

`Condition` 的无参构造函数设置 `field=null, operator=null`，与 `isOrSeparator()` 判断逻辑耦合。任何无意中将两者设为 null 的 Condition 都会被误判为 OR 分隔符。

**涉及文件:** `mongo-flex-core/.../lambda/Condition.java`

#### BUG-21: RepositoryRegistrar 回退 basePackage 静默扫描错误包

`RepositoryRegistrar.java` 第 47 行：`AutoConfigurationPackages.get()` 为空时回退到 `MongoFlexAutoConfiguration` 所在包，可能扫描框架自身而非用户包，也可能遗漏用户仓库。至少应 `log.warn()` 提示扫了哪个包。

**涉及文件:** `mongo-flex-core/.../v2/RepositoryRegistrar.java`

#### BUG-22: RepositoryRegistrar 中 Class.forName 未指定类加载器

`RepositoryRegistrar.java` 第 98 行 `Class.forName(className)` 使用线程上下文类加载器，在 OSGi / Spring Boot uber-jar / 应用服务器等非标准环境中可能失败。应使用 `ClassUtils.forName()`。

**涉及文件:** `mongo-flex-core/.../v2/RepositoryRegistrar.java`

#### BUG-23: resolveTypeVariable 无循环检测

`RepositoryRegistrar.java` 第 189-195 行递归解析类型变量，无 visited set。循环泛型层次（如 `A<T extends A<T>>`）会导致 `StackOverflowError`。

**涉及文件:** `mongo-flex-core/.../v2/RepositoryRegistrar.java`

#### BUG-25: `convertToMap` 中对系统类型的死代码 ClassCastException

`MongoMappingConvertor.java` 第 106-108 行：

```java
if (isPrimitiveOrSystemType(clazz)) {
    return (Map<String, Object>) entity;
}
```

当 entity 是 String/Date/Number 等系统类型时，强制转换为 `Map<String, Object>` 必然抛出 `ClassCastException`。当前因为唯一递归调用方 `processFieldValue()` 在调用前做了 `!isPrimitiveOrSystemType(valueClass)` 守卫，此分支永远不会执行。但代码结构隐藏风险——未来如有新的调用路径到达这里会直接崩溃。应改为抛出明确的 `IllegalArgumentException` 或完全移除此分支。

**涉及文件:** `mongo-flex-core/.../convertor/MongoMappingConvertor.java`

#### BUG-26: `DynamicMongoClient` Bean 名称拼接未过滤特殊字符

`DynamicMongoClient.java` 第 82 行将租户名直接拼接到 Spring Bean 名称 `MONGO_CLIENT_PREFIX + tenantId`。Spring 的 Bean 名称不允许包含 `.`、`/`、`[`、`]` 等特殊字符，如果 `TenantConfig.name` 或 `tablePrefix` 包含这些字符，`genericApplicationContext.registerBean()` 会抛异常导致应用启动失败。

**涉及文件:** `mongo-flex-core/.../v2/DynamicMongoClient.java`

---

### 🟡 轻微 — 代码规范 & 代码质量

#### MINOR-1: FrameworkExclusionFilter 存在未使用的 import

`import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;` 和 `import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;` 从未使用。

**涉及文件:** `mongo-flex-core/.../config/FrameworkExclusionFilter.java`

#### MINOR-2: MongoFlexConstant 缺少 private 构造函数

常量工具类没有 private 构造函数，允许 `new MongoFlexConstant()`。

**涉及文件:** `mongo-flex-core/.../constant/MongoFlexConstant.java`

#### MINOR-3: CollectionField / CollectionName 缺少 Javadoc 且注释错误

`CollectionField.java` 中的行注释实际描述的是 `CollectionName` 的语义（复制粘贴错误）。两个文件均无类级别 Javadoc。

**涉及文件:** `mongo-flex-core/.../annotation/CollectionField.java`, `CollectionName.java`

#### MINOR-4: Param 注解完全没有 Javadoc

是代码库中唯一没有任何文档的注解。应说明 `value()` 用作 `#{paramName}` 占位符名。

**涉及文件:** `mongo-flex-core/.../v2/Param.java`

#### MINOR-5: IdType 枚举常量注释仅中文

类级 Javadoc 和 4 个枚举值中 3 个仅中文，未遵循双语注释规范。`OBJECT_ID` 是唯一有双语的。

**涉及文件:** `mongo-flex-core/.../constant/IdType.java`

#### MINOR-6: PageDTO 使用 @Data 在大集合 DTO 上

`@Data` 生成包含 `records` 列表的 `equals/hashCode`，大量记录时分页 DTO 比较开销极大。应改为 `@Getter/@Setter`。

**涉及文件:** `mongo-flex-core/.../entity/PageDTO.java`

#### MINOR-7: CollectionField.fill 属性类型不安全

`fill()` 为 `String` 类型，接受 `FillConstant` 值但无编译期检查。默认值 `""` 无对应的 `FillConstant.NONE` 常量配对。

**涉及文件:** `mongo-flex-core/.../annotation/CollectionField.java`, `constant/FillConstant.java`

#### MINOR-8: MongoFlexProperties 使用 @Data 暴露内部缓存

`@Data` 为 `cachedDatabase` 和 `tenantDatabaseCache` 生成 public setter，允许外部意外覆盖缓存。

**涉及文件:** `mongo-flex-core/.../config/MongoFlexProperties.java`

#### MINOR-9: FrameworkExclusionFilter 中常量集合未不可变

`EXCLUDED_CONFIGURATIONS` 是可变 `HashSet`，初始化后再无修改。应包装为 `Collections.unmodifiableSet()`。

**涉及文件:** `mongo-flex-core/.../config/FrameworkExclusionFilter.java`

#### MINOR-10: CollectionNameUtil 租户 ID 解析脆弱

`tenantId.split("_")` 假设格式为 `<namespace>_<tenantName>`。无下划线时静默忽略租户，多个下划线时仅用第二个片段。

**涉及文件:** `mongo-flex-core/.../util/CollectionNameUtil.java`

#### MINOR-11: CollectionNameUtil 错误消息仅中文

与其他文件的异常消息使用双语的做法不一致。

**涉及文件:** `mongo-flex-core/.../util/CollectionNameUtil.java`

#### MINOR-12: ReflectUtil.getImplClassFromLambda 返回声明类而非具体类

子类继承父类 getter 时（如 `Child extends Parent`，`Child::getName`），返回的是 `Parent.class` 而非 `Child.class`。调用方用此查找 `ClassFieldMetaData` 可能获取到错误类的元数据。

**涉及文件:** `mongo-flex-core/.../util/ReflectUtil.java`

#### MINOR-13: MongoMappingConvertor.write/read 空值契约不一致

`write(null)` 抛出 `IllegalArgumentException`，但 `read(null, ...)` 静默返回 null。两个方法无统一的 null 处理策略。

**涉及文件:** `mongo-flex-core/.../convertor/MongoMappingConvertor.java`

#### MINOR-14: FrameworkExclusionFilter 缺少类级 Javadoc

仅有一行中文注释解释目的，无双语 Javadoc。

**涉及文件:** `mongo-flex-core/.../config/FrameworkExclusionFilter.java`

#### MINOR-15: TenantConfig / MongoFlexConstant 完全无 Javadoc

零注释、零文档。

**涉及文件:** `mongo-flex-core/.../config/TenantConfig.java`, `constant/MongoFlexConstant.java`

#### MINOR-16: ClassFieldMetaData 构造函数接受 null class 参数

传入 null 时不报错，静默产生空元数据对象，下游调用返回 null/空结果，难以排查。

**涉及文件:** `mongo-flex-core/.../convertor/ClassFieldMetaData.java`

---

## 架构重构（来自 ARCHITECTURE_REFACTOR.md）

以下条目按 ARCHITECTURE_REFACTOR.md 规划的实施优先级排列，需依次实现。

### Phase 1：统一查询抽象 `QuerySpec<T>`

当前三条查询路径（SFunction 方法引用、LambdaQueryWrapper、@Find/@Count/@Delete 注解）各自独立构建 filter（Document 或 Bson），没有统一的查询抽象接口。需要创建 `QuerySpec<T>` 接口，让所有查询路径产出物都实现同一个接口。

```java
public interface QuerySpec<T> {
    Bson toBson(MongoMappingConvertor convertor);
    Class<T> getEntityClass();
}
```

**涉及文件:**
- 新建 `query/QuerySpec.java`
- `LambdaQueryWrapper.java` — 实现 `QuerySpec<T>`
- `MongoRepository.java` — `LambdaQueryWrapper<T>` 参数逐步替换为 `QuerySpec<T>`，旧方法标记 `@Deprecated`

### Phase 2：统一执行引擎 `QueryExecutor<T>`

当前 `SimpleMongoRepository` 是事实上的执行交汇点，但它通过直接实现 `MongoRepository` 接口承担了过多职责。各条路径调用 `SimpleMongoRepository` 上不同的方法（如 `findOne(LambdaQueryWrapper)` vs `findOneByFilter(Bson)` vs `findOne(SFunction, value)`），而不是汇入统一的执行器。需要抽取 `QueryExecutor<T>` 合并重复的 find/findOne/count/delete 逻辑。

```java
public class QueryExecutor<T, ID> {
    public List<T> find(QuerySpec<T> filter, Integer skip, Integer limit) { ... }
    public T findOne(QuerySpec<T> filter) { ... }
    public long count(QuerySpec<T> filter) { ... }
    public boolean delete(QuerySpec<T> filter) { ... }
    public long deleteMany(QuerySpec<T> filter) { ... }
    public long update(QuerySpec<T> filter, T entity) { ... }
    public long upsert(QuerySpec<T> filter, T entity) { ... }
    public <R> List<R> aggregate(List<Bson> pipeline, Class<R> outputClass) { ... }
}
```

**涉及文件:**
- 新建 `query/QueryExecutor.java`
- `SimpleMongoRepository.java` — 委托给 `QueryExecutor`，自身只做参数适配
- 注解处理器（`@Find`/`@Count`/`@Delete`）— 构建 `MqlQuerySpec` → `QueryExecutor`

### Phase 3：LambdaAggregationWrapper —— 聚合/联表查询

当前无类型安全的聚合流水线构建能力。`@Find`/@Count/@Delete 只覆盖了 CRUD 操作，聚合查询（group、lookup 等）只能手写 pipeline JSON。需要创建 `LambdaAggregationWrapper<T>`，核心复用点是 `$match` 阶段直接复用 `MongoBsonRenderer.render()`，21 个 operator 零成本可用。

```
LambdaAggregationWrapper ─┬── .match(LambdaQueryWrapper) → 复用 MongoBsonRenderer
                           ├── .group(field).sum().count() → PipelineStage
                           ├── .lookup(from, local, foreign, as) → PipelineStage
                           ├── .sort() / .limit() / .skip() → PipelineStage
                           └── .render() → List<Bson> → collection.aggregate()
```

**涉及文件:**
- 新建 `query/LambdaAggregationWrapper.java`
- 新建 `query/PipelineStage.java`（MatchStage, GroupStage, LookupStage, SortStage 等）
- `MongoRepository.java` — 新增 `aggregate()` 方法
- `SimpleMongoRepository.java` — 实现 `aggregate()`

### Phase 4：Repository 接口分层 <sup>已废弃</sup>

~~当前 30+ 个方法平铺在一个 `MongoRepository` 接口中，无继承层次。需要拆出最小 API 接口，降低用户入门门槛。~~

**已废弃。** 三层拆分（CrudRepository → QueryRepository → MongoRepository）过度设计，已回退为单层 `MongoRepository`。参考 MyBatis-Plus 的 `BaseMapper<T>`，单层接口更简单直观。

~~涉及文件:~~
- ~~新建 `CrudRepository.java`~~
- ~~新建 `QueryRepository.java`~~
- ~~`MongoRepository.java` — 改为 `extends QueryRepository<T, ID>`~~
- ~~`SimpleMongoRepository.java` — 改为 `implements MongoRepository<T, ID>`~~
- ~~`RepositoryRegistrar.java` — 支持三种接口类型的声明~~

### Phase 5：代理分发插件化 `MethodHandler`

当前 `MyRepositoryProxyHandler.doInvoke()` 通过硬编码 `if (method.isAnnotationPresent(Find.class))` 等分支判断，无扩展点。需要改为 `MethodHandler` 责任链，每种方法类型（@Find/@Mql、Repository 继承、未来扩展）各自实现。

```java
public interface MethodHandler {
    boolean supports(Method method);
    Object invoke(Method method, Object[] args) throws Exception;
}
```

**涉及文件:**
- 新建 `MethodHandler.java`
- 新建 `FindMethodHandler.java` / `CountMethodHandler.java` / `DeleteMethodHandler.java`
- `MyRepositoryProxyHandler.java` — 改为委托给 handler 列表
- `RepositoryFactoryBean.java` — 装配 handler 列表

### Phase 6：补充缺失能力

| 能力 | 说明 |
|------|------|
| `LambdaUpdateWrapper` | 支持 `set(Entity::getField, value)` 链式部分字段更新 |
| `QueryWrapper<T>` | 字符串字段名版本的 query wrapper（类似 MyBatis Plus） |
| `existsById()` / `exists(QuerySpec)` | 存在性判断，避免 `count > 0` |
| `save()` | upsert-on-id（根据 ID 是否有值决定 insert 还是 update） |
| 方法名推导查询 | `findByNameAndAge` → 自动解析为 query |
| `$text` / `$near` 操作符 | LambdaQueryWrapper 中补充地理位置查询和全文搜索操作符 |

---

## 实施顺序

| 优先级 | Phase | 理由 |
|--------|-------|------|
| P0 | Phase 1：`QuerySpec<T>` 抽象 | 所有后续工作的基础 |
| P1 | Phase 2：统一 `QueryExecutor<T>` | 消除重复，让所有路径共享执行逻辑 |
| P2 | Phase 3：LambdaAggregationWrapper | 聚合/联表查询的类型安全支持 |
| P3 | Phase 4：接口分层 | 渐进式 @Deprecated 过渡 |
| P4 | Phase 5：代理插件化 | 锦上添花，当前 if-else 够用 |
| P5 | Phase 6：补充能力 | 按需实现，不阻塞架构重构 |
