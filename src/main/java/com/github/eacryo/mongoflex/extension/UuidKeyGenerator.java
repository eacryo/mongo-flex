package com.github.eacryo.mongoflex.extension;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UuidKeyGenerator implements KeyGenerator<String> {
    @Override
    public String get() {
        return UUID.randomUUID().toString();
    }
}
