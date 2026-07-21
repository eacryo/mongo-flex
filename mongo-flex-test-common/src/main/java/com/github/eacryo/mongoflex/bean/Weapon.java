package com.github.eacryo.mongoflex.bean;

import com.github.eacryo.mongoflex.annotation.CollectionField;
import com.github.eacryo.mongoflex.annotation.CollectionId;
import com.github.eacryo.mongoflex.annotation.CollectionName;
import com.github.eacryo.mongoflex.constant.IdType;
import lombok.Data;

import java.util.Date;

@CollectionName("weapon")
@Data
public class Weapon {

    @CollectionId(IdType.ULID)
    private String id;

    /** 武器名 */
    private String name;

    /** 武器类型: Sword, Claymore, Polearm, Bow, Catalyst */
    private String type;

    /** 基础攻击力 */
    @CollectionField("base_atk")
    private Integer baseAtk;

    /** 副属性 */
    @CollectionField("sub_stat")
    private String subStat;

    /** 副属性值 */
    @CollectionField("sub_value")
    private Double subValue;

    /** 稀有度: 4 / 5 星 */
    private Integer rarity;

    /** 关联的角色 ID */
    @CollectionField("character_id")
    private String characterId;

    private Boolean deleted;
}
