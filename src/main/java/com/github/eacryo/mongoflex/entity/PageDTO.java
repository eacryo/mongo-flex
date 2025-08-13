package com.github.eacryo.mongoflex.entity;



import lombok.Data;

import java.util.List;


@Data
public class PageDTO<T> {
    private Long currentPage = 1L; // 当前页码，默认从1开始
    private Long pageSize = 10L; // 每页记录数，默认10条
    private Long total; // 总记录数
    private Long totalPage; // 总页数
    private List<String> orderBy; // 排序字段，前端传递的字段名
    private List<T> records; // 当前页的记录列表
    private boolean orderByAsc = true; // 是否升序排序
}
