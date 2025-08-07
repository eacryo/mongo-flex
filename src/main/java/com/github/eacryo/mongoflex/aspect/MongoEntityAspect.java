package com.github.eacryo.mongoflex.aspect;


import com.github.eacryo.mongoflex.config.MongoTemplateFactory;
import com.github.eacryo.mongoflex.constant.SystemConstant;
import com.github.eacryo.mongoflex.entity.BaseEntity;
import com.github.eacryo.mongoflex.util.ReflectUtil;
import com.github.f4b6a3.ulid.UlidCreator;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

/**
 * Aspect for MongoDB entity operations
 * 1. Auto-generates ID for new BaseEntity instances
 * 2. Updates updateTime field on save/update operations
 */
@Aspect
@Component
public class MongoEntityAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoEntityAspect.class);


    //TODO:已知问题，手动设置id时handleInsertOrSave插入会被误判为update操作

    // 处理insert和save操作
    @Around("execution(* org.springframework.data.mongodb.core.MongoTemplate.insert*(Object,String))")
    public Object handleInsertOrSave(ProceedingJoinPoint joinPoint) throws Throwable {
        LOGGER.info("命中insert,save操作");
        Object[] args = joinPoint.getArgs();

        int processedCount = 0;
        Object arg = args[0];
        processedCount += processArgument(arg, 0);

        if (processedCount > 0) {
            LOGGER.info("处理了{}个BaseEntity实例", processedCount);
        }

        return joinPoint.proceed();
    }

    /**
     * 处理单个参数，支持BaseEntity和Collection<BaseEntity>
     *
     * @param arg        参数对象
     * @param paramIndex 参数索引
     * @return 处理的实体数量
     */
    private int processArgument(Object arg, int paramIndex) {
        int processedCount = 0;

        if (! (arg instanceof Map) && ! (arg instanceof Collection)) {
            // 普通的 java bean
            processBaseEntity(arg, paramIndex, "单个实体");
            processedCount++;
        } else if (arg instanceof Collection) {
            // 处理Collection<BaseEntity>
            Collection<?> collection = (Collection<?>) arg;
            processedCount += processEntityCollection(collection, paramIndex);
        }

        return processedCount;
    }

    /**
     * 处理Collection<BaseEntity>
     *
     * @param collection 集合对象
     * @param paramIndex 参数索引
     * @return 处理的实体数量
     */
    private int processEntityCollection(Collection<?> collection, int paramIndex) {
        int processedCount = 0;

        for (Object item : collection) {
            if (!(item instanceof Map)) {
                processBaseEntity(item, paramIndex, "集合中的实体");
                processedCount++;
            }
        }

        if (processedCount > 0) {
            LOGGER.debug("在第{}个参数中处理了{}个BaseEntity集合元素", paramIndex, processedCount);
        }

        return processedCount;
    }

    /**
     * 处理单个entity
     *
     * @param entity entity
     * @param paramIndex 参数索引
     * @param context    上下文信息（用于日志）
     */
    private void processBaseEntity(Object entity, int paramIndex, String context) {
        LOGGER.debug("处理第{}个参数中的{}: {}", paramIndex, context, entity.getClass().getSimpleName());
            Field idField = ReflectUtil.getIdField(entity.getClass());
            idField.setAccessible(true);
            Object id;
            try {
                id = idField.get(entity);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        // 生成ID (如果是新实体)
        if (Objects.isNull(id)) {
            //TODO:
            //先写死ULID
            try {
                id = UlidCreator.getUlid().toString();
                idField.set(entity,id);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            //baseEntity.setCreateAt(new Date());
            String uid = MDC.get(SystemConstant.UID);
            //TODO:使用注解填充字段
//            if (Objects.nonNull(uid)) {
//                baseEntity.setCreatedBy(uid);
//            } else {
//                baseEntity.setCreatedBy("SYSTEM");
//            }
            LOGGER.debug("为新实体生成ID: {}", id);
        } else {
            // 更新时间戳
//            baseEntity.setLastModifiedAt(new Date());
//            baseEntity.setLastModifiedBy("SYSTEM");
//            LOGGER.debug("更新现有实体的时间戳: {}", baseEntity.getId());
            LOGGER.debug("实体已经存在主键");
        }
    }

    // TODO:处理update操作
    @Around("execution(* org.springframework.data.mongodb.core.MongoTemplate.update*(..)) || " +
            "execution(* org.springframework.data.mongodb.core.MongoTemplate.upsert*(..)) || " +
            "execution(* org.springframework.data.mongodb.core.MongoTemplate.findAndModify*(..))")
    public Object handleUpdateOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        LOGGER.info("命中update操作");
        Object[] args = joinPoint.getArgs();

        // 遍历所有参数，查找Update类型并设置更新时间
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Update) {
                Update update = (Update) args[i];
                update.set("lastModifiedAt", new Date());
                String uid = MDC.get(SystemConstant.UID);
                if (Objects.nonNull(uid)) {
                    update.set("lastModifiedBy", uid);
                } else {
                    update.set("lastModifiedBy", "SYSTEM");
                }
                LOGGER.debug("在第{}个参数中设置Update时间戳", i);
            }
        }

        return joinPoint.proceed();
    }
}
