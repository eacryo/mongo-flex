package com.github.eacryo.mongoflex.testOfDisable;


import com.github.eacryo.mongoflex.TestApplication;
import com.github.eacryo.mongoflex.constant.MongoFlexConstant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("disable-multi-tenants")
public class BaseDisableTest {

}
