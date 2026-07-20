# mongo-flex

[English](README.md)

## 这是什么？

Mongo-flex 是一个轻量级 MongoDB 工具库，提供三种查询路径，全部收敛为 MongoDB `Bson`：

| 路径 | 机制 | 适用场景 |
|---|---|---|
| Repository 方法 | `MongoRepository<T,ID>` 接口 | 按 ID 的 CRUD、实体示例查询、Lambda 字段查询 |
| Lambda 类型安全查询 | `LambdaQueryWrapper<T>` + 操作符 | 类型安全的动态查询，21 种操作符 |
| 注解驱动 JSON 查询 | `@Find` / `@Count` / `@Delete` / `@Update` 注解 | 复杂 / 临时 JSON 查询 |

**`MongoRepository<T, ID>`** — 覆盖所有操作的单层接口：

- 基础 CRUD — insert, insertMany, findById, findAll, count, deleteOneById, deleteAll, updateOneById
- 按实体 — findOneByEntity, findListByEntity, findPageByEntity, countByEntity, deleteByEntity
- 按 Lambda 引用 — findOne, count, updateOne, updateMany, deleteOne, deleteMany (SFunction)
- 按 LambdaQueryWrapper — findOne, findList, findPage, count, update, delete

编译目标为 Java 8 字节码，兼容 JDK 8+ 和 Spring Boot 2.7.x / 3.x。

## 版本兼容性

Mongo-flex 编译为 Java 8 字节码，兼容 **JDK 8+** 和 **Spring Boot 2.7.x / 3.x**。

**重要：** MongoDB driver 和 Spring Boot starter 在 mongo-flex 的 pom 中声明为 `optional`，这意味着它们**不会自动传递**到你的项目。你需要确保以下依赖已存在（通常由你的 Spring Boot starter 提供）：

- `spring-boot-starter`（或 `spring-boot-autoconfigure`）—— 由你的 Spring Boot 项目提供
- `mongodb-driver-sync` —— 版本取决于你的 Spring Boot 版本：

| Spring Boot | MongoDB driver 版本 |
|-------------|---------------------|
| 2.7.x       | 4.11.x              |
| 3.x         | 5.x（由 BOM 管理）    |

**Spring Boot 3.x 用户：** 无需额外配置，SB3 已自带 MongoDB driver 5.x。

**Spring Boot 2.7.x / JDK 8 用户：** 需要显式添加 MongoDB driver：

```xml
<dependency>
    <groupId>org.mongodb</groupId>
    <artifactId>mongodb-driver-sync</artifactId>
    <version>4.11.1</version>
</dependency>
```

**与 Spring Data MongoDB 可共存：** mongo-flex 仅排除 `MongoAutoConfiguration`（防止 Spring Boot 自动连接 `localhost:27017`），**不会干扰** `MongoDataAutoConfiguration`（Spring Data MongoDB 的自动配置类）。若你同时引入了 Spring Data MongoDB 并提供了 `MongoClient` Bean，两个框架可在同一项目中正常协作。

## 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>io.github.eacryo</groupId>
    <artifactId>mongo-flex</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置连接

```yaml
mongo-flex:
  uri: mongodb://localhost:27017/mydb
```

不启用多租户时，默认使用 `mongo-flex.uri` 创建一个 `MongoClient`。

### 3. 定义实体

```java
@CollectionName("character")
@Data
public class Character {

    @CollectionId(IdType.ULID)
    private String id;
    // 可用 ID 策略：
    //   IdType.OBJECT_ID  — MongoDB 原生 ObjectId（默认，Java 侧存为 24 字符 hex String）
    //   IdType.ULID  — 26 字符字典序可排序唯一 ID，推荐用于新项目
    //   IdType.UUID  — 标准 UUID v4
    //   IdType.INPUT — 自定义 IdGenerator 实现

    private String name;

    @CollectionField("c_area")
    private String area;

    @CreateDate
    private Date createAt;

    @UpdateDate
    private Date updateAt;
}
```

