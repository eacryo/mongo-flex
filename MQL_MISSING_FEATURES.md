# @Mql 缺失功能清单

本文档列出 `@Mql` 注解查询路径相对于 `LambdaQueryWrapper` 缺失的功能，按优先级排序，依次实现。

---

## 背景

`QueryParser` 当前正则表达式：

```java
Pattern.compile("^db\\.getCollection\\('(.*?)'\\)\\." +
    "(find|findOne|insertOne|updateOne|updateMany|deleteOne|deleteMany|count|aggregate)\\((.*?)\\)$");
```

**核心问题**：只捕获一个括号参数（filter 文档），不支持链式调用和附加选项。

---

## 1. 排序（Sort）

**状态**：完全缺失

**LambdaQueryWrapper 对应**：
- `orderByAsc(SFunction<T, ?> field)` — 升序
- `orderByDesc(SFunction<T, ?> field)` — 降序

**目标 MQL 语法**（参考 MongoDB shell）：
```
db.getCollection('users').find({}).sort({ name: 1, age: -1 })
```

**实现要点**：
- QueryParser 需要支持解析 `.sort({...})` 链式调用
- sort 文档格式：`{ fieldName: 1 | -1 }`（1 升序，-1 降序）
- sort 为可选参数，不影响现有不带 sort 的查询

---

## 2. 分页（Pagination / Skip & Limit）

**状态**：完全缺失

**LambdaQueryWrapper 对应**：
- 通过 `PageQuery` + Repository 方法隐式处理分页
- 无直接暴露的 skip/limit 方法，但 Repository 层支持分页

**目标 MQL 语法**：
```
db.getCollection('users').find({}).limit(10)
db.getCollection('users').find({}).skip(20).limit(10)
db.getCollection('users').find({}).sort({ name: 1 }).skip(20).limit(10)
```

**实现要点**：
- 支持 `.limit(n)` 和 `.skip(n)` 链式调用
- 与 sort 可任意组合，链式顺序灵活
- limit/skip 均为可选参数

---

## 3. 投影（Projection）

**状态**：完全缺失

**LambdaQueryWrapper 对应**：
- 无直接对应（LambdaQueryWrapper 主要用于查询条件，投影通过 Repository 方法上的注解或其他方式处理）

**目标 MQL 语法**：
```
db.getCollection('users').find({}, { name: 1, email: 1, _id: 0 })
```

**实现要点**：
- `find` / `findOne` 的第二个参数是投影文档
- 1 表示包含，0 表示排除（不能混用，`_id` 除外）
- projection 为可选参数

---

## 4. like / notLike 便捷操作符

**状态**：需手写 `$regex`，无通配符自动转换

**LambdaQueryWrapper 对应**：
- `like(field, pattern)` — `*` 和 `%` 自动转为 `.*`，内部用 `$regex` 实现
- `notLike(field, pattern)` — 同上 + `$not` 包裹

**当前 @Mql 中的替代方案**（手写正则）：
```json
// like("name", "*zhang*") 等价于：
{ "name": { "$regex": ".*zhang.*" } }

// notLike("name", "*zhang*") 等价于：
{ "name": { "$not": { "$regex": ".*zhang.*" } } }
```

**可选实现方案**：
- 方案 A：在 QueryParser 层面不支持，文档里说明等价写法即可
- 方案 B：新增自定义 MQL 函数语法，如 `{ name: { $like: "*zhang*" } }`，在渲染层转换
- **建议**：方案 A，因为 `$regex` 已经是标准 MongoDB 语法，`like`/`notLike` 只是 Lambda 查询的类型安全快捷方式，对 MQL 这种直接写 JSON 的路径价值不大

---

## 5. between 便捷操作符

**状态**：需手写 `$gte` + `$lte`

**LambdaQueryWrapper 对应**：
- `between(field, start, end)` — 内部生成 `{ field: { $gte: start, $lte: end } }`

**当前 @Mql 中的替代方案**：
```json
// between("age", 18, 30) 等价于：
{ "age": { "$gte": 18, "$lte": 30 } }
```

**建议**：同 like/notLike，写文档说明等价写法即可，不值得新增自定义操作符。

---

## 6. Upsert 支持

**状态**：完全缺失

**目标 MQL 语法**（参考 MongoDB shell）：
```
db.getCollection('users').updateOne({ name: 'zhang' }, { $set: { age: 30 } }, { upsert: true })
db.getCollection('users').updateMany({ status: 'active' }, { $inc: { count: 1 } }, { upsert: true })
```

**实现要点**：
- `updateOne` / `updateMany` 的第三个参数是 options 文档 `{ upsert: true, ... }`
- 需要区分第二个参数（update 操作）和第三个参数（options）
- 当前 QueryParser 只捕获一个括号参数，需要扩展到三个
- options 为可选参数，不影响现有不带 options 的 update 调用

---

## 7. 多参数命令支持（修复 QueryParser）

**状态**：QueryParser 正则只能解析一个括号参数

**当前问题**：
- `find(query)` ✅
- `find(query, projection)` ❌ 无法解析第二个参数
- `updateOne(filter, update)` ❌ 当前把整个 `{...}, {...}` 当作一个参数
- `updateOne(filter, update, options)` ❌

**实现要点**：
- 重写 QueryParser 解析逻辑，支持多参数
- 需要考虑参数内的嵌套对象 `{ }` 和数组 `[ ]` 的正确匹配
- 简单正则已不足以处理，建议改为逐字符扫描的括号匹配解析

---

## 8. sort 的字段名映射

**状态**：待确定

**说明**：sort 文档中的字段名应该用 MongoDB 字段名还是 Java 字段名？

- LambdaQueryWrapper 通过 `SFunction` 自动映射 Java 字段名 → MongoDB 字段名
- @Mql 中写的是原始 MongoDB shell 语法，用户可能需要直接写 MongoDB 字段名
- 如果用户有 `@CollectionField("mongo_name")` 映射，sort 中写 `mongo_name` 还是 `javaName`？

**建议**：@Mql 保持原始 MongoDB 语法，字段名使用 MongoDB 名称（与 MongoDB shell 一致）。如有需要后续可扩展 `$sort: { java_field: 1 }` 自动映射。

---

## 实现顺序建议

| 优先级 | 功能 | 理由 |
|--------|------|------|
| P0 | 多参数命令支持 | 是所有后续功能的基础，当前解析器不修复，其他功能无法实现 |
| P1 | 分页 (skip/limit) | 高频使用，没有分页查询基本不可用 |
| P2 | 排序 (sort) | 高频使用 |
| P3 | 投影 (projection) | 中频使用，优化查询性能 |
| P4 | Upsert | update 场景常用 |
| P5 | like/notLike/between | 低优先级，已有等价写法，仅便捷性提升 |

---

## 参考资料

- [MongoDB Query Operators](https://www.mongodb.com/docs/manual/reference/operator/query/)
- [MongoDB Cursor Methods](https://www.mongodb.com/docs/manual/reference/method/js-cursor/)
- `LambdaQueryWrapper.java` — 操作符定义
- `QueryParser.java` — 当前 MQL 解析实现
- `MongoBsonRenderer.java` — Lambda 查询渲染为 Bson 的逻辑
