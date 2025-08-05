# mongo-flex

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
    <version>0.0.1-SNAPSHOT</version>
</dependency>
``` 
### 2. Configure MongoDB
In your `application.properties` or `application.yml`, configure the MongoDB connection settings:

mongo-flex supports multi-tenancy, currently supporting 
1. different MongoDB databases running on different servers or the same server 
2. the same database but different tables, or called collections in nosql, distinguished by table name prefixes.Both modes can run at same time.

We strongly recommend **against** using 1 and 2 in the same database. This will reduce readability.

application.properties:
```properties
spring.data.mongodb.tenants[0].name=testTenant
spring.data.mongodb.tenants[0].uri=mongodb://root:Aa123456@localhost:27017/your_db?authSource=admin

```
application.yml (recommended):
```yaml
spring:
  data:
    mongodb:
      tenants:
          - name : testTenant
            uri: mongodb://root:Aa123456@localhost:27017/your_db?authSource=admin
```     