### 4. 创建 Repository 接口

选择你需要的接口层级：

```java
@MRepository
public interface CharacterRepository extends MongoRepository<Character, String> {

    @Find("{name: #{name}}")
    List<Character> findByName(@Param("name") String name);

    @Find(value = "{area: #{area}}", skip = 0, limit = 10)
    List<Character> findTop10ByArea(@Param("area") String area);

    @Update(value = "{name: #{name}}", update = "{$set: {level: #{level}}}")
    long updateLevelByName(@Param("name") String name, @Param("level") int level);
}
```

### 5. 使用

```java
@Autowired
private CharacterRepository repo;

// 插入
Character c = new Character();
c.setName("Furina");
c.setArea("Fontaine");
repo.insert(c);

// 按 ID 查询
Character found = repo.findById(c.getId());

// Lambda 查询构造器
LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
wrapper.eq(Character::getName, "Furina");
List<Character> list = repo.findList(wrapper);

// isNull / isNotNull — 对齐 SQL IS NULL / IS NOT NULL 语义
// isNull → { field: null }，匹配字段为 null 或不存在的文档
wrapper.isNull(Character::getPhone);
// isNotNull → { field: { $ne: null } }，匹配字段存在且非 null 的文档
wrapper.isNotNull(Character::getVision);

// 查询全部
List<Character> all = repo.findAll();

// 统计
long total = repo.count();

// 删除全部（显式操作）
long deleted = repo.deleteAll();

// 实体示例查询
Character probe = new Character();
probe.setName("Furina");
Character one = repo.findOneByEntity(probe);        // 查第一条匹配
List<Character> list = repo.findListByEntity(probe); // 查全部匹配

// 分页 + Lambda 排序
PageDTO<Character> page = new PageDTO<>();
page.setCurrentPage(1L);
page.setPageSize(10L);

LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
wrapper.eq(Character::getArea, "Fontaine")
       .orderByAsc(Character::getName);
PageDTO<Character> result = repo.findPage(wrapper, page);
result.getRecords();   // 当前页数据
result.getTotal();     // 匹配文档总数

// 更新单条（updateOne）
Character ganyu = new Character();
ganyu.setId("some-id");
ganyu.setArea("Liyue");
repo.updateOneById(ganyu);  // 按 _id 更新单条

// 更新多条（updateMany）
repo.updateMany(Character::getName, "Ganyu", ganyu);  // 按字段更新所有匹配文档

// upsert：存在则更新，不存在则插入（传入 upsert=true）
repo.updateOneById(ganyu, true);  // 按 _id upsert
repo.updateMany(Character::getName, "Ganyu", ganyu, true);  // 按字段 upsert

// 删除
repo.deleteOneById("some-id");  // 按 _id 删除单条
repo.deleteMany(Character::getName, "Ganyu");  // 按字段删除所有匹配

// OR 查询
wrapper.or(w -> w.eq(Character::getArea, "Liyue")
                  .eq(Character::getArea, "Fontaine"));
List<Character> orResult = repo.findList(wrapper);
```

#### Entity 转 Wrapper 工厂方法

`LambdaQueryWrapper.fromEntity(entity)` 从已填充的实体对象自动构建查询条件。每个非 null 字段自动生成 `eq()` 条件，static / transient 字段会被忽略：

```java
Character probe = new Character();
probe.setArea("Liyue");
// probe → wrapper.eq(Character::getArea, "Liyue")

List<Character> result = repo.findList(
    LambdaQueryWrapper.fromEntity(probe)
        .include(Character::getName, Character::getLevel)
        .orderByAsc(Character::getName)
);
```

#### 字段投影（include / exclude）

使用 `include()` 和 `exclude()` 限制返回字段：

