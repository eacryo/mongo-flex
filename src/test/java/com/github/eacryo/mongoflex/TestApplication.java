package com.github.eacryo.mongoflex;

import com.github.eacryo.mongoflex.v2.MyOrmRegistrar;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(MyOrmRegistrar.class)
public class TestApplication {
}
