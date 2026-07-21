package com.github.eacryo.mongoflex.bean;

import com.github.eacryo.mongoflex.annotation.CollectionField;
import lombok.Data;

/**
 * Aggregation output DTO: Character joined with Weapon via $lookup /
 * 聚合输出 DTO：通过 $lookup 联表 Character 和 Weapon
 */
@Data
public class CharacterWithWeapon {

    private String id;
    private String name;
    private String vision;
    private Integer level;

    @CollectionField("c_area")
    private String area;

    /** Weapon subdocument from $lookup + $unwind / 由 $lookup + $unwind 产生的 Weapon 子文档 */
    private Weapon weapon;
}
