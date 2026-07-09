package com.github.eacryo.mongoflex.constant;

public enum IdType {
    NONE(0),
    ULID(1),
    UUID(2),
    INPUT(3);

    private final int key;

    private IdType(int key) {
        this.key = key;
    }

    public int getKey() {
        return this.key;
    }}
