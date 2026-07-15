package com.github.eacryo.mongoflex.query;

import com.github.eacryo.mongoflex.convertor.MongoMappingConvertor;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

/**
 * 将 LambdaQueryWrapper 中的条件渲染为 MongoDB Bson 过滤器。
 * 不直接对外使用，由 {@link com.github.eacryo.mongoflex.repository.SimpleMongoRepository} 在调用前
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

    /**
     * Render a group of conditions (AND-connected) to a Bson filter.
     * <p>
     * Raw type casts for sub-wrapper entityClass assignment (ELEM_MATCH, NOT) are
     * semantically safe: ELEM_MATCH uses the field's declared generic element type,
     * NOT reuses the outer wrapper's entityClass. Java wildcard capture prevents
     * calling {@code setEntityClass(Class<?>)} on {@code LambdaQueryWrapper<?>}.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Bson renderGroup(List<Condition> conditions, Class<?> entityClass, MongoMappingConvertor convertor) {
        List<Bson> filters = new ArrayList<>();

        for (Condition c : conditions) {

            // 优先用字段声明类的元数据（implClass），其次用 entityClass。
            // 这样 LiyueCharacter::getIsAdeptus 通过 CharacterRepository 查询时，
            // 也能正确解析 @CollectionField("is_adeptus") 映射。
            Class<?> resolveClass = c.implClass() != null ? c.implClass() : entityClass;
            String field = resolveClass != null
                    ? convertor.resolveMongoFieldName(resolveClass, c.field())
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

                case LIKE: {
                    String regex = c.value().toString()
                            .replace("*", ".*")
                            .replace("%", ".*");
                    filters.add(Filters.regex(field, regex));
                    break;
                }

                case NOT_LIKE: {
                    String regex = c.value().toString()
                            .replace("*", ".*")
                            .replace("%", ".*");
                    // $not must be field-level, not top-level
                    filters.add(new Document(field, new Document("$not", new Document("$regex", regex))));
                    break;
                }

                case BETWEEN: {
                    List<?> range = (List<?>) c.value();
                    filters.add(Filters.and(
                            Filters.gte(field, range.get(0)),
                            Filters.lte(field, range.get(1))
                    ));
                    break;
                }

                case IS_NULL:
                    filters.add(Filters.exists(field, false));
                    break;

                case IS_NOT_NULL:
                    filters.add(Filters.exists(field, true));
                    break;

                case NOT: {
                    LambdaQueryWrapper<?> subWrapper = (LambdaQueryWrapper<?>) c.value();
                    if (entityClass != null && subWrapper.getEntityClass() == null) {
                        ((LambdaQueryWrapper) subWrapper).setEntityClass(entityClass);
                    }
                    Bson subFilter = render(subWrapper, convertor);
                    // Use $nor instead of $not — $not is not a valid top-level operator
                    filters.add(Filters.nor(subFilter));
                    break;
                }

                case MOD: {
                    List<?> modArgs = (List<?>) c.value();
                    filters.add(Filters.mod(field, (Integer) modArgs.get(0), (Integer) modArgs.get(1)));
                    break;
                }

                case TYPE:
                    filters.add(Filters.type(field, c.value().toString()));
                    break;

            }

        }

        if (filters.isEmpty()) {
            return Filters.empty();
        }
        return Filters.and(filters);
    }

}
