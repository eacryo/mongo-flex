package com.github.eacryo.mongoflex.bean;

import com.github.eacryo.mongoflex.annotation.CollectionField;
import lombok.Data;
import lombok.ToString;

@Data
@ToString(callSuper = true)
public class GenshinCharacter extends Character{
    private String element;
}
