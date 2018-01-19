package com.less.aspider.db;

import java.util.List;

public interface BaseDao<T> {

    void createTable();

    void save(T entity);

    void delete(Long id);

    void update(T entity);

    void saveOrUpdate(T entity);

    T getById(Long id);

    List<T> findAll();

    Class getEntityClass();

    Result<T> getPage(int currentPage);

    int count();

    List<T> search(String text);

}
