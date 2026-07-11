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

## Bug 2（已修复 ✅）：`ObjectId.isValid()` 启发式转换可能误判 String 类型的 `_id`

**文件:** `mongo-flex-core/src/main/java/com/github/eacryo/mongoflex/v2/SimpleMongoRepository.java`

- `convertIdIfNecessary`
- `buildFilterFromLambda`

**问题:** 原逻辑使用 `ObjectId.isValid()` 猜测一个 String 是否应转为 ObjectId，纯粹基于字符串格式。虽然 ULID（26 字符）和 UUID（36 字符）天然不会误判，但如果用户使用 `IdType.INPUT` + 自定义 `IdGenerator` 生成 24 位 hex 字符串作为 ID，会被错误转换为 ObjectId，导致 MongoDB 中 String 和 ObjectId 类型不匹配，查询静默返回空。

```java
// 原逻辑：凭字符串格式猜测
if (id instanceof String && ObjectId.isValid((String) id)) {
    return new ObjectId((String) id);
}
```

**修复方案:** 利用框架已有的 `IdType` 元数据做精确判断——仅在 `IdType.NONE` 下才转换，因为此时 MongoDB 生成的是 ObjectId，Java 侧以 hex String 存储，查询时必须转回。

| IdType | 存储的实际类型 | 是否需要 String→ObjectId |
|--------|-------------|------------------------|
| `NONE` | ObjectId（MongoDB 生成） | ✅ 是 |
| `ULID` | String | ❌ 否 |
| `UUID` | String | ❌ 否 |
| `INPUT` | 取决于生成器 | ❌ 否 |

```java
// 新增辅助方法
private boolean shouldConvertToObjectId() {
    Field idField = mongoMappingConvertor.getCollectionIdField(entityClass);
    if (idField == null) return false;
    return idField.getAnnotation(CollectionId.class).value() == IdType.NONE;
}

// 改后：仅 NONE 模式下才转换
private Object convertIdIfNecessary(ID id) {
    if (shouldConvertToObjectId()
        && id instanceof String
        && ObjectId.isValid((String) id)) {
        return new ObjectId((String) id);
    }
    return id;
}
```

**对比 Spring Data MongoDB:** Spring Data 以声明类型（`String` vs `ObjectId`）做判断；mongo-flex 不能照搬，因为 mongo-flex 的 `IdType.NONE` 模式下声明类型为 `String` 但实际存储为 `ObjectId`，必须结合 `IdType` 一起判断。这个方案比 Spring Data 的更贴合 mongo-flex 的模型。

**测试验证:** `ObjectIdConversionTest` — 4 个测试覆盖 `IdType.NONE` 转换和 `IdType.ULID` 不转换两个方向。

**测试文件:**
- `mongo-flex-test-common/.../bean/ObjectIdEntity.java`
- `mongo-flex-test-common/.../v2/ObjectIdEntityRepository.java`
- `mongo-flex-test-spring-boot3/.../v2/ObjectIdConversionTest.java`

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
