package org.esprit.services;

import java.util.List;

public interface IService<T> {

    void add(T t) throws Exception;
    void update(T t) throws Exception;
    void delete(T t) throws Exception;
    List<T> getAll() throws Exception;
}