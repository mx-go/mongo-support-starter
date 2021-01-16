package com.github.mx.mongo.mapper;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * Create by max on 2020/01/16
 */
@Getter
@Setter
@ToString
public class EntityMapper<T> {

    private Class<T> clazz;

    private FieldInfo idField;

    private List<FieldInfo> fieldInfos;
}