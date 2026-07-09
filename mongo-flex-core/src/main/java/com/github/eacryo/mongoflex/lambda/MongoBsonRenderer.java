package com.github.eacryo.mongoflex.lambda;

import com.github.eacryo.mongoflex.convertor.MongoMappingConvertor;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;

import java.util.ArrayList;
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

        List<Condition> allConditions = wrapper.getConditions();
        Class<?> entityClass = wrapper.getEntityClass();

        List<List<Condition>> groups = new ArrayList<>();
        List<Condition> currentGroup = new ArrayList<>();
        for (Condition c : allConditions) {
            if (c.isOrSeparator()) {
                if (!currentGroup.isEmpty()) {
                    groups.add(currentGroup);
                    currentGroup = new ArrayList<>();
                }
            } else {
                currentGroup.add(c);
            }
        }
        if (!currentGroup.isEmpty() || groups.isEmpty()) {
            groups.add(currentGroup);
        }

        if (groups.size() == 1) {
            return renderGroup(groups.get(0), entityClass, convertor);
        }

        List<Bson> orFilters = new ArrayList<>();
        for (List<Condition> group : groups) {
            if (!group.isEmpty()) {
                orFilters.add(renderGroup(group, entityClass, convertor));
            }
        }
        if (orFilters.isEmpty()) {
            return Filters.empty();
        }
        if (orFilters.size() == 1) {
            return orFilters.get(0);
        }
        return Filters.or(orFilters);
    }

    private static Bson renderGroup(List<Condition> conditions, Class<?> entityClass, MongoMappingConvertor convertor) {
        List<Bson> filters = new ArrayList<>();

        for (Condition c : conditions) {

            String field = entityClass != null
                    ? convertor.resolveMongoFieldName(entityClass, c.field())
                    : c.field();

            switch (c.operator()) {

                case EQ:
                    filters.add(Filters.eq(field, c.value()));
                    break;

                case NE:
                    filters.add(Filters.ne(field, c.value()));
                    break;

                case GT:
                    filters.add(Filters.gt(field, c.value()));
                    break;

                case LT:
                    filters.add(Filters.lt(field, c.value()));
                    break;

                case GTE:
                    filters.add(Filters.gte(field, c.value()));
                    break;

                case LTE:
                    filters.add(Filters.lte(field, c.value()));
                    break;

                case REGEX:
                    filters.add(Filters.regex(field, c.value().toString()));
                    break;

                case IN:
                    filters.add(Filters.in(field, (Iterable<?>) c.value()));
                    break;

                case NIN:
                    filters.add(Filters.nin(field, (Iterable<?>) c.value()));
                    break;

                case EXISTS:
                    filters.add(Filters.exists(field, (Boolean) c.value()));
                    break;

                case ALL:
                    filters.add(Filters.all(field, (Iterable<?>) c.value()));
                    break;

                case SIZE:
                    filters.add(Filters.size(field, (Integer) c.value()));
                    break;

                case ELEM_MATCH: {
                    LambdaQueryWrapper<?> subWrapper = (LambdaQueryWrapper<?>) c.value();
                    if (entityClass != null && subWrapper.getEntityClass() == null) {
                        Class<?> subClass = convertor.getFieldGenericElementType(entityClass, c.field());
                        if (subClass != null) {
                            ((LambdaQueryWrapper) subWrapper).setEntityClass(subClass);
                        }
                    }
                    Bson subFilter = render(subWrapper, convertor);
                    filters.add(Filters.elemMatch(field, subFilter));
                    break;
                }

            }

        }

        if (filters.isEmpty()) {
            return Filters.empty();
        }
        return Filters.and(filters);
    }

}
