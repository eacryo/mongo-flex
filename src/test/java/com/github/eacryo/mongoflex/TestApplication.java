package com.github.eacryo.mongoflex;

import com.github.eacryo.mongoflex.v2.RepositoryRegistrar;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(RepositoryRegistrar.class)
public class TestApplication {
}
