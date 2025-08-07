package com.github.eacryo.mongoflex.repository;


import com.github.eacryo.mongoflex.config.MongoTemplateFactory;
import com.github.eacryo.mongoflex.entity.BaseEntity;
import com.github.eacryo.mongoflex.entity.PageDTO;
import com.github.eacryo.mongoflex.util.CollectionNameUtil;
import com.github.eacryo.mongoflex.util.ReflectUtil;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ResolvableType;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

//@Component
//如果标了Component会实例化两次（父类本身和子类，第一次触发父类实例化的时候拿不到泛型信息就会报错）
//这个类应当被设置为抽象类，因为它本身不会被使用，使用的是它的子类
public class BaseRepository<T> implements IBaseRepository<T> {

    //TODO:可以考虑初始化时把DTO对象信息缓存起来，这样不用每次都通过反射获取对象信息了。

    //这里private也可以，不需要protected
    private Class<T> entityClass;

    private List<Field> entityFields;

    private Field idField;

    //TODO:考虑移除对同库多租户的支持
    private String collectionName;


    //TODO:这里会报错，但是使用起来没问题
    @Autowired
    private MongoTemplateFactory mongoTemplateFactory;

    @Autowired
    private CollectionNameUtil collectionNameUtil;


    /**
     * @param entity
     * @return 主键id
     */
    @Override
    public T save(T entity) {
        return mongoTemplateFactory.select().save(entity, collectionName);
    }

    @Override
    public boolean insertAll(Collection<T> entities) {
        if (CollectionUtils.isEmpty(entities)) {
            Assert.isTrue(false, "插入的集合不能为空");
        }
        mongoTemplateFactory.select().insert(entities, collectionName);
        return true;
    }

    @Override
    public T findById(String id) {
        return mongoTemplateFactory.select().findById(id, entityClass, collectionName);
    }

    @Override
    public T findOneByField(String fieldName, Object value) {
        Query query = new Query();
        query.addCriteria(Criteria.where(fieldName).is(value));
        return mongoTemplateFactory.select().findOne(query, entityClass, collectionName);
    }

    @Override
    public List<T> findListByField(String fieldName, Object value, String... orderBy) {
        Query query = new Query();
        query.addCriteria(Criteria.where(fieldName).is(value));
        for (String order : orderBy) {
            query.with(Sort.by(Sort.Direction.ASC, order));
        }
        return mongoTemplateFactory.select().find(query, entityClass, collectionName);
    }

    @Override
    public List<T> findListByFieldDesc(String fieldName, Object value, String... orderBy) {
        Query query = new Query();
        query.addCriteria(Criteria.where(fieldName).is(value));
        for (String order : orderBy) {
            query.with(Sort.by(Sort.Direction.DESC, order));
        }
        return mongoTemplateFactory.select().find(query, entityClass, collectionName);
    }

    @Override
    public List<T> findList(Query query) {
        return mongoTemplateFactory.select().find(query, entityClass, collectionName);
    }

