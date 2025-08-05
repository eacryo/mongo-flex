package com.github.eacryo.mongoflex.entity;

import org.springframework.data.annotation.Id;

import java.util.Date;

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getCreateAt() {
        return createAt;
    }

    public void setCreateAt(Date createAt) {
        this.createAt = createAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(Date lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
