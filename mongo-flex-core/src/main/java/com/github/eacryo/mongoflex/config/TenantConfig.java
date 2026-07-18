package com.github.eacryo.mongoflex.config;


import lombok.Data;

@Data
public class TenantConfig {
    private String name;
    private String uri;
}
