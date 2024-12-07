package com.example.webbrowser.database;

import java.util.List;

public interface Database<T> {

    void insert(T object);
    void remove(T object);
    List<T> getAll();
    void create();
    void clear();

}
