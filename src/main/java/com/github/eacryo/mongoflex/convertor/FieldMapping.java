package com.github.eacryo.mongoflex.convertor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldMapping {
    Field field;
    String mongoFieldName;
    Class<?> fieldType;
    Type genericType;
}
