# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Compile core module only
mvn compile -pl mongo-flex-core -am

# Run all SB3 tests (requires REMOTE_MONGO_URI env var)
mvn test -pl mongo-flex-test-spring-boot3

# Run a single test class
mvn test -pl mongo-flex-test-spring-boot3 -Dtest=LambdaQueryWrapperOperatorTest

# Run a single test method
mvn test -pl mongo-flex-test-spring-boot3 -Dtest=LambdaQueryWrapperOperatorTest#testLike

# Clean install core to local repo (needed when test-common depends on updated core)
mvn clean install -pl mongo-flex-core -am -DskipTests
```

Tests connect to a remote MongoDB Atlas cluster via the `REMOTE_MONGO_URI` environment variable. There is no embedded MongoDB. **Tests are slow** (remote Atlas latency): a single test class takes 2–5 minutes; the full suite takes 10+ minutes. Always use a generous timeout when running test commands (≥ 600 seconds for Bash tool timeout).

## Java 8 Constraint

The project targets **Java 8 bytecode** (`<java.version>1.8</java.version>`). Do not use APIs introduced after JDK 8:
- ❌ `List.of()`, `Set.of()`, `Map.of()` — use `Arrays.asList()` or `new ArrayList<>()`
- ❌ `var` keyword
- ❌ `java.time.*` (unless the dependency already brings it in)
- ❌ Streams `.toList()` (Java 16) — use `.collect(Collectors.toList())`

## Commit Message Convention

Use **Conventional Commits** format:

```
<type>: <description>

type: feat|fix|refactor|test|docs|chore|style|perf
```

Do **not** run `git commit`. Suggest the message and let the developer commit manually.

## Documentation Sync

When modifying `README.md`, apply the same changes to `README-zh.md` (Chinese translation). Both files must stay in sync.

## Architecture Overview

**mongo-flex** is a lightweight MongoDB toolkit for Spring Boot providing three query paths:

| Path | Mechanism | Use Case |
|---|---|---|
| Repository methods | `MongoRepository<T,ID>` interface (v2 package) | CRUD by ID, entity example, simple field=value |
| Lambda queries | `LambdaQueryWrapper<T>` + 13 operators | Type-safe dynamic queries |
| Raw MQL | `@Mql` annotation with shell-like syntax | Complex/adhoc queries |

### Module Map

```
mongo-flex-core/          ← framework code
mongo-flex-test-common/   ← shared test entities & repository interfaces
mongo-flex-test-spring-boot3/  ← integration tests (active profile: v2)
mongo-flex-test-spring-boot2/  ← legacy SB2 tests
```

### Key Design Decisions

- **Entity mapping**: `@CollectionId` (maps to `_id`), `@CollectionField("mongo_name")`, implicit `id`→`_id` mapping. Priority: `@CollectionId` > `@CollectionField` > field name heuristic.
- **ID generation**: Controlled by `IdType` enum — `NONE` (MongoDB ObjectId, stored as hex String), `ULID` (26-char sortable), `UUID` (v4), `INPUT` (custom `IdGenerator`).
- **ObjectId conversion**: Only converts String→ObjectId when `IdType.NONE` (MongoDB generates ObjectId but Java stores hex String). Other modes store native String.
- **Repository proxies**: `@MRepository` interfaces get JDK proxy via `RepositoryFactoryBean`. `@Mql` methods dispatched to executors; `MongoRepository` methods delegated to `SimpleMongoRepository`.
- **Field name resolution**: `ReflectUtil.getFieldNameFromLambda()` extracts field name from `SFunction` via `SerializedLambda`; `MongoMappingConvertor.resolveMongoFieldName()` maps Java→MongoDB field names using cached `ClassFieldMetaData`.

### Lambda Query Operators

Full operator list in `LambdaQueryWrapper`: `eq`, `ne`, `gt`, `lt`, `gte`, `lte`, `regex`, `like`, `notLike`, `in`, `nin`, `exists`, `all`, `size`, `elemMatch`, `between`, `isNull`, `isNotNull`, `not`, `mod`, `type`, `or()`.

Rendering chain: `LambdaQueryWrapper` → `Condition` list → `MongoBsonRenderer.render()` → MongoDB `Bson` filter. OR groups are split on sentinel `Condition` (field=null, operator=null), rendered independently, joined with `Filters.or()`.

**Important**: `$not` is NOT a valid top-level operator in MongoDB. Use field-level `$not` (for single-field negation like `NOT_LIKE`) or `$nor` (for negating an entire expression like `not(subWrapper)`).
