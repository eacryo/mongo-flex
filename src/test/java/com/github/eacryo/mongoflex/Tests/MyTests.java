package com.github.eacryo.mongoflex.Tests;

import com.github.eacryo.mongoflex.TestApplication;
import com.github.eacryo.mongoflex.bean.TestBean;
import com.github.eacryo.mongoflex.repository.TestRepository;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;


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
        testRepository.save(testBean);
    }


}
