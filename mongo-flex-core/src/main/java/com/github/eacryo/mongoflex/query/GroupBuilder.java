package com.github.eacryo.mongoflex.query;

import com.github.eacryo.mongoflex.convertor.MongoMappingConvertor;
import com.github.eacryo.mongoflex.util.ReflectUtil;
import com.github.eacryo.mongoflex.util.SFunction;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Builder for the {@code $group} stage accumulators / {@code $group} 阶段累加器构建器
 * <p>
 * Created by {@link AggregationWrapper#group(SFunction...)}. Chain accumulator
 * methods ({@code sum}, {@code count}, {@code avg}, {@code min}, {@code max})
 * and call {@link #end()} to return to the parent {@link AggregationWrapper}.
 * <p>
 * 由 {@link AggregationWrapper#group(SFunction...)} 创建。链式调用累加器方法
 * （{@code sum}, {@code count}, {@code avg}, {@code min}, {@code max}），
 * 最后调用 {@link #end()} 回到父级 {@link AggregationWrapper}。
 *
 * @param <T> entity type / 实体类型
 */
public final class GroupBuilder<T> {

    private final AggregationWrapper<T> parent;
    private final List<SFunction<T, ?>> idFields;
    private final List<Accumulator> accumulators = new ArrayList<>();

    private static class Accumulator {
        final String alias;
        final String operator;
        final SFunction<?, ?> field;

        Accumulator(String alias, String operator, SFunction<?, ?> field) {
            this.alias = alias;
            this.operator = operator;
            this.field = field;
        }
    }

    GroupBuilder(AggregationWrapper<T> parent, SFunction<T, ?>[] idFields) {
        this.parent = parent;
        this.idFields = Arrays.asList(idFields);
    }

    /** {@code $sum} accumulator / {@code $sum} 累加器 */
    public GroupBuilder<T> sum(String alias, SFunction<T, ?> field) {
        Objects.requireNonNull(field, "field must not be null");
        accumulators.add(new Accumulator(alias, "$sum", field));
        return this;
    }

    /** {@code $avg} accumulator / {@code $avg} 累加器 */
    public GroupBuilder<T> avg(String alias, SFunction<T, ?> field) {
        Objects.requireNonNull(field, "field must not be null");
        accumulators.add(new Accumulator(alias, "$avg", field));
        return this;
    }

    /** {@code $min} accumulator / {@code $min} 累加器 */
    public GroupBuilder<T> min(String alias, SFunction<T, ?> field) {
        Objects.requireNonNull(field, "field must not be null");
        accumulators.add(new Accumulator(alias, "$min", field));
        return this;
    }

    /** {@code $max} accumulator / {@code $max} 累加器 */
    public GroupBuilder<T> max(String alias, SFunction<T, ?> field) {
        Objects.requireNonNull(field, "field must not be null");
        accumulators.add(new Accumulator(alias, "$max", field));
        return this;
    }

    /** {@code $count} accumulator ({@code {$sum: 1}}) / {@code $count} 累加器（{@code {$sum: 1}}） */
    public GroupBuilder<T> count(String alias) {
        accumulators.add(new Accumulator(alias, "$sum", null));
        return this;
    }

    /** {@code $push} accumulator / {@code $push} 累加器 */
    public GroupBuilder<T> push(String alias, SFunction<T, ?> field) {
        Objects.requireNonNull(field, "field must not be null");
        accumulators.add(new Accumulator(alias, "$push", field));
        return this;
    }

    /** {@code $addToSet} accumulator / {@code $addToSet} 累加器 */
    public GroupBuilder<T> addToSet(String alias, SFunction<T, ?> field) {
        Objects.requireNonNull(field, "field must not be null");
        accumulators.add(new Accumulator(alias, "$addToSet", field));
        return this;
    }

    /** {@code $first} accumulator / {@code $first} 累加器 */
    public GroupBuilder<T> first(String alias, SFunction<T, ?> field) {
        Objects.requireNonNull(field, "field must not be null");
        accumulators.add(new Accumulator(alias, "$first", field));
        return this;
    }

    /** {@code $last} accumulator / {@code $last} 累加器 */
    public GroupBuilder<T> last(String alias, SFunction<T, ?> field) {
        Objects.requireNonNull(field, "field must not be null");
        accumulators.add(new Accumulator(alias, "$last", field));
        return this;
    }

    /**
     * Finalize the group stage and return to the parent {@link AggregationWrapper} /
     * 完成 group 阶段并返回父级 {@link AggregationWrapper}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public AggregationWrapper<T> end() {
        if (parent.isStagesEmpty() && accumulators.isEmpty()) {
            throw new IllegalStateException(
                    "GroupBuilder must have at least one accumulator. Did you forget to call sum/count/avg/etc. before end()?"
                    + " / GroupBuilder 至少需要一个累加器，是否在 end() 前忘记调用 sum/count/avg 等方法？");
        }
        parent.addStage(new GroupStageFunction((List) idFields, accumulators));
        return parent;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static class GroupStageFunction implements Function<MongoMappingConvertor, Bson> {
        private final List<SFunction<?, ?>> idFields;
        private final List<Accumulator> accumulators;

        GroupStageFunction(List<SFunction<?, ?>> idFields, List<Accumulator> accumulators) {
            this.idFields = idFields;
            this.accumulators = accumulators;
        }

        @Override
        public Bson apply(MongoMappingConvertor convertor) {
            Document groupDoc = new Document();

            if (idFields.size() == 1) {
                SFunction f = idFields.get(0);
                Class<?> implClass = ReflectUtil.getImplClassFromLambda((SFunction) f);
                String mongoField = convertor.resolveMongoFieldPath(implClass,
                        ReflectUtil.getFieldNameFromLambda((SFunction) f));
                groupDoc.append("_id", "$" + mongoField);
            } else {
                Document idObj = new Document();
                for (SFunction f : idFields) {
                    Class<?> implClass = ReflectUtil.getImplClassFromLambda((SFunction) f);
                    String mongoField = convertor.resolveMongoFieldPath(implClass,
                            ReflectUtil.getFieldNameFromLambda((SFunction) f));
                    idObj.append(mongoField, "$" + mongoField);
                }
                groupDoc.append("_id", idObj);
            }

            for (Accumulator acc : accumulators) {
                if (acc.field == null) {
                    groupDoc.append(acc.alias, new Document("$sum", 1));
                } else {
                    Class<?> implClass = ReflectUtil.getImplClassFromLambda((SFunction) acc.field);
                    String mongoField = convertor.resolveMongoFieldPath(implClass,
                            ReflectUtil.getFieldNameFromLambda((SFunction) acc.field));
                    groupDoc.append(acc.alias, new Document(acc.operator, "$" + mongoField));
                }
            }

            return new Document("$group", groupDoc);
        }
    }
}
