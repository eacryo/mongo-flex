# mongo-flex

[中文](README-zh.md)

## What is it?

Mongo-flex is a lightweight MongoDB toolkit for simplifying development. It provides a repository abstraction, lambda-based query builder, and `@Mql` annotation for raw MongoDB shell queries.

## Compatibility

Mongo-flex is compiled to Java 8 bytecode and is compatible with **JDK 8+** and **Spring Boot 2.7.x / 3.x**.

**Important:** MongoDB driver and Spring Boot starter are declared as `optional` in mongo-flex's pom. This means they will NOT be pulled into your project automatically. You need to ensure the following dependencies are present (usually via your own Spring Boot starter):

- `spring-boot-starter` (or `spring-boot-autoconfigure`) — provided by your Spring Boot project
- `mongodb-driver-sync` — version depends on your Spring Boot version:

| Spring Boot | MongoDB driver version |
|-------------|----------------------|
| 2.7.x       | 4.11.x               |
| 3.x         | 5.x (managed by BOM) |

**For Spring Boot 3.x users:** No extra config needed — SB3 already manages MongoDB driver 5.x.

**For Spring Boot 2.7.x / JDK 8 users:** Add the MongoDB driver explicitly:

```xml
<dependency>
    <groupId>org.mongodb</groupId>
    <artifactId>mongodb-driver-sync</artifactId>
    <version>4.11.1</version>
</dependency>
```

## Quick Start

### 1. Add dependency

```xml
<dependency>
    <groupId>io.github.eacryo</groupId>
    <artifactId>mongo-flex</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Define your entity

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

### 3. Create a repository interface

```java
@MRepository
public interface CharacterRepository extends MongoRepository<Character, String> {

    @Mql("db.getCollection('character').find({'name':'#{name}'})")
    List<Character> findByName(@Param("name") String name);

    @Mql("db.getCollection('character').count({})")
    long countAll();
}
```

### 4. Use it

```java
@Autowired
private CharacterRepository repo;

// insert
Character c = new Character();
c.setName("Furina");
c.setArea("Fontaine");
repo.insert(c);

// find by id
Character found = repo.findById(c.getId());

// find with lambda query wrapper
LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
wrapper.eq(Character::getName, "Furina");
List<Character> list = repo.findList(wrapper);

// find all
List<Character> all = repo.findAll();

// count
long total = repo.count();

// delete all (explicit opt-in)
long deleted = repo.deleteAll();
```

### 5. Multi-tenancy (optional)

```yaml
mongo-flex:
  enable-multi-tenants: true
  tenants:
    - name: tenantA
      uri: mongodb://localhost:27017/db_a
```

Set the active tenant before each operation:

```java
MDC.put(MongoFlexConstant.TENANT, "tenantA");
repo.insert(c);
MDC.clear();
```

If multi-tenancy is not enabled, a default `MongoClient` is created using the standard `spring.data.mongodb.uri` property.
