# Entity Inheritance Issues

## 背景

mongo-flex 当前没有类型鉴别机制（如 Spring Data MongoDB 的 `_class` 字段）。`MongoMappingConvertor.read()` 永远实例化传入的 `targetClass`，不会根据文档内容选择子类。这导致以下四个问题。

以 `Character`（父）和 `LiyueCharacter extends Character`（子）为例，假设两者存储在同一个 `character` 集合中。

---

## 问题 1：反序列化类型丢失，子类字段被丢弃（✅ 设计如此，不需要解决）

**设计原则（与 MyBatis-Plus 一致）：Repository 的类型参数决定了读写的数据边界。**

- `CharacterRepository` → 只负责 `Character` 的字段，返回 `Character` 实例
- `LiyueCharacterRepository` → 负责 `Character` + `LiyueCharacter` 的字段，返回 `LiyueCharacter` 实例

如果需要完整读写子类字段，应当定义专用的 `LiyueCharacterRepository extends MongoRepository<LiyueCharacter, String>`，而不是期望通过父类 Repository 拿到子类实例。这一设计选择避免了隐式类型转换的"魔法"，调用方明确知道拿到的就是声明的类型，不需要 `instanceof` 防御。

> Spring Data MongoDB 默认通过 `_class` 字段自动实例化子类，是一种不同的设计选择（隐式多态），但会增加使用复杂度和潜在的类型安全问题。mongo-flex 选择 MyBatis-Plus 式的显式类型绑定。

**根因：** `MongoMappingConvertor.read(doc, targetClass)` 中 `targetClass` 来自仓库接口声明的编译期类型（`CharacterRepository<Character>` → `Character.class`），永远实例化 `Character`。MongoDB 文档中子类特有字段（如 `title`、`is_adeptus`）在反序列化时被静默忽略。

```java
// INSERT — ✅ 写入正确
LiyueCharacter hutao = new LiyueCharacter();
hutao.setVision("Pyro");
hutao.setTitle("雪霁梅香");          // 子类字段
hutao.setAffiliation("往生堂");       // 子类字段
hutao.setMoraAmount(999_999_999L);
repo.insert(hutao);                 // 运行时类型正确，全字段序列化

// MongoDB 文档：
// { "_id": "...", "vision": "Pyro", "title": "雪霁梅香", "affiliation": "往生堂", ... }

// READ — ❌ 子类字段丢失
Character c = repo.findById(hutao.getId());
c.getVision();       // "Pyro" ✅
c.getClass();        // Character ❌ 不是 LiyueCharacter
// title、affiliation、moraAmount 存在于 MongoDB 文档中，但反序列化时被忽略
```

**影响：**
- INSERT 和 READ 的类型不对称：存入的是 `LiyueCharacter`，读出来的是 `Character`
- 子类独有数据永久丢失（除非改用专用子类 Repository 读取）

---

## 问题 2：无法通过 `instanceof` 判断实际类型

**根因：** `MongoMappingConvertor.read()` 始终通过 `targetClass.getDeclaredConstructor().newInstance()` 创建实例，无论如何都是父类对象。

```java
// MongoDB 里存的是 LiyueCharacter
LiyueCharacter hutao = new LiyueCharacter();
hutao.setTitle("雪霁梅香");
repo.insert(hutao);

// 读回来
Character c = repo.findById(hutao.getId());

if (c instanceof LiyueCharacter) {   // ❌ 永远 false！
    LiyueCharacter lc = (LiyueCharacter) c;
    lc.getTitle();                    // 这段代码永远不会执行
}
```

**影响：**
- 即使将来增加了类型标识字段（如 `_class`），也无法通过 `instanceof` 做类型判断
- 同一集合无法实现多态查询

---

## 问题 3：Lambda 查询子类字段时，`@CollectionField` 映射失效（✅ 已解决）

### 设计分析

Repository 的类型参数和 Lambda 引用的声明类是**两件独立的事情**：

| 决定什么 | 由谁控制 | 含义 |
|---|---|---|
| 返回什么类型的对象 | `Repository<T>` 的 `T` | 读出来的对象是 `Character` 还是 `LiyueCharacter` |
| 用什么字段做查询条件 | Lambda 引用的声明类 | 字段的 `@CollectionField` 映射用哪个类的元数据解析 |

两个维度可以自由组合，三种场景都合理：

```java
// ===== 场景 A：父类 Repository + 子类字段过滤 =====
// "我只想要 Character 的结果，但我想筛出仙人的角色"
CharacterRepository repo;
LambdaQueryWrapper<Character> w = new LambdaQueryWrapper<>(Character.class);
w.eq(LiyueCharacter::getIsAdeptus, true);   // 过滤条件：is_adeptus = true
List<Character> result = repo.findList(w);   // 返回：Character 列表（不含子类字段）

// ===== 场景 B：子类 Repository + 父类字段过滤 =====
// "我要完整的 LiyueCharacter，但用通用的 vision 字段过滤"
LiyueCharacterRepository repo;
LambdaQueryWrapper<LiyueCharacter> w = new LambdaQueryWrapper<>(LiyueCharacter.class);
w.eq(Character::getVision, "Pyro");         // 过滤条件：vision = "Pyro"
List<LiyueCharacter> result = repo.findList(w);  // 返回：完整的 LiyueCharacter 列表

// ===== 场景 C：子类 Repository + 子类字段过滤 =====
// "我要完整的 LiyueCharacter，用子类特有字段过滤"
LiyueCharacterRepository repo;
LambdaQueryWrapper<LiyueCharacter> w = new LambdaQueryWrapper<>(LiyueCharacter.class);
w.eq(LiyueCharacter::getIsAdeptus, true);   // 过滤条件：is_adeptus = true
List<LiyueCharacter> result = repo.findList(w);  // 返回：完整的 LiyueCharacter 列表
```

