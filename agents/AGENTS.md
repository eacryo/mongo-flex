# AGENTS.md

This file provides guidance when working with code in this repository.

## Build & Test Commands

```bash
# Compile core module only
mvn compile -pl mongo-flex-core -am

# Run all SB3 tests (requires REMOTE_MONGO_URI env var)
mvn test -pl mongo-flex-test-spring-boot3

# Run all SB2 tests (requires REMOTE_MONGO_URI env var)
mvn test -pl mongo-flex-test-spring-boot2

# Run a single test class
mvn test -pl mongo-flex-test-spring-boot3 -Dtest=LambdaQueryWrapperOperatorTest

# Run a single test method
mvn test -pl mongo-flex-test-spring-boot3 -Dtest=LambdaQueryWrapperOperatorTest#testLike

# Clean install core to local repo (needed when test-common depends on updated core)
mvn clean install -pl mongo-flex-core -am -DskipTests
```

Tests connect to a remote MongoDB Atlas cluster via the `REMOTE_MONGO_URI` environment variable. There is no embedded MongoDB. **Tests are slow** (remote Atlas latency): a single test class takes 2–5 minutes; the full suite takes 10+ minutes. Always use a generous timeout when running test commands (≥ 600 seconds for Bash tool timeout).

**每次代码改动必须同时验证 SB2 和 SB3：** `mongo-flex-test-spring-boot2` 与 `mongo-flex-test-spring-boot3` 保持相同的测试集（test classes are mirrored between the two modules）。Every code change must pass the full test suites of **both** `mongo-flex-test-spring-boot2` and `mongo-flex-test-spring-boot3`. When adding or modifying a test, apply the same change to both modules (test code is Java 8 compatible across the whole reactor, so files can usually be copied verbatim).

## Java 8 Constraint

The project targets **Java 8 bytecode** (`<java.version>1.8</java.version>`). Do not use APIs introduced after JDK 8:
- ❌ `List.of()`, `Set.of()`, `Map.of()` — use `Arrays.asList()` or `new ArrayList<>()`
- ❌ `var` keyword
- ❌ `java.time.*` (unless the dependency already brings it in)
- ❌ Streams `.toList()` (Java 16) — use `.collect(Collectors.toList())`

## Code Comments — 代码注释规范

**All code comments MUST be written in both Chinese and English (中英双语).** Format:

```java
// English description / 中文描述
```

- Javadoc, line comments, and block comments all follow this rule.
- English first, then a space + `/` + space, then Chinese.
- Example: `// Find user by email / 根据邮箱查找用户`

## Commit Message Convention

Use **Conventional Commits** format, **single line only** (no body):

```
<type>: <description>
```

type: feat|fix|refactor|test|docs|chore|style|perf

Keep the description concise — one sentence, no bullet points, no body text.

Do **not** run `git commit`. Suggest the message and let the developer commit manually.

## Documentation Sync

When modifying `README.md`, apply the same changes to `README-zh.md` (Chinese translation). Both files must stay in sync.

## Architecture Overview

**mongo-flex** is a lightweight MongoDB toolkit for Spring Boot providing three query paths:

| Path | Mechanism | Use Case |
|---|---|---|
| Repository methods | `MongoRepository<T,ID>` interface (v2 package) | CRUD by ID, entity example, simple field=value |
| Lambda queries | `LambdaQueryWrapper<T>` + operators | Type-safe dynamic queries |
| Raw JSON | `@Find` / `@Count` / `@Delete` annotations | Complex/adhoc queries |

### Module Map

```
mongo-flex-core/          ← framework code
mongo-flex-test-common/   ← shared test entities & repository interfaces
mongo-flex-test-spring-boot3/  ← integration tests (active profile: v2)
mongo-flex-test-spring-boot2/  ← integration tests on Spring Boot 2.7 (mirrors SB3 test set)
```

### Key Design Decisions

- **Entity mapping**: `@CollectionId` (maps to `_id`), `@CollectionField("mongo_name")`, implicit `id`→`_id` mapping. Priority: `@CollectionId` > `@CollectionField` > field name heuristic.
- **ID generation**: Controlled by `IdType` enum — `OBJECT_ID` (MongoDB ObjectId, stored as hex String), `ULID` (26-char sortable), `UUID` (v4), `INPUT` (custom `IdGenerator`).
- **ObjectId conversion**: Only converts String→ObjectId when `IdType.OBJECT_ID` (MongoDB generates ObjectId but Java stores hex String). Other modes store native String. Use `shouldConvertToObjectId()` — never raw `ObjectId.isValid()`.
- **Repository proxies**: `@MRepository` interfaces get JDK proxy via `RepositoryFactoryBean`. `@Find`/`@Count`/`@Delete` methods use `JsonTemplateParser`; `MongoRepository` methods delegated to `SimpleMongoRepository`.
- **Field name resolution**: `ReflectUtil.getFieldNameFromLambda()` extracts field name from `SFunction` via `SerializedLambda`; `MongoMappingConvertor.resolveMongoFieldPath()` maps Java→MongoDB field paths using cached `ClassFieldMetaData` — accepts a single field name (`"name"`) or a dot-separated nested path (`"address.city"`), mapping each segment independently (`@CollectionField`/`id`→`_id`) and traversing `List` segments into their generic element type.
- **Nested field queries**: `FieldPath.of(A::getB).then(B::getC)` (or `FieldPath.of(A::getB, B::getC)`) builds a type-safe nested path for `LambdaQueryWrapper` operator overloads, rendered as dot notation. `@Find`/`@Count`/`@Delete` support raw JSON dot keys natively. Entity-based (`*ByEntity`) queries are single-layer: nested objects match as exact subdocuments, never flattened.

### Lambda Query Operators

Full operator list in `LambdaQueryWrapper`: `eq`, `ne`, `gt`, `lt`, `gte`, `lte`, `regex`, `like`, `notLike`, `in`, `nin`, `exists`, `all`, `size`, `elemMatch`, `between`, `isNull`, `isNotNull`, `not`, `mod`, `type`, `or()`.

Rendering chain: `LambdaQueryWrapper` → `Condition` list → `MongoBsonRenderer.render()` → MongoDB `Bson` filter. OR groups are split on sentinel `Condition` (field=null, operator=null), rendered independently, joined with `Filters.or()`.

**Important**: `$not` is NOT a valid top-level operator in MongoDB. Use field-level `$not` (for single-field negation like `NOT_LIKE`) or `$nor` (for negating an entire expression like `not(subWrapper)`).

## Additional Project Files

- `ISSUES.md` — known issues and bug tracker
- `TODO.md` — feature roadmap and code-review bug list (BUG-1 through BUG-26)
- `ARCHITECTURE_REFACTOR.md` — planned architecture refactoring phases
