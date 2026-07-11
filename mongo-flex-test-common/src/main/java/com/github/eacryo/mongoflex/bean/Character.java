package com.github.eacryo.mongoflex.bean;


import com.github.eacryo.mongoflex.Constant;
import com.github.eacryo.mongoflex.annotation.*;
import com.github.eacryo.mongoflex.constant.IdType;
import lombok.Data;
import lombok.ToString;

import java.util.Date;
import java.util.List;

@CollectionName(Constant.COLLECTION_NAME)
@Data
@ToString
public class Character {

    @CollectionId(IdType.ULID)
    private String id;
    private String cid;
    private String name;
    private String email;
    private String phone;
    private String address;
    private Date birthday;
    private String gender;
    private String status;
    private String description;

    // ========== 原神角色属性 ==========

    /** 神之眼/元素：Anemo, Geo, Electro, Dendro, Hydro, Pyro, Cryo */
    private String vision;

    /** 武器类型：Sword, Claymore, Polearm, Bow, Catalyst */
    @CollectionField("weapon_type")
    private String weapon;

    /** 稀有度：4 或 5 星 */
    private Integer rarity;

    /** 等级：1-90 */
    private Integer level;

    /** 命之座：0-6 */
    private Integer constellation;

    /** 基础攻击力 */
    @CollectionField("base_atk")
    private Integer baseATK;

    /** 好感度：1-10 */
    private Integer friendship;

    /** 是否为七神（尘世七执政） */
    @CollectionField("is_archon")
    private Boolean isArchon;

    /** 天赋技能名列表：如 ["蝶引来生", "血之灶火", "彼岸蝶舞"] */
    private List<String> talents;

    @CreateDate
    private Date createAt;
    @UpdateDate
    private Date updateAt;
    @CollectionField(value = "c_area")
    private String area;
}
