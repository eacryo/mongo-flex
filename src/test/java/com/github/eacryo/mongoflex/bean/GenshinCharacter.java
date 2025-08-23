package com.github.eacryo.mongoflex.bean;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(callSuper = true)
public class GenshinCharacter extends Character{
    private String element;
}
