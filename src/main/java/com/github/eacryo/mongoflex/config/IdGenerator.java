package com.github.eacryo.mongoflex.config;

public interface IdGenerator<T> {
    T create();
}
