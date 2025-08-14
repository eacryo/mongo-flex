# mongo-flex

[中文](README-zh.md)

## what is it?
Mongo-flex is a toolkit of MongoDB for simplifying development.

## features
1. **Multi-tenancy support**: Easily manage multiple MongoDB databases or collections.
2. **Base repository**: Provides a base repository for common CRUD operations.
3. **Advance query or update**: use a Bean's non-null field to build query or update with non-null fields.

## dependecy
need JDK21 or later, and spring-boot 3.x. For JDK8 and springboot 2.x, we have further version to adaptation it.

## how to use it?
### 1. Add dependency
(TODO) Add the following dependency to your `pom.xml` file:
```xml
<dependency>
    <groupId>com.github.eacryo</groupId>
    <artifactId>mongo-flex</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
``` 
### 2. Enable multi-tenancy support as needed

application.properties:
```properties
mongo-flex.enable-multi-tenants=true
mongo-flex.tenants[0].name=testTenant
mongo-flex.tenants[0].uri=mongodb://sa:Aa123456@192.168.0.103:27017/test_db?authSource=admin
```
application.yml (recommend):
```yaml
mongo-flex:
  enable-multi-tenants: true
  tenants:
    - name: testTenant
      uri: mongodb://sa:Aa123456@192.168.0.103:27017/test_db?authSource=admin
``` 
MongoTemplateFactory use MDC (i.e. ThreadLocal) to get the information of the current tenant, please use it before calling
```java
 MDC.put(MongoFlexConstant.TENANT,"yourTenant");
```


This step is not necessary. If you don't do any configuration, then mongo-flex defaults to MongoTemplateFactory
Place a MongoTemplate with a tenant id of 'default'. You can do this directly through the MongoTemplateFactory instance
calling select() on to get it.
For more information, see the source code

### 3.extends BaseRepository
```java
@Component
public class CharacterRepository extends BaseRepository<Character>{
    //You can write your own method here or not
}
```

### 4.use with pleasure
use updateById(T entity) to update or findList(T entity) to query