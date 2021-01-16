package com.github.mx.mongo.mongo;

import org.mongodb.morphia.mapping.Mapper;

/**
 * 支持设定collection的前后缀，比如 2019_%s, %s_2019
 * <p>
 * Create by max on 2020/01/16
 */
public class MapperExt extends Mapper {
    private final String format;

    public MapperExt(String format) {
        this.format = format;
    }

    @Override
    public String getCollectionName(Object object) {
        return String.format(format, super.getCollectionName(object));
    }
}