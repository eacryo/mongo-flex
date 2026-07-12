package com.github.eacryo.mongoflex.entity;

import lombok.Data;

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
     * Compute total pages from total and pageSize / 根据总记录数和每页大小计算总页数
     * <p>
     * Follows MyBatis-Plus {@code IPage.getPages()} pattern: computed getter, not a
     * stored field. Zero total yields zero pages; pageSize of zero is a guard (returns 0).
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

}
