# mongo-flex

[English](README.md)

## 这是什么？

Mongo-flex 是一个轻量级 MongoDB 工具库，提供 Repository 抽象、Lambda 查询构造器和 `@Mql` 注解来简化 MongoDB 开发。

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

## 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>io.github.eacryo</groupId>
    <artifactId>mongo-flex</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 定义实体

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

### 3. 创建 Repository 接口

```java
@MRepository
public interface CharacterRepository extends MongoRepository<Character, String> {

    @Mql("db.getCollection('character').find({'name':'#{name}'})")
    List<Character> findByName(@Param("name") String name);

    @Mql("db.getCollection('character').count({})")
    long countAll();
}
```

### 4. 使用

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

// upsert：存在则更新，不存在则插入
Character ganyu = new Character();
ganyu.setId("some-id");
ganyu.setName("Ganyu");
ganyu.setArea("Liyue");
repo.upsertById(ganyu);  // 按 _id upsert
repo.upsert(Character::getName, "Ganyu", ganyu);  // 按字段 upsert

// OR 查询
wrapper.or(w -> w.eq(Character::getArea, "Liyue")
                  .eq(Character::getArea, "Fontaine"));
List<Character> orResult = repo.findList(wrapper);
```

#### Lambda 方法引用 → 字段名解析规则

Lambda 查询包装器通过方法引用提取字段名，解析规则遵循 **MyBatis `PropertyNamer` 规范**（即 JavaBeans 标准）：

| 方法引用 | 解析出的字段 | 规则 |
|---|---|---|
| `Entity::getName` | `name` | 去除 `get` 前缀，首字母小写 |
| `Entity::isActive` | `active` | 去除 `is` 前缀，首字母小写 |
| `Entity::getURL` | `URL` | 首字母缩写：第二个字母大写 → 保留首字母大写 |

> **已知限制：** `isActive()` 这样的方法具有天然歧义——它可能是 `boolean active` 的 getter，也可能是 `boolean isActive` 的 getter。解析器始终按 JavaBeans 约定采用前一种解释。如果你的字段确实叫 `isActive`，请使用 `@CollectionField("is_active")` 覆盖 MongoDB 字段映射，或将 Java 字段重命名为 `active`。

### 5. 实体继承

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

### 6. 多租户（可选）

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

如果不启用多租户，默认会使用标准 `spring.data.mongodb.uri` 配置创建一个 `MongoClient`。
