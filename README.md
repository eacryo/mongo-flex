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
    // Available ID strategies:
    //   IdType.OBJECT_ID  — MongoDB native ObjectId (default, stored as 24-char hex String)
    //   IdType.ULID  — 26-char sortable unique ID, recommended for new projects
    //   IdType.UUID  — standard UUID v4
    //   IdType.INPUT — custom IdGenerator implementation

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

// entity example queries
Character probe = new Character();
probe.setName("Furina");
Character one = repo.findOneByEntity(probe);     // find first match
List<Character> list = repo.findListByEntity(probe);  // find all matching

// pagination with Lambda sort
PageDTO<Character> page = new PageDTO<>();
page.setCurrentPage(1L);
page.setPageSize(10L);

LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>(Character.class);
wrapper.eq(Character::getArea, "Fontaine")
       .orderByAsc(Character::getName);
PageDTO<Character> result = repo.findPage(wrapper, page);
result.getRecords();   // current page data
result.getTotal();     // total matching documents

// upsert: update if exists, insert if not
Character ganyu = new Character();
ganyu.setId("some-id");
ganyu.setName("Ganyu");
ganyu.setArea("Liyue");
repo.upsertById(ganyu);  // upsert by _id
repo.upsert(Character::getName, "Ganyu", ganyu);  // upsert by field

// OR queries
wrapper.or(w -> w.eq(Character::getArea, "Liyue")
                  .eq(Character::getArea, "Fontaine"));
List<Character> orResult = repo.findList(wrapper);
```

### 5. Entity Inheritance

Mongo-flex follows the **MyBatis-Plus style** of explicit type binding: a `Repository<T>` works with exactly `T` — no more, no less.

```java
// Parent entity
@CollectionName("character")
public class Character {
    @CollectionId(IdType.ULID)
    private String id;
    private String name;
    private String vision;
}

// Child entity with extra fields
public class LiyueCharacter extends Character {
    private String title;              // 称号
    @CollectionField("is_adeptus")
    private Boolean isAdeptus;         // 是否仙人
}
```

**Key rule:** Use a dedicated Repository for each type you want to fully read/write.

```java
// ✅ Read parent fields via parent Repository
@MRepository
public interface CharacterRepository extends MongoRepository<Character, String> {}

Character c = characterRepo.findById(id);
c.getVision();  // ✅ works

// ✅ Read all fields (parent + child) via child Repository
@MRepository
public interface LiyueCharacterRepository extends MongoRepository<LiyueCharacter, String> {}

LiyueCharacter lc = liyueRepo.findById(id);
lc.getVision();     // ✅ inherited field
lc.getTitle();      // ✅ child field
lc.getIsAdeptus();  // ✅ child field, @CollectionField("is_adeptus") honored
```

```java
// ❌ Don't expect child fields through parent Repository
Character c = characterRepo.findById(id);
c.getTitle();       // ❌ compile error — Character has no getTitle()
c instanceof LiyueCharacter;  // ❌ always false — read() returns Character, never LiyueCharacter
```

**Why not auto-polymorphism like Spring Data MongoDB?** Spring Data stores a `_class` discriminator and automatically instantiates the subclass — but this means `repo.findById(id)` can silently return a `LiyueCharacter` when you declared `Character`. It's flexible but requires `instanceof` guards. Mongo-flex chooses explicit type binding: insert stores all fields (runtime type), but read returns only what the Repository interface declares (compile-time type). No surprises.

### 6. Multi-tenancy (optional)

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
