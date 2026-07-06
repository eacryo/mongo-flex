package com.github.eacryo.mongoflex.lambda;

import com.github.eacryo.mongoflex.convertor.MongoMappingConvertor;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
