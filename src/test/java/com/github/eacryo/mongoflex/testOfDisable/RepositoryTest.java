package com.github.eacryo.mongoflex.testOfDisable;

import com.github.eacryo.mongoflex.bean.User;
import com.github.eacryo.mongoflex.repository.UserRepsitory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

public class RepositoryTest extends BaseDisableTest{

    @Autowired
    private UserRepsitory userRepsitory;

    @Test
    public void testLoad(){

    }

    @Test
    public void testSaveAndQuery(){
        User user = new User();
        user.setName("Yuzuha");
        user.setJoinDate(new Date());
        userRepsitory.save(user);
        userRepsitory.findList(user).forEach(System.out::println);
    }
}
