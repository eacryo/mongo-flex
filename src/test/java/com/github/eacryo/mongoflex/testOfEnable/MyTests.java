package com.github.eacryo.mongoflex.testOfEnable;

import com.github.eacryo.mongoflex.bean.TestBean;
import com.github.eacryo.mongoflex.repository.TestRepository;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;


public class MyTests extends BaseMultiTenantsTest{

    @Autowired
    private TestRepository testRepository;

    @Test
    public void testLoad(){

    }

    @Test
    public void testSave(){
        MDC.put("tenant","testTenant");
        TestBean testBean = new TestBean();
        testBean.setName("test");
        testBean.setJoinDate(new Date());
        testRepository.save(testBean);
    }


}
