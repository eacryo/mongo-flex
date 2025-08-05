package com.github.eacryo.mongoflex.entity;



import java.util.List;

public class PageDTO<T> {
    private Long page = 1L; // 当前页码，默认从1开始
    private Long pageSize = 10L; // 每页记录数，默认10条
    private Long total; // 总记录数
    private Long totalPage; // 总页数
    private List<String> orderBy; // 排序字段，前端传递的字段名
    private List<T> records; // 当前页的记录列表
    private boolean orderByAsc = true; // 是否升序排序

    public Long getPage() {
        return page;
    }

    public void setPage(Long page) {
        this.page = page;
    }

    public Long getPageSize() {
        return pageSize;
    }

    public void setPageSize(Long pageSize) {
        this.pageSize = pageSize;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Long getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(Long totalPage) {
        this.totalPage = totalPage;
    }

    public List<String> getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(List<String> orderBy) {
        this.orderBy = orderBy;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public boolean isOrderByAsc() {
        return orderByAsc;
    }

    public void setOrderByAsc(boolean orderByAsc) {
        this.orderByAsc = orderByAsc;
    }
}
