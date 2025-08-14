# mongo-flex

## 这是什么?
Mongo-flex 是一套基于spring-data-mongodb二次封装的用于简化开发的框架.

## 功能

1. **多租户支持**: 目前支持不同的租户使用不同的数据库链接，未来会考虑增加对同库异表多租户的支持
2. **基础增删改查**: 继承BaseRepository即可进行增删改查.
3. **查询和更新**: 支持根据传入对象的非空字段进行查询或更新.
4. **最小化侵入性**: 基于spring-data-mongodb，支持原生MongoTemplate操作。

## 依赖
基于jdk21和springboot 3.4开发，未来将增加对于jdk8和springboot 2.0的支持

## 如何使用
### 1. 引入依赖
将此依赖加入到你的 `pom.xml` 文件:
```xml
<dependency>
    <groupId>com.github.eacryo</groupId>
    <artifactId>mongo-flex</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
``` 
### 2. 依据需要开启多租户支持

application.properties:
```properties
mongo-flex.enable-multi-tenants=true
mongo-flex.tenants[0].name=testTenant
mongo-flex.tenants[0].uri=mongodb://sa:Aa123456@192.168.0.103:27017/test_db?authSource=admin
```
application.yml (推荐):
```yaml
mongo-flex:
  enable-multi-tenants: true
  tenants:
    - name: testTenant
      uri: mongodb://sa:Aa123456@192.168.0.103:27017/test_db?authSource=admin
``` 
MongoTemplateFactory通过MDC(ThreadLocal)来获取当前租户的信息，请在调用前使用
```java
 MDC.put(MongoFlexConstant.TENANT,"yourTenant");
```
来置入租户信息。

这一步不是必须。如果不做任何配置，那么mongo-flex会默认在MongoTemplateFactory中
置入一个租户id为`default`的MongoTemplate。你可以直接通过在MongoTemplateFactory的实例
上调用select()来获取它。
更多的信息参见源码

### 3.继承 BaseRepository
```java
@Component
public class CharacterRepository extends BaseRepository<Character>{
    //这里可以写你自己的方法，也可以不写
}
```

### 4.愉快地使用
使用updateById(T entity)来更新或findList(T entity)来查询


