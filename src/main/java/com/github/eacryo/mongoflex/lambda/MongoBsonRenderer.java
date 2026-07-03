package com.github.eacryo.mongoflex.lambda;

import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MongoBsonRenderer {

    public static Bson render(
            LambdaQueryWrapper<?> wrapper) {

        List<Bson> filters = new ArrayList<>();

        for (Condition c : wrapper.getConditions()) {

            switch (c.operator()) {

                case EQ -> filters.add(
                        Filters.eq(
                                c.field(),
                                c.value()
                        )
                );

                case NE -> filters.add(
                        Filters.ne(
                                c.field(),
                                c.value()
                        )
                );

                case GT -> filters.add(
                        Filters.gt(
                                c.field(),
                                c.value()
                        )
                );

                case LT -> filters.add(
                        Filters.lt(
                                c.field(),
                                c.value()
                        )
                );

                case GTE -> filters.add(
                        Filters.gte(
                                c.field(),
                                c.value()
                        )
                );

                case LTE -> filters.add(
                        Filters.lte(
                                c.field(),
                                c.value()
                        )
                );

                case REGEX -> filters.add(
                        Filters.regex(
                                c.field(),
                                c.value().toString()
                        )
                );

                case IN -> filters.add(
                        Filters.in(
                                c.field(),
                                (Iterable<?>) c.value()
                        )
                );

                case NIN -> filters.add(
                        Filters.nin(
                                c.field(),
                                (Iterable<?>) c.value()
                        )
                );

                case EXISTS -> filters.add(
                        Filters.exists(
                                c.field(),
                                (Boolean) c.value()
                        )
                );

                case ALL -> filters.add(
                        Filters.all(
                                c.field(),
                                (Iterable<?>) c.value()
                        )
                );

                case SIZE -> filters.add(
                        Filters.size(
                                c.field(),
                                (Integer) c.value()
                        )
                );

                case ELEM_MATCH -> filters.add(
                        Filters.elemMatch(
                                c.field(),
                                (Bson) c.value()
                        )
                );

            }

        }

        return Filters.and(filters);
    }

}