```java
// 仅返回 name 和 age（_id 默认仍返回）
wrapper.include(Character::getName, Character::getAge);

// 排除敏感字段
wrapper.exclude(Character::getPassword, Character::getLargeData);

// 组合使用：包含指定字段但排除 _id
wrapper.include(Character::getName, Character::getAge)
       .exclude(Character::getId);  // _id 是混合模式下唯一允许排除的字段
```

> MongoDB 不允许在非 `_id` 字段上同时使用 include 和 exclude。若先调用了 `include()`，则 `exclude()` 仅能排除 `_id`。

#### Lambda 方法引用 → 字段名解析规则

Lambda 查询包装器通过方法引用提取字段名，解析规则遵循 **MyBatis `PropertyNamer` 规范**（即 JavaBeans 标准）：

| 方法引用 | 解析出的字段 | 规则 |
|---|---|---|
| `Entity::getName` | `name` | 去除 `get` 前缀，首字母小写 |
| `Entity::isActive` | `active` | 去除 `is` 前缀，首字母小写 |
| `Entity::getURL` | `URL` | 首字母缩写：第二个字母大写 → 保留首字母大写 |

> **已知限制：** `isActive()` 这样的方法具有天然歧义——它可能是 `boolean active` 的 getter，也可能是 `boolean isActive` 的 getter。解析器始终按 JavaBeans 约定采用前一种解释。如果你的字段确实叫 `isActive`，请使用 `@CollectionField("is_active")` 覆盖 MongoDB 字段映射，或将 Java 字段重命名为 `active`。

#### 嵌套字段查询（点号语法）

MongoDB 使用点号语法（`address.city`）定位嵌套子文档中的字段。三条查询路径的支持方式如下：

**Lambda 路径——类型安全的 `FieldPath` 链式方法引用：**

```java
public class Character {
    @CollectionField("home_region")
    private Region region;          // 嵌套对象
}
public class Region {
    private String nation;
    @CollectionField("main_city")
    private String mainCity;
    private Integer altitude;
}

// 两级路径 → 渲染为 {"home_region.main_city": "璃月港"}
wrapper.eq(FieldPath.of(Character::getRegion, Region::getMainCity), "璃月港");

// 流式写法，通过 then() 支持任意深度
wrapper.gt(FieldPath.of(Character::getRegion).then(Region::getAltitude), 1000);

// 排序与投影同样支持
wrapper.orderByDesc(FieldPath.of(Character::getRegion, Region::getAltitude))
       .include(FieldPath.of(Character::getRegion, Region::getMainCity));
```

路径的每一段都会应用 `@CollectionField` 映射（`region` → `home_region`、`mainCity` → `main_city`），`List` 段会透明穿透到其元素类型。所有 filter 操作符（`eq`/`gt`/`between`/`exists`/……）均提供 `FieldPath` 重载。

**注解路径——原始 JSON 点号键原生支持：**

```java
@Find("{'home_region.main_city': #{city}}")
List<Character> findByRegionCity(@Param("city") String city);
```

> 原始 JSON 中使用的是 MongoDB 字段名（`@CollectionField` 映射后的名字），不是 Java 字段名。

**Entity 路径（`*ByEntity`）——单层语义：**

按实体查询时，嵌套对象字段按**精确子文档**匹配——整个嵌套对象必须完全一致（所有字段，包括字段顺序），**不会**展平为点号逐字段匹配。需要逐字段嵌套匹配时，请改用 `FieldPath` 或 `@Find` 点号键。

```java
Character probe = new Character();
probe.setRegion(new Region("Liyue", "璃月港", 500));
repo.findListByEntity(probe);  // 仅命中整个子文档完全一致的文档
```

#### 日期/时间字段自动填充

使用 `@CreateDate` 和 `@UpdateDate` 在插入和更新时自动填充时间戳。无需配置——直接注解字段即可。

```java
@CreateDate
private LocalDateTime createAt;   // 插入时自动填充

@UpdateDate
private LocalDateTime updateAt;   // 每次插入和更新时自动填充
```