场景 B 和 C 本来就能正确工作（`entityClass = LiyueCharacter.class`，`ClassFieldMetaData` 遍历完整层级）。**真正有问题的是场景 A**——Lambda 引用的是子类字段，但 `entityClass` 是父类。

### 根因

`MongoBsonRenderer` 调用 `convertor.resolveMongoFieldName(entityClass, javaFieldName)`，用 `entityClass`（来自 `LambdaQueryWrapper.getEntityClass()`）去解析字段映射。对于场景 A，`entityClass = Character.class`，`ClassFieldMetaData(Character.class)` 不包含 `LiyueCharacter` 的 `isAdeptus` 字段及其 `@CollectionField("is_adeptus")` 映射，导致：

```java
// resolveMongoFieldName(Character.class, "isAdeptus")
//   → Character 的 ClassFieldMetaData 里没有 "isAdeptus"
//   → 直接返回 "isAdeptus" 作为 MongoDB 字段名（fallback 为 Java 字段名）
//   → 生成的查询：{ "isAdeptus": true }
//   → 实际数据库字段名：{ "is_adeptus": true }  ← @CollectionField 映射被忽略
//   → 查询静默返回空
```

### 影响

- 通过父类 Repository + 子类 Lambda 引用查询时，子类字段的 `@CollectionField` 映射不生效
- 即使 Java 字段名碰巧和 MongoDB 字段名一致（无 `@CollectionField` 覆盖），虽然能查到，但这只是巧合

### 解决方案

Lambda 表达式本身携带了字段声明类的信息——`SerializedLambda.getImplClass()`。`LiyueCharacter::getIsAdeptus` 返回 `LiyueCharacter.class`。只需让 `MongoBsonRenderer` 用每个字段自己的 `implClass` 去解析映射，而不是统一用 `entityClass`：

1. `ReflectUtil` 新增 `getImplClassFromLambda(SFunction)` —— 从 lambda 提取声明类
2. `Condition` 增加 `implClass` 字段 —— 记录每个条件的字段声明类
3. `LambdaQueryWrapper` —— 每个条件构造时传入 `implClass`
4. `MongoBsonRenderer.renderGroup()` —— 字段名解析用 `c.implClass() ?? entityClass`

---

## 问题 4：`@Mql` 查询同样无法反序列化为子类（✅ 设计如此，不需要解决）

**根因：** `FindExecutor` / `FindOneExecutor` 通过 `method.getReturnType()` 或 `method.getGenericReturnType()` 获取目标类型，即方法签名中声明的返回类型。

```java
// CharacterRepository 中声明：
@Mql("db.getCollection('character').find({})")
List<Character> findAll();

// FindExecutor 反序列化时：
// listElementClass = Character.class（从方法签名的泛型参数获取）
// mongoMappingConvertor.read(doc, Character.class)
// → 永远返回 Character 实例，子类字段丢失
```

即使 MongoDB 文档中包含完整的 `LiyueCharacter` 数据，`@Mql` 查询返回的也是按照方法声明类型反序列化的结果。

**影响：**
- `@Mql` 和 `LambdaQueryWrapper` 两条查询路径存在相同的问题
- 如果方法返回 `List<Object>` 或 `List`，虽然会走到 fallback 逻辑（`FindExecutor` 中 `Object.class`），但同样不会用到子类

---

## 总结

| 问题 | 关键类/方法 | 根本原因 |
|---|---|---|
| 1. 子类字段丢失 | `MongoMappingConvertor.read()` | 永远实例化编译期类型，不读运行时类型 |
| 2. instanceof 失效 | `MongoMappingConvertor.read()` | `newInstance()` 创建的是父类 |
| 3. 子类字段映射断裂 ✅ | `MongoBsonRenderer.renderGroup()` → `resolveMongoFieldName()` | ~~用父类的 `ClassFieldMetaData` 解析子类字段~~ 已修复：优先用 lambda 的 `implClass` 解析 |
| 4. @Mql 同样问题 ✅ | `FindExecutor` / `FindOneExecutor` | ~~用方法声明返回类型反序列化~~ 设计如此：返回类型决定数据边界，需要子类就声明子类返回类型 |

**核心矛盾：** INSERT 用运行时类型（能正确处理子类），READ 用编译期类型（丢失子类信息）。要实现多态支持，需要引入类型鉴别机制，让 READ 也能感知原始类型。
