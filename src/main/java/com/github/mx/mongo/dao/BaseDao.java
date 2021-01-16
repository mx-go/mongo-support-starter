package com.github.mx.mongo.dao;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.List;

/**
 * 基础方法
 * Create by max on 2020/01/16
 */
public interface BaseDao<T> {

    /**
     * 获取Datastore,自定义操作
     *
     * @return Datastore
     */
    Datastore getDatastore();

    /**
     * 保存对象
     * 注意，这里只能用于新建保存，更新的话不要用这个方法.
     * 如果ID为String类型时会插入一条新的文档ID为字符串的新记录而不是更新类型为ObjectId的那条记录
     *
     * @return 返回新建的id
     */
    String insert(T entity);

    /**
     * 批量保存
     *
     * @param entities 对象集合
     */
    void insertBatch(List<T> entities);

    /**
     * 根据逻辑与条件查询一条记录
     *
     * @param condition 查询条件
     * @return 实体列表
     */
    T selectOne(T condition);

    /**
     * 根据逻辑与条件查询列表.
     *
     * @param condition 查询条件
     * @return 实体列表
     */
    List<T> selectList(T condition);

    /**
     * 查询指定数量
     *
     * @param condition 查询条件
     * @param offset    游标
     * @param limit     限制条数
     * @return 实体列表
     */
    List<T> selectList(T condition, int offset, int limit);

    /**
     * 根据id查询指定记录.
     *
     * @param id 记录id
     */
    T selectById(String id);

    /**
     * 根据id列表查询指定记录
     *
     * @param ids 记录id列表
     * @return
     */
    List<T> selectByIds(List<String> ids);

    /**
     * 根据逻辑与条件查询记录数目.
     *
     * @param condition 查询条件
     * @return 记录数目
     */
    long selectCount(T condition);

    /**
     * 更新
     *
     * @param query  查询条件
     * @param update 需要更新的信息
     * @return 影响的行数
     */
    long update(Query<T> query, UpdateOperations<T> update);

    /**
     * 更新
     *
     * @param entity 需要更新的信息
     * @param update 更新条件
     * @return 影响的行数
     */
    long update(T entity, UpdateOperations<T> update);

    /**
     * 删除
     *
     * @param condition 删除条件
     * @return 影响的行数
     */
    long delete(T condition);

    /**
     * 删除
     *
     * @param query 删除条件
     * @return 影响的行数
     */
    long delete(Query<T> query);

    /**
     * 创建查询
     *
     * @return Query
     */
    Query<T> createQuery();

    /**
     * 创建更新
     *
     * @return UpdateOperations
     */
    UpdateOperations<T> createUpdateOperations();
}