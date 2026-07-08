package com.github.eacryo.mongoflex.lambda;

import com.github.eacryo.mongoflex.convertor.MongoMappingConvertor;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 将 LambdaQueryWrapper 中的条件渲染为 MongoDB Bson 过滤器。
 * 不直接对外使用，由 {@link com.github.eacryo.mongoflex.v2.SimpleMongoRepository} 在调用前
 * 自动补齐 entityClass。直接调用时请确保 wrapper 已设置 entityClass，否则字段名无法转换为
 * MongoDB 实际字段名。
 */
public class MongoBsonRenderer {

    public static Bson render(
            LambdaQueryWrapper<?> wrapper,
            MongoMappingConvertor convertor) {

        List<Bson> filters = new ArrayList<>();
        Class<?> entityClass = wrapper.getEntityClass();

        for (Condition c : wrapper.getConditions()) {

            String field = entityClass != null
                    ? convertor.resolveMongoFieldName(entityClass, c.field())
                    : c.field();

            switch (c.operator()) {

                case EQ -> filters.add(
                        Filters.eq(field, c.value())
                );

                case NE -> filters.add(
                        Filters.ne(field, c.value())
                );

                case GT -> filters.add(
                        Filters.gt(field, c.value())
                );

                case LT -> filters.add(
                        Filters.lt(field, c.value())
                );

                case GTE -> filters.add(
                        Filters.gte(field, c.value())
                );

                case LTE -> filters.add(
                        Filters.lte(field, c.value())
                );

                case REGEX -> filters.add(
                        Filters.regex(field, c.value().toString())
                );

                case IN -> filters.add(
                        Filters.in(field, (Iterable<?>) c.value())
                );

                case NIN -> filters.add(
                        Filters.nin(field, (Iterable<?>) c.value())
                );

                case EXISTS -> filters.add(
                        Filters.exists(field, (Boolean) c.value())
                );

                case ALL -> filters.add(
                        Filters.all(field, (Iterable<?>) c.value())
                );

                case SIZE -> filters.add(
                        Filters.size(field, (Integer) c.value())
                );

                case ELEM_MATCH -> {
                    LambdaQueryWrapper<?> subWrapper = (LambdaQueryWrapper<?>) c.value();
                    if (entityClass != null && subWrapper.getEntityClass() == null) {
                        Class<?> subClass = convertor.getFieldGenericElementType(entityClass, c.field());
                        if (subClass != null) {
                            ((LambdaQueryWrapper) subWrapper).setEntityClass(subClass);
                        }
                    }
                    Bson subFilter = render(subWrapper, convertor);
                    filters.add(Filters.elemMatch(field, subFilter));
                }

            }

        }

        return Filters.and(filters);
    }

}
