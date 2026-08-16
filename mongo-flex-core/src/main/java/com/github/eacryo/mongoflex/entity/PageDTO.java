package com.github.eacryo.mongoflex.entity;

import com.github.eacryo.mongoflex.util.SFunction;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Pagination DTO / 分页数据传输对象
 * <p>
 * Follows MyBatis-Plus design: core numeric fields use primitive {@code long} to
 * avoid null unboxing risk. {@code totalPage} is a computed getter, not a stored
 * field — this keeps it always in sync with {@code total} and {@code pageSize}.
 * <p>
 * 遵循 MyBatis-Plus 设计：核心数字字段使用基本类型 {@code long} 避免 null 拆箱风险。
 * {@code totalPage} 是计算 getter 而非存储字段——始终与 {@code total} 和 {@code pageSize} 保持同步。
 * <p>
 * Two pagination modes / 两种分页模式：
 * <ul>
 *   <li><b>Page-number mode (default)</b> — driven by {@link #currentPage} + {@link #pageSize},
 *       skip = {@code (currentPage - 1) * pageSize}. / <b>页码模式（默认）</b>——由
 *       {@link #currentPage} + {@link #pageSize} 驱动，skip = {@code (currentPage - 1) * pageSize}。</li>
 *   <li><b>Offset mode</b> — when {@link #setOffset(Long)} is set, {@code offset} takes
 *       precedence: skip = {@code offset}, limit = {@code pageSize}, and {@link #currentPage}
 *       is ignored. / <b>offset 模式</b>——设置了 {@link #setOffset(Long)} 时 offset 优先：
 *       skip = {@code offset}，limit = {@code pageSize}，此时忽略 {@link #currentPage}。</li>
 * </ul>
 * <p>
 * Two total-count modes / 两种总数统计模式（参考 MyBatis-Plus {@code searchCount} 与
 * Spring Data {@code Slice}）：
 * <ul>
 *   <li><b>{@code countTotal = true} (default)</b> — a {@code countDocuments} query fills
 *       {@link #total} and the navigation properties ({@link #getHasNext()} / {@link #isFirst()}
 *       / {@link #isLast()} / {@link #hasPrevious()}) are derived from it. /
 *       <b>统计总数（默认）</b>——执行 {@code countDocuments} 填充 {@link #total}，
 *       导航属性（{@link #getHasNext()} / {@link #isFirst()} / {@link #isLast()} /
 *       {@link #hasPrevious()}）基于它计算。</li>
 *   <li><b>{@code countTotal = false}</b> — skips the count query (cheaper on large
 *       collections); the executor fetches {@code pageSize + 1} documents and sets
 *       {@link #getHasNext()} from the extra one. {@link #total} stays 0 and
 *       {@link #getTotalPage()} is meaningless. / <b>不统计总数</b>——跳过 count 查询
 *       （大集合更省）；执行器多取 {@code pageSize + 1} 条，用多出的那条填充
 *       {@link #getHasNext()}。此时 {@link #total} 保持 0，{@link #getTotalPage()} 无意义。</li>
 * </ul>
 *
 * @param <T> record type / 记录类型
 */
@Data
public class PageDTO<T> {
    /** Current page number, starts from 1 / 当前页码，默认从1开始 */
    private long currentPage = 1L;
    /** Page size, default 10 / 每页记录数，默认10条 */
    private long pageSize = 10L;
    /** Total record count / 总记录数 */
    private long total;
    /** Sort orders (field name + direction) / 排序规则列表（字段名 + 方向） */
    private List<SortOrder<T>> orderBy;
    /** Current page records / 当前页的记录列表 */
    private List<T> records;

    /**
     * Whether the paginated query should also count the total number of matching documents.
     * Defaults to {@code true}. Set to {@code false} for a lightweight "slice" query that
     * skips the count — the executor then derives {@link #getHasNext()} by fetching one
     * extra document (Spring Data {@code Slice} semantics). /
     * 分页查询是否统计匹配文档总数。默认 {@code true}。设为 {@code false} 可跳过 count，
     * 执行器通过多取一条文档推导 {@link #getHasNext()}（Spring Data {@code Slice} 语义）。
     */
    private boolean countTotal = true;

    /**
     * Filled by the pagination executor: whether there is a next page. {@code null} means
     * "not filled" — {@link #getHasNext()} then derives it from {@link #total}. Callers
     * should not set this directly. / 由分页执行器填充：是否还有下一页。{@code null} 表示
     * "未填充"——{@link #getHasNext()} 将基于 {@link #total} 计算。调用方不应直接设置。
     * The getter is hand-written (with fallback logic), so Lombok generation is explicitly
     * disabled for it; the boxed setter {@code setHasNext(Boolean)} is still generated. /
     * getter 为手写（含回退计算逻辑），因此显式禁用 Lombok 生成；装箱 setter
     * {@code setHasNext(Boolean)} 仍由 Lombok 生成。
     */
    @Getter(AccessLevel.NONE)
    private Boolean hasNext;

    /**
     * Whether there is a next page. Prefers the executor-filled value (lightweight mode);
     * otherwise derives it from {@link #total}. / 是否还有下一页。优先使用执行器填充的值
     * （轻量模式）；否则基于 {@link #total} 推导。
     */
    public boolean getHasNext() {
        if (hasNext != null) {
            return hasNext;
        }
        if (offset != null) {
            return offset + pageSize < total;
        }
        return currentPage < getTotalPage();
    }

    /** Whether this is the first page / 是否为第一页 */
    public boolean isFirst() {
        return offset != null ? offset == 0 : currentPage <= 1;
    }

    /** Whether this is the last page / 是否为最后一页 */
    public boolean isLast() {
        if (hasNext != null) {
            return !hasNext;
        }
        return offset != null ? offset + pageSize >= total : currentPage >= getTotalPage();
    }

    /** Whether there is a previous page / 是否有上一页 */
    public boolean hasPrevious() {
        return offset != null ? offset > 0 : currentPage > 1;
    }

    /**
     * Optional offset-based pagination. {@code null} (default) keeps page-number semantics;
     * a non-null value makes the query skip this many documents (limit is {@link #pageSize})
     * and ignores {@link #currentPage}. / 可选的 offset 分页。{@code null}（默认）保持页码语义；
     * 非 null 时查询跳过这么多条文档（limit 为 {@link #pageSize}），并忽略 {@link #currentPage}。
     * <p>
     * Lombok generates {@code setOffset(Long)} / {@code getOffset()} for this field — use
     * {@code setOffset(2L)} or {@code setOffset(null)} to clear. For int literals and fluent
     * chaining use {@link #offset(long)} (a hand-written method whose name does not collide
     * with Lombok's setter). / Lombok 为此字段生成 {@code setOffset(Long)} / {@code getOffset()}
     * ——可用 {@code setOffset(2L)} 或 {@code setOffset(null)} 清除；int 字面量与链式调用请用
     * {@link #offset(long)}（手写方法，方法名与 Lombok setter 不冲突）。
     */
    private Long offset;

    /** Fluent, int-friendly setter: {@code pageDTO.offset(5)} — chainable with {@link #sortBy}. / 流畅式、int 友好的设置：{@code pageDTO.offset(5)}——可与 {@link #sortBy} 链式调用。 */
    public PageDTO<T> offset(long offset) {
        this.offset = offset;
        return this;
    }

    /**
     * Compute total pages from total and pageSize / 根据总记录数和每页大小计算总页数
     * <p>
     * Follows MyBatis-Plus {@code IPage.getPages()} pattern: computed getter, not a
     * stored field. Zero total yields zero pages; pageSize of zero is a guard (returns 0).
     * Note: only meaningful in page-number mode — offset mode ignores it. /
     * 遵循 MyBatis-Plus {@code IPage.getPages()} 模式：计算 getter，非存储字段。
     * total 为 0 时返回 0；pageSize 为 0 时防御性返回 0。注意：仅在页码模式下有意义，
     * offset 模式忽略它。
     *
     * @return total page count / 总页数
     */
    public long getTotalPage() {
        if (pageSize == 0) {
            return 0;
        }
        long pages = total / pageSize;
        if (total % pageSize != 0) {
            pages++;
        }
        return pages;
    }

    // ---- sortBy / 排序便捷方法 ----

    private List<SortOrder<T>> orderBy0(SortOrder<T> order) {
        if (orderBy == null) {
            orderBy = new ArrayList<>();
        }
        orderBy.add(order);
        return orderBy;
    }

    /**
     * Append sort orders (chainable). Existing orders are kept; use {@link #setOrderBy(List)}
     * to replace the whole list. / 追加排序条件（可链式）。已存在的排序保留；
     * 如需整体替换请用 {@link #setOrderBy(List)}。
     */
    @SafeVarargs
    public final PageDTO<T> sortBy(SortOrder<T>... orders) {
        if (orders != null) {
            for (SortOrder<T> order : orders) {
                if (order != null) {
                    orderBy0(order);
                }
            }
        }
        return this;
    }

    /**
     * Append a string-based sort order, e.g. {@code sortBy("address", true)} — the field is
     * resolved against the repository's entity class (honors {@code @CollectionField}). /
     * 追加字符串字段排序，如 {@code sortBy("address", true)}——字段按仓库实体类解析（支持
     * {@code @CollectionField} 映射）。
     */
    public PageDTO<T> sortBy(String field, boolean ascending) {
        return sortBy(new SortOrder<>(field, ascending));
    }

    /**
     * Append a lambda-based sort order, e.g. {@code sortBy(Character::getAddress, false)} —
     * type-safe and resolves {@code @CollectionField} from the field's declaring class. /
     * 追加 lambda 类型安全排序，如 {@code sortBy(Character::getAddress, false)}——从字段声明类
     * 正确解析 {@code @CollectionField} 映射。
     */
    public PageDTO<T> sortBy(SFunction<T, ?> field, boolean ascending) {
        return sortBy(new SortOrder<>(field, ascending));
    }

    /**
     * Parse and append sort orders from a comma-separated expression, e.g.
     * {@code sortBy("address:desc, level:asc")}. Each item is {@code field[:asc|:desc]};
     * the direction defaults to ascending when omitted. Field names are Java field names
     * resolved against the repository's entity class. / 解析逗号分隔的排序表达式并追加，
     * 如 {@code sortBy("address:desc, level:asc")}。每项格式为 {@code field[:asc|:desc]}，
     * 缺省方向为升序。字段名为 Java 字段名，按仓库实体类解析。
     *
     * @param sortExpr comma-separated sort expression, null/blank is a no-op /
     *                 逗号分隔的排序表达式，null 或空白为无操作
     */
    public PageDTO<T> sortBy(String sortExpr) {
        if (sortExpr == null || sortExpr.trim().isEmpty()) {
            return this;
        }
        for (String item : sortExpr.split(",")) {
            String trimmed = item.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] parts = trimmed.split(":");
            String field = parts[0].trim();
            boolean ascending = parts.length < 2 || !"desc".equalsIgnoreCase(parts[1].trim());
            orderBy0(new SortOrder<>(field, ascending));
        }
        return this;
    }

}
