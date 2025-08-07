package com.github.eacryo.mongoflex.bean;

import com.github.eacryo.mongoflex.annotation.CollectionId;
import com.github.eacryo.mongoflex.annotation.CollectionName;

@CollectionName("testBean")
public class TestBean {

    @CollectionId
    private String id;
    private String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "TestBean{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
