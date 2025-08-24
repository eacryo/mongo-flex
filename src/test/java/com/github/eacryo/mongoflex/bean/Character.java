package com.github.eacryo.mongoflex.bean;

import com.github.eacryo.mongoflex.Constant;
import com.github.eacryo.mongoflex.annotation.CollectionId;
import com.github.eacryo.mongoflex.annotation.CollectionName;
import com.github.eacryo.mongoflex.annotation.CreateDate;
import com.github.eacryo.mongoflex.annotation.UpdateDate;
import com.github.eacryo.mongoflex.constant.IdType;
import lombok.Data;
import lombok.ToString;

import java.util.Date;

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
    @CreateDate
    private Date createAt;
    @UpdateDate
    private Date updateAt;
}
