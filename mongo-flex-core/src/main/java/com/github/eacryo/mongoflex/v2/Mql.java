package com.github.eacryo.mongoflex.v2;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MongoDB Shell command query — <b>deprecated</b>, use {@link Find} / {@link Count} / {@link Delete} instead.
 * <p>
 * MongoDB Shell 命令查询——<b>已废弃</b>，请使用 {@link Find} / {@link Count} / {@link Delete} 替代。
 *
 * @deprecated since 2.0 — replaced by {@link Find}, {@link Count}, and {@link Delete} annotations
 *             which use pure JSON filter templates instead of shell command parsing.
 */
@Deprecated
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Mql {
    /** MongoDB shell command, e.g. {@code db.getCollection('x').find({...})} / MongoDB Shell 命令 */
    String value();
}
