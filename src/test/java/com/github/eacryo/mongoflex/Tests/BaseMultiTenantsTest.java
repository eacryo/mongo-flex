package com.github.eacryo.mongoflex.Tests;

import com.github.eacryo.mongoflex.TestApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("enable-multi-tenants")
public class BaseMultiTenantsTest {
}
