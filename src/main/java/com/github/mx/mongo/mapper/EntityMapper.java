package com.github.mx.mongo.mapper;

import java.util.List;

/**
 * Create by max on 2020/01/16
 */
public class EntityMapper<T> {

    private Class<T> clazz;

    private FieldInfo idField;

    private List<FieldInfo> fieldInfos;

    public Class<T> getClazz() {
        return clazz;
    }

    public void setClazz(Class<T> clazz) {
        this.clazz = clazz;
    }

    public FieldInfo getIdField() {
        return idField;
    }

    public void setIdField(FieldInfo idField) {
        this.idField = idField;
    }

    public List<FieldInfo> getFieldInfos() {
        return fieldInfos;
    }

    public void setFieldInfos(List<FieldInfo> fieldInfos) {
        this.fieldInfos = fieldInfos;
    }
}