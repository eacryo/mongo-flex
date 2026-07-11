# Simple Mongo Repository Issue

## Bug 1（已修复 ✅）：`@CollectionId` 注解在非 `id` 命名字段上不映射到 `_id`

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

```java
// 修复后（ClassFieldMetaData.java）
if (field.isAnnotationPresent(CollectionId.class)) {
    // @CollectionId 强制映射到 _id，与 Spring Data MongoDB 的 @Id 行为一致
    mongoFieldName = MONGO_ID_FIELD;
} else if (field.isAnnotationPresent(CollectionField.class)) {
    mongoFieldName = field.getAnnotation(CollectionField.class).value();
} else if (JAVA_ID_FIELD.equals(field.getName())) {
    // 隐式 id → _id 映射（未被 @CollectionId 覆盖时生效）
    mongoFieldName = MONGO_ID_FIELD;
} else {
    mongoFieldName = field.getName();
}
```

**测试验证:** 新增 `CustomIdEntity`（字段名 `userId` 标注 `@CollectionId`），覆盖 insert → findById → updateById → findOneByEntity → deleteById 完整链路，所有 5 个测试通过。

**测试文件:**
- `mongo-flex-test-common/.../bean/CustomIdEntity.java`
- `mongo-flex-test-common/.../v2/CustomIdEntityRepository.java`
- `mongo-flex-test-spring-boot3/.../v2/CustomIdFieldTest.java`

---

## Bug 2（中等）：`ObjectId.isValid()` 启发式转换可能误判 String 类型的 `_id`

**文件:** `mongo-flex-core/src/main/java/com/github/eacryo/mongoflex/v2/SimpleMongoRepository.java`

- `convertIdIfNecessary`:355
- `buildFilterFromLambda`:380

**问题:** 两处方法均使用 `ObjectId.isValid()` 判断一个 String 是否应该被转换为 ObjectId：

```java
if (id instanceof String && ObjectId.isValid((String) id)) {
    return new ObjectId((String) id);
}
```

如果用户有意使用 24 位 hex 字符串（如某些 hash 值、自定义 ID 格式）作为 String 类型的 `_id`，该方法会错误地将其转换为 `ObjectId`。MongoDB 中 `ObjectId("507f1f77bcf86cd799439011")` 与 `"507f1f77bcf86cd799439011"`（String）是两种不同类型，查询将静默返回空结果。

**当前为何未暴露:** 常见 ID 格式不易与 ObjectId 的 24 位 hex 格式碰撞——ULID 为 26 字符，UUID 带连字符为 36 字符。但这属于巧合，不是设计保证。

**影响方法:** `findById`、`deleteById`、`findOneByEntity`、`countByEntity`、`deleteByEntity`，以及所有 `SFunction` 版本的单字段查询（`findOne`、`count`、`update`、`delete`）。

**修复:** 改为从 `ClassFieldMetaData` 中读取 ID 字段的实际 Java 类型来决定是否需要转换，而非仅凭字符串格式猜测。

---

## 问题 3（低，代码质量）：`doc.remove("_id")` + `$set` 模式重复 4 次

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

## 问题 4（低，代码质量）：`fillDate` 异常捕获过于宽泛

**文件:** `mongo-flex-core/src/main/java/com/github/eacryo/mongoflex/v2/SimpleMongoRepository.java:348`

```java
} catch (Exception e){
    throw new RuntimeException("Failed to set date fields", e);
}
```

`catch (Exception e)` 覆盖范围过宽，连 NPE、`SecurityException` 等也会被吞掉并重新包装。虽然原始异常作为 cause 被保留不影响排查，但失去了异常类型的区分度。

**修复:** 只 catch `IllegalAccessException`；`DateValueGenerator.generateCurrentDate()` 的 `IllegalArgumentException`（不支持的日期类型）应让其自然传播，调用方有更明确的异常类型可以处理。