    @Override
    public List<T> findList(T entity) {
        Query query = new Query();
        for (Field field : entityFields) {
            field.setAccessible(true);
            try {
                Object value = field.get(entity);
                if (value != null) {
                    query.addCriteria(Criteria.where(field.getName()).is(value));
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("无法访问字段: " + field.getName(), e);
            }
        }
        return mongoTemplateFactory.select().find(query,entityClass, collectionName);
    }


    /**
     * @param orderBy 默认按照升序排列
     * @return
     */
    @Override
    public List<T> findAll(String... orderBy) {
        Query query = new Query();
        for (String order : orderBy) {
            query.with(Sort.by(Sort.Direction.ASC, order));
        }
        return mongoTemplateFactory.select().find(query, entityClass, collectionName);
    }

    @Override
    public List<T> findAllDesc(String... orderBy) {
        Query query = new Query();
        for (String order : orderBy) {
            query.with(Sort.by(Sort.Direction.DESC, order));
        }
        return mongoTemplateFactory.select().find(query, entityClass, collectionName);
    }

    @Override
    public List<String> findDistinct(String fieldName) {
        Query query = new Query();
        return mongoTemplateFactory.select().findDistinct(query, fieldName, collectionName, entityClass, String.class);
    }

    /**
     * 传入对象的值如果非空那么就更新该字段
     *
     * @param entity
     * @return
     */
    @Override
    public boolean updateById(T entity) {
        if (entity == null || getIdFieldValue(entity) == null) {
            throw new IllegalArgumentException("更新的对象或ID不能为空");
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(getIdFieldValue(entity)));
        Update update = new Update();
        for (Field field : entityFields) {
            field.setAccessible(true);
            try {
                Object value = field.get(entity);
                if (value != null) {
                    update.set(field.getName(), value);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("无法访问字段: " + field.getName(), e);
            }
        }
        mongoTemplateFactory.select().updateFirst(query, update, entityClass, collectionName);
        return true;
    }


    @Override
    public PageDTO<T> findPage(PageDTO<T> pageDTO) {
        return findPage(new Criteria(), pageDTO);
    }

    @Override
    public PageDTO<T> findPage(Criteria criteria, PageDTO<T> pageDTO) {
        Query query = new Query();
        query.addCriteria(criteria);
        long total = mongoTemplateFactory.select().count(query, entityClass, collectionName);
        PageDTO<T> result = new PageDTO<>();
        BeanUtils.copyProperties(pageDTO, result);
        if (total == 0) {
            result.setPage(0L);
            return result;
        }
        long totalPage = (total + pageDTO.getPageSize() - 1) / pageDTO.getPageSize();
        result.setPageSize(pageDTO.getPageSize());
        result.setPage(pageDTO.getPage());
        result.setTotal(total);
        result.setTotalPage(totalPage);
        if (!CollectionUtils.isEmpty(pageDTO.getOrderBy())) {
            query.with(Sort.by(pageDTO.isOrderByAsc() ? Sort.Direction.ASC : Sort.Direction.DESC,
                    pageDTO.getOrderBy().toArray(new String[0])));
        }
        query.skip((pageDTO.getPage() - 1) * pageDTO.getPageSize());
        query.limit(pageDTO.getPageSize().intValue());
        result.setRecords(mongoTemplateFactory.select().find(query, entityClass, collectionName));
        return result;
    }


    @SuppressWarnings("unchecked")
    @PostConstruct
    private void InitBaseRepository() {
        // 使用反射获取泛型信息
        Type genericSuperclass = getClass().getGenericSuperclass();
        //jdk21的语法，instanceof的同时赋值
        if (genericSuperclass instanceof ParameterizedType parameterizedType) {
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (actualTypeArguments.length > 0 && actualTypeArguments[0] instanceof Class) {
                this.entityClass = (Class<T>) actualTypeArguments[0];
            } else {
                this.entityClass = null;
                throw new IllegalArgumentException("无法解析泛型类型参数");
            }
        } else {
            // 如果直接获取失败，尝试使用Spring的ResolvableType作为备选方案
            this.entityClass = (Class<T>) ResolvableType
                    .forClass(getClass())
                    .getSuperType()
                    .getGeneric(0)
                    .resolve();
        }

        if (entityClass == null) {
            throw new IllegalArgumentException("无法解析泛型类型，请确保正确继承BaseRepository<T>");
        }

        this.entityFields = ReflectUtil.getAllFieldsIncludingInherited(entityClass);

        this.idField = ReflectUtil.getIdField(this.entityFields);
        idField.setAccessible(true);

        this.collectionName = collectionNameUtil.getByClass(entityClass);


    }

    /**
     * 把checked Exception转化为runtime exception，面得每次都要try catch
     * @param entity
     * @return
     */
    private Object getIdFieldValue(T entity) {
        this.idField.setAccessible(true);
        try {
            return idField.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


}
