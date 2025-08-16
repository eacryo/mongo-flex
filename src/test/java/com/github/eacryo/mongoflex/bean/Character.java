package com.github.eacryo.mongoflex.bean;

import com.github.eacryo.mongoflex.Constant;
import com.github.eacryo.mongoflex.annotation.CollectionId;
import com.github.eacryo.mongoflex.annotation.CollectionName;
import lombok.Data;

import java.util.Date;

@CollectionName(Constant.COLLECTION_NAME)
@Data
public class Character {

    @CollectionId
    private String id;
    private String cid;
    private String name;
    private String email;
    private String phone;
    private String address;
    private Date birthday;
    private String gender;
    private String status;
    private Date createAt;
}
