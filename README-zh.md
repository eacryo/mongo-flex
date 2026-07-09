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
```

### 5. 多租户（可选）

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
