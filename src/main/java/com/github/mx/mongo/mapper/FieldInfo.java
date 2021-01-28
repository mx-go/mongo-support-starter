package com.github.mx.mongo.mapper;

import com.github.mx.mongo.util.NameUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Create by max on 2020/01/16
 */
public class FieldInfo {

    private String fieldName;
    private String columnName;
    private Method getterMethod;
    private Method setterMethod;


    public static List<FieldInfo> parseField(Class<?> clazz) {
        final Field[] fields = clazz.getDeclaredFields();
        List<FieldInfo> fieldInfos = new ArrayList<>();
        Stream.of(fields).forEach(field -> {
            try {
                final Method getterMethod = clazz.getMethod("get" + NameUtils.capitalize(field.getName()));
                final Method setterMethod = clazz.getMethod("set" + NameUtils.capitalize(field.getName()), field.getType());
                if (getterMethod.getReturnType().equals(field.getType())) {
                    fieldInfos.add(new FieldInfo(field.getName(), field.getName(), getterMethod, setterMethod));
                }
            } catch (NoSuchMethodException e) {
                //ignore exception: just skip this field
            }
        });
        return fieldInfos;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public Method getGetterMethod() {
        return getterMethod;
    }

    public void setGetterMethod(Method getterMethod) {
        this.getterMethod = getterMethod;
    }

    public Method getSetterMethod() {
        return setterMethod;
    }

    public void setSetterMethod(Method setterMethod) {
        this.setterMethod = setterMethod;
    }

    public FieldInfo() {
    }

    public FieldInfo(String fieldName, String columnName, Method getterMethod, Method setterMethod) {
        this.fieldName = fieldName;
        this.columnName = columnName;
        this.getterMethod = getterMethod;
        this.setterMethod = setterMethod;
    }
}
