package com.github.eacryo.mongoflex.handler;

import com.github.eacryo.mongoflex.config.IdGenerator;
import com.github.f4b6a3.ulid.UlidCreator;
import org.springframework.stereotype.Component;

@Component
public class MyIdGenerator implements IdGenerator<String> {
    @Override
    public String create() {
        return UlidCreator.getUlid().toString()+"_INPUT";
    }
}
