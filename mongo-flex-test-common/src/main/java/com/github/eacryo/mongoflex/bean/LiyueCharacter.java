package com.github.eacryo.mongoflex.bean;

import com.github.eacryo.mongoflex.annotation.CollectionField;
import lombok.Data;
import lombok.ToString;

/**
 * 璃月地区角色，继承 Character 通用属性，增加璃月特有的额外信息。
 */
@Data
@ToString(callSuper = true)
public class LiyueCharacter extends Character {

    /** 称号，如 "护法夜叉"、"无冕的龙王" */
    private String title;

    /** 所属组织，如 "往生堂"、"璃月七星"、"仙人"、"南十字船队" */
    private String affiliation;

    /** 是否仙人/半仙之体 */
    @CollectionField("is_adeptus")
    private Boolean isAdeptus;

    /** 摩拉数量（璃月以财富闻名） */
    @CollectionField("mora_amount")
    private Long moraAmount;

}
