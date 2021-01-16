package com.github.mx.mongo.dao;

import com.github.mx.mongo.mapper.EntityMapper;
import com.github.mx.mongo.mapper.EntityMapperManager;
import com.github.mx.mongo.mapper.FieldInfo;
import com.github.mx.mongo.mongo.DatastoreExt;
import com.google.common.base.Strings;
import com.mongodb.AggregationOptions;
import com.mongodb.Cursor;
import com.mongodb.DBObject;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 基础方法实现类
 * <p>
 * Create by max on 2020/01/16
 */
@Getter
@Setter
public abstract class BaseDaoImpl<T> implements BaseDao<T> {

    private DatastoreExt datastore;
    private Class<T> clazz;

    public BaseDaoImpl(DatastoreExt datastoreExt, Class<T> clazz) {
        this.datastore = datastoreExt;
        this.clazz = clazz;
    }

    @Override
    public DatastoreExt getDatastore() {
        return this.datastore;
    }

    @Override
    public String insert(T entity) {
        return datastore.save(entity).getId().toString();
    }

    @Override
    public void insertBatch(List<T> entities) {
        datastore.insert(entities);
    }

    @Override
    public List<T> selectList(T condition) {
        final Query<T> query = createQuery(condition);
        return query.asList();
    }

    @Override
    public T selectOne(T condition) {
        List<T> list = this.selectList(condition);
        if (list.size() == 1) {
            return list.get(0);
        } else if (list.size() > 1) {
            throw new RuntimeException("Expected one result (or null) to be returned by queryOne(), but found: " + list.size());
        } else {
            return null;
        }
    }

    @Override
    public List<T> selectList(T condition, int offset, int limit) {
        final Query<T> query = createQuery(condition);
        FindOptions findOptions = new FindOptions();
        findOptions.skip(offset);
        findOptions.limit(limit);
        return query.asList(findOptions);
    }

    @Override
    public T selectById(String id) {
        final Query<T> query = createQuery();
        query.field(Mapper.ID_KEY).equal(new ObjectId(id));
        return query.get();
    }

    @Override
    public List<T> selectByIds(List<String> ids) {
        final Query<T> query = createQuery();
        List<ObjectId> objectIds = ids.stream().map(ObjectId::new).collect(Collectors.toList());
        query.field(Mapper.ID_KEY).in(objectIds);
        return query.asList();
    }

    @Override
    public long selectCount(T condition) {
        final Query<T> query = createQuery(condition);
        return datastore.getCount(query);
    }

    @Override
    public long delete(T condition) {
        return datastore.delete(condition).getN();
    }

    @Override
    public long delete(Query<T> query) {
        return datastore.delete(query).getN();
    }

    @Override
    public long update(T entity, UpdateOperations<T> operations) {
        return datastore.update(entity, operations).getUpdatedCount();
    }

    @Override
    public long update(Query<T> query, UpdateOperations<T> update) {
        return datastore.update(query, update).getUpdatedCount();
    }

    @Override
    public Query<T> createQuery() {
        return datastore.createQuery(clazz);
    }

    @Override
    public UpdateOperations<T> createUpdateOperations() {
        return datastore.createUpdateOperations(clazz);
    }

    public Query<T> createQuery(T condition) {
        final EntityMapper<T> entityMapper = EntityMapperManager.INSTANCE.getEntityMapper(clazz);
        final Query<T> query = createQuery();
        try {
            final String id = (String) entityMapper.getIdField().getGetterMethod().invoke(condition);
            if (id != null) {
                query.field(Mapper.ID_KEY).equal(new ObjectId(id));
            }
            for (final FieldInfo fieldInfo : entityMapper.getFieldInfos()) {
                if (!fieldInfo.getFieldName().equals(entityMapper.getIdField().getFieldName())) {
                    final Object fieldValue = fieldInfo.getGetterMethod().invoke(condition);
                    if (fieldValue instanceof String) {
                        if (!Strings.isNullOrEmpty((String) fieldValue)) {
                            query.field(fieldInfo.getFieldName()).equal(fieldValue);
                        }
                    } else {
                        if (null != fieldValue) {
                            query.field(fieldInfo.getFieldName()).equal(fieldValue);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return query;
    }

    /**
     * 聚合查询
     *
     * @param pipeline pipeline
     * @param options  AggregationOptions
     * @return
     */
    public Cursor aggregate(final List<? extends DBObject> pipeline, final AggregationOptions options) {
        return datastore.getCollection(clazz).aggregate(pipeline, options);
    }
}