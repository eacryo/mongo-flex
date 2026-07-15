# mongo-flex

[中文](README-zh.md)

## What is it?

Mongo-flex is a lightweight MongoDB toolkit offering three query paths that converge into a unified `QuerySpec` abstraction and execution engine:

| Path | Mechanism | Use Case |
|---|---|---|
| Repository methods | `MongoRepository<T,ID>` interface | CRUD by ID, entity example, lambda field queries |
| Lambda type-safe queries | `LambdaQueryWrapper<T>` + operators | Type-safe dynamic queries, 21 operators |
| Annotation-driven JSON | `@Find` / `@Count` / `@Delete` | Complex / ad-hoc JSON queries |

**Interface hierarchy:**

```
CrudRepository<T,ID>       — insert, findById, findAll, count, deleteOneById, deleteAll
  └─ QueryRepository<T,ID>  — findOne, findList, findPage, count, update, delete (QuerySpec)
       └─ MongoRepository<T,ID> — SFunction / entity / LambdaQueryWrapper convenience methods
```

All query paths produce `Bson` and execute through a single `QueryExecutor<T>`. Compiled to Java 8 bytecode — compatible with JDK 8+ and Spring Boot 2.7.x / 3.x.

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

**Coexists with Spring Data MongoDB:** mongo-flex only excludes `MongoAutoConfiguration` (preventing Spring Boot from auto-connecting to `localhost:27017`) and intentionally leaves `MongoDataAutoConfiguration` (Spring Data MongoDB's auto-configuration) untouched. If you include Spring Data MongoDB and provide a `MongoClient` bean, both frameworks work together in the same project.

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

Choose the interface level you need:

```java
// Basic CRUD — insert / findById / findAll / count / deleteOneById / deleteAll only
@MRepository
public interface CharacterRepository extends CrudRepository<Character, String> {}

// CRUD + QuerySpec queries — all Lambda / MQL / entity query paths available
@MRepository
public interface CharacterRepository extends QueryRepository<Character, String> {}

// Full-featured — adds SFunction / entity / LambdaQueryWrapper convenience methods
@MRepository
public interface CharacterRepository extends MongoRepository<Character, String> {

    @Find("{name: #{name}}")
    List<Character> findByName(@Param("name") String name);
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

// updateOne — update first match
Character ganyu = new Character();
ganyu.setId("some-id");
ganyu.setArea("Liyue");
repo.updateOneById(ganyu);  // update by _id

// updateMany — update all matches
repo.updateMany(Character::getName, "Ganyu", ganyu);  // update by field

// upsert — update if exists, insert if not (pass upsert=true)
repo.updateOneById(ganyu, true);  // upsert by _id
repo.updateMany(Character::getName, "Ganyu", ganyu, true);  // upsert by field

// delete
repo.deleteOneById("some-id");  // delete one by _id
repo.deleteMany(Character::getName, "Ganyu");  // delete all matches by field

// OR queries
wrapper.or(w -> w.eq(Character::getArea, "Liyue")
                  .eq(Character::getArea, "Fontaine"));
List<Character> orResult = repo.findList(wrapper);
```

#### Field Name Resolution from Lambda Methods

Lambda query wrappers use method references to extract field names. The resolution follows the **MyBatis `PropertyNamer` convention** (JavaBeans standard):

| Method Reference | Resolved Field | Rule |
|---|---|---|
| `Entity::getName` | `name` | Strip `get` prefix, lowercase first char |
| `Entity::isActive` | `active` | Strip `is` prefix, lowercase first char |
| `Entity::getURL` | `URL` | Acronym: second char uppercase → keep first char uppercase |

> **Known Limitation:** A method like `isActive()` is inherently ambiguous — it could be the getter for `boolean active` or `boolean isActive`. The resolver always assumes the former (JavaBeans convention). If your field is literally named `isActive`, use `@CollectionField("is_active")` to override the MongoDB field mapping, or rename your Java field to `active`.

#### Auto-fill Date/Time Fields

Use `@CreateDate` and `@UpdateDate` to auto-fill timestamps on insert and update. No configuration needed — just annotate the field.

```java
@CreateDate
private LocalDateTime createAt;   // auto-filled on insert

@UpdateDate
private LocalDateTime updateAt;   // auto-filled on every insert & update
```

**Built-in type support:**

| Field Type | Generated Value |
|---|---|
| `java.util.Date` | `new Date()` |
| `String` | `LocalDateTime.now().format(pattern)` |
| `LocalDateTime` | `LocalDateTime.now()` |
| `LocalDate` | `LocalDate.now()` |
| `Instant` | `Instant.now()` |
| `Long` / `long` | `System.currentTimeMillis()` |

**Custom pattern for String fields:**

```java
@CreateDate(pattern = "yyyy/MM/dd HH:mm")
private String createTime;  // → "2026/07/12 14:30"
```

**Per-field custom provider:**

```java
// 1. Implement DateValueProvider
public class MyZonedProvider implements DateValueProvider {
    @Override
    public Object generateCurrentDate(Class<?> fieldType, String pattern) {
        if (fieldType == ZonedDateTime.class) return ZonedDateTime.now();
        return null; // let built-ins handle other types
    }
}

// 2. Reference on the field
@CreateDate(providerClass = MyZonedProvider.class)
private ZonedDateTime createAt;
```

**Global provider (applies to all fields):**

Register a `DateValueProvider` Spring bean — it serves as the default fallback before the built-in type table.

> **Resolution order:** `providerClass` on annotation → global `DateValueProvider` bean → built-in type table.

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
