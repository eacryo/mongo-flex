package com.github.eacryo.mongoflex.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.Date;


@Data
public abstract class BaseEntity {

    @Id
    private String id; // 在切面中自动生成，类型为String，强烈建议不要手动给这个字段赋值

    private Date createAt; // 自动记录的创建时间

    private String createdBy; // 创建人

    private Date lastModifiedAt; // 自动记录的更新时间

    private String lastModifiedBy; // 最后修改人

    private String description; // 描述

    private String remark;

    private boolean deleted = false; // 软删除标记 (默认未删除)

}
