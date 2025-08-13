package com.github.eacryo.mongoflex.repository;


import com.github.eacryo.mongoflex.entity.PageDTO;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Collection;
import java.util.List;

//@Component
public interface IBaseRepository<T> {
    T save(T entity);

    boolean insertAll(Collection<T> entities);

    T findById(String id);

    T findOneByField(String fieldName, Object value);

    //这里无法实现字段A先按ASC排序，字段B再按DESC排序
    List<T> findListByField(String fieldName, Object value,String... orderBy);

    List<T> findListByFieldDesc(String fieldName, Object value,String... orderBy);

    List<T> findList(Query query);

    List<T> findList(T entity);

    List<T> findAll(String... orderBy);

    List<T> findAllDesc(String... orderBy);

    List<String> findDistinct(String fieldName);

    boolean updateById(T entity);

    PageDTO<T> findPage(PageDTO<T> pageDTO);

    //这里只接收Criteria而不接收Query作为参数是为了防止在Query中和findPage的实现方法中同时出现关于分页、排序等相关的逻辑，导致分页逻辑混乱
    PageDTO<T> findPage(Criteria criteria, PageDTO<T> pageDTO);
}
