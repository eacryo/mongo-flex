# TODO

待实现功能清单。

---

## 待实现

### PageDTO 分页 ✅ 已完成

分页查询逻辑已实现。`MongoRepository` 新增 `findPage`（LambdaQueryWrapper 条件分页）和 `findPageByEntity`（实体条件分页），底层基于 MongoDB 的 `skip()` / `limit()` + `countDocuments()`。

**涉及文件:**
- `mongo-flex-core/.../entity/PageDTO.java`
- `mongo-flex-core/.../v2/MongoRepository.java`
- `mongo-flex-core/.../v2/SimpleMongoRepository.java`

### 排序（sort / orderBy）🔄 部分完成

LambdaQueryWrapper 已支持 `orderByAsc/Desc(SFunction)` 类型安全排序（含 @CollectionField 映射），Repository 的 `findPage` 已集成。`@Mql` 的 `FindExecutor` 仍不支持 sort/projection 参数。

**涉及文件:**
- `mongo-flex-core/.../lambda/LambdaQueryWrapper.java` ✅
- `mongo-flex-core/.../v2/SimpleMongoRepository.java` ✅
- `mongo-flex-core/.../strategy/FindExecutor.java` ❌ 待实现

### 投影 / 字段筛选（projection）

三种方式都返回完整文档，无法指定只返回部分字段。MongoDB 的 `find()` 支持第二个参数做 projection，但现有实现均未利用。

**涉及文件:**
- `mongo-flex-core/.../strategy/FindExecutor.java`
- `mongo-flex-core/.../strategy/FindOneExecutor.java`
- `mongo-flex-core/.../v2/SimpleMongoRepository.java`

### @Mql 未实现的 Executor（5/9 已解析命令无执行器）

QueryParser 正则已能解析 9 种 MongoDB shell 命令，但仅 4 个有 Executor（find、findOne、count、deleteOne）。以下 5 个命令解析后直接抛 `UnsupportedOperationException`：

| 命令 | 说明 |
|------|------|
| `insertOne` | 单条插入 |
| `updateOne` | 单条更新 |
| `updateMany` | 批量更新 |
| `deleteMany` | 批量删除 |
| `aggregate` | 聚合管道 |

还有 `distinct`、`bulkWrite` 等命令甚至连正则都没覆盖。

**涉及文件:**
- `mongo-flex-core/.../v2/QueryParser.java`
- `mongo-flex-core/.../strategy/`（需新增 5 个 Executor）

### @Mql 不接受 LambdaQueryWrapper 参数

@Mql 方法的参数只支持 `@Param` 标注的基本类型（值通过 `.toString()` 拼入 MQL 字符串），无法接受 `LambdaQueryWrapper` 作为查询条件。两种类型安全能力不能组合。

**涉及文件:**
- `mongo-flex-core/.../v2/MyRepositoryProxyHandler.java`

### LambdaQueryWrapper 缺少查询修饰字段

当前 `LambdaQueryWrapper` 只承载过滤条件（15 种操作符 + OR），缺少 sort、limit、skip、projection 等查询修饰字段。需要扩展 wrapper 使其成为完整的查询载体。

**涉及文件:**
- `mongo-flex-core/.../lambda/LambdaQueryWrapper.java`
- `mongo-flex-core/.../lambda/MongoBsonRenderer.java`

### LambdaQueryWrapper 缺少 MongoDB 操作符

当前支持的 15 种操作符中缺少以下常见 MongoDB 操作符：

| 操作符 | 说明 |
|------|------|
| `not` / `nor` | 逻辑取反 / 全不匹配 |
| `near` / `nearSphere` | 地理位置查询 |
| `text` | 全文搜索 |
| `mod` / `type` / `expr` | 取模 / 类型匹配 / 表达式 |

**涉及文件:**
- `mongo-flex-core/.../lambda/LambdaQueryWrapper.java`
- `mongo-flex-core/.../lambda/Operator.java`
- `mongo-flex-core/.../lambda/MongoBsonRenderer.java`

### upsert 支持

update 相关方法（MongoRepository 的 `update`、`updateById`、`updateAll`，以及将来 @Mql 的 updateExecutor）均不支持 upsert 选项。需增加 `upsert` 参数或在方法签名中提供 upsert 版本。

**涉及文件:**
- `mongo-flex-core/.../v2/SimpleMongoRepository.java`
- `mongo-flex-core/.../v2/MongoRepository.java`

### 批量插入（insertMany）

当前仅支持单条 `insert(entity)`，无批量插入方法。MongoDB 的 `insertMany` 相比逐条 `insert` 有显著的性能优势。

**涉及文件:**
- `mongo-flex-core/.../v2/MongoRepository.java`
- `mongo-flex-core/.../v2/SimpleMongoRepository.java`

### 更新 README.md / README-zh.md

待上述功能实现到一定程度后，同步更新 README 文档中的 Quick Start 示例。缺失内容包括：LambdaQueryWrapper OR 用法、@Mql 可用命令范围说明、分页/排序/投影示例等。

**涉及文件:**
- `README.md`
- `README-zh.md`

