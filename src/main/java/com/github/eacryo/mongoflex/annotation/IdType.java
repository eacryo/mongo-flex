package com.github.eacryo.mongoflex.annotation;

public enum IdType {

    NONE(0),
    TIMESTAMP(1),
    UUID(2),
    ULID(3),
    CUSTOM(4);

    private final int key;

    private IdType(int key) {
        this.key = key;
    }

    public int getKey() {
        return this.key;
    }
}
