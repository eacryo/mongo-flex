package com.github.eacryo.mongoflex.bean;

import com.github.eacryo.mongoflex.annotation.CollectionId;
import com.github.eacryo.mongoflex.annotation.CollectionName;
import lombok.Data;

import java.util.Date;

@CollectionName("testBean")
@Data
public class User {

    @CollectionId
    private String id;
    private String uid;
    private String name;
    private String password;
    private String email;
    private String phone;
    private String address;
    //这里不需要处理不同时区，前端传递的是带时区的时间，序列化时会自动处理
    private String birthday;
    private Date joinDate;
    private Date leaveDate;
    //状态，标记在职例如
    private String status;
    //角色，开发，测试，运维，产品，设计，运营，销售，市场，财务，行政，人事，法务，其他
    private String role;
    //上级
    private String supervisor;
    private String departmentCode;
    private String departmentName;
}