**内置类型支持：**

| 字段类型 | 生成值 |
|---|---|
| `java.util.Date` | `new Date()` |
| `String` | `LocalDateTime.now().format(pattern)` |
| `LocalDateTime` | `LocalDateTime.now()` |
| `LocalDate` | `LocalDate.now()` |
| `Instant` | `Instant.now()` |
| `Long` / `long` | `System.currentTimeMillis()` |

**String 字段自定义格式：**

```java
@CreateDate(pattern = "yyyy/MM/dd HH:mm")
private String createTime;  // → "2026/07/12 14:30"
```

**按字段自定义提供器：**

```java
// 1. 实现 DateValueProvider 接口
public class MyZonedProvider implements DateValueProvider {
    @Override
    public Object generateCurrentDate(Class<?> fieldType, String pattern) {
        if (fieldType == ZonedDateTime.class) return ZonedDateTime.now();
        return null; // 其他类型交给内置处理器
    }
}

// 2. 在字段上引用
@CreateDate(providerClass = MyZonedProvider.class)
private ZonedDateTime createAt;
```

**全局提供器（作用于所有字段）：**

注册一个 `DateValueProvider` Spring bean——它会在内置类型表之前作为默认回退。

> **解析优先级：** 注解上的 `providerClass` → 全局 `DateValueProvider` bean → 内置类型表。

### 6. 实体继承

Mongo-flex 采用 **MyBatis-Plus 风格**的显式类型绑定：`Repository<T>` 只处理 `T` 的字段，不多不少。

```java
// 父类实体
@CollectionName("character")
public class Character {
    @CollectionId(IdType.ULID)
    private String id;
    private String name;
    private String vision;
}

// 子类，包含额外字段
public class LiyueCharacter extends Character {
    private String title;              // 称号
    @CollectionField("is_adeptus")
    private Boolean isAdeptus;         // 是否仙人
}
```

**核心规则：** 每种需要完整读写的类型都应定义独立的 Repository。

```java
// ✅ 通过父类 Repository 读取父类字段
@MRepository
public interface CharacterRepository extends MongoRepository<Character, String> {}

Character c = characterRepo.findById(id);
c.getVision();  // ✅ 正常

// ✅ 通过子类 Repository 读取全部字段（父类 + 子类）
@MRepository
public interface LiyueCharacterRepository extends MongoRepository<LiyueCharacter, String> {}

LiyueCharacter lc = liyueRepo.findById(id);
lc.getVision();     // ✅ 继承字段
lc.getTitle();      // ✅ 子类字段
lc.getIsAdeptus();  // ✅ 子类字段，@CollectionField("is_adeptus") 正确映射
```

```java
// ❌ 不要期望通过父类 Repository 获取子类字段
Character c = characterRepo.findById(id);
c.getTitle();       // ❌ 编译错误 — Character 没有 getTitle()
c instanceof LiyueCharacter;  // ❌ 永远为 false — read() 返回的是 Character，不会变成 LiyueCharacter
```

**为什么不像 Spring Data MongoDB 那样自动多态？** Spring Data 会存储 `_class` 鉴别字段并自动实例化子类——但这意味着 `repo.findById(id)` 在你声明 `Character` 时可能悄悄返回一个 `LiyueCharacter`。灵活但需要 `instanceof` 防御。Mongo-flex 选择显式类型绑定：insert 存入全部字段（运行时类型），但 read 只返回 Repository 接口声明的字段（编译期类型）。没有意外。

### 7. 多租户（可选）

```yaml
mongo-flex:
  enable-multi-tenants: true
  tenants:
    - name: tenantA
      uri: mongodb://localhost:27017/db_a
```

每次操作前设置当前租户：

```java
MDC.put(MongoFlexConstant.TENANT, "tenantA");
repo.insert(c);
MDC.clear();
```

如果不启用多租户，默认会使用 `mongo-flex.uri` 配置创建一个 `MongoClient`。
