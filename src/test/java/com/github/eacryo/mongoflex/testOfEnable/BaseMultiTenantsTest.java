package com.github.eacryo.mongoflex.testOfEnable;

import com.github.eacryo.mongoflex.Constant;
import com.github.eacryo.mongoflex.TestApplication;
import com.github.eacryo.mongoflex.constant.MongoFlexConstant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.MDC;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("enable-multi-tenants")
public class BaseMultiTenantsTest {
    @BeforeEach
    public void setup() {
        MDC.put(MongoFlexConstant.TENANT,Constant.TENANT_ID);
    }

    @AfterEach
    public void tearDown() {
        MDC.clear();
    }
}
