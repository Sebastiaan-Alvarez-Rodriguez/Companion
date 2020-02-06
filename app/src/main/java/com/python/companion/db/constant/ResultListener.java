package com.python.companion.db.constant;

/**
 * Simple interface to return query results of any type to UI
 * @param <T> Type of returned result
 */
public interface ResultListener<T> {
    void onResult(T result);
}

