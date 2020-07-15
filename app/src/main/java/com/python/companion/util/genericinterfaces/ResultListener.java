package com.python.companion.util.genericinterfaces;

/**
 * Simple interface to return query results of any type
 * @param <T> Type of returned result
 */
public interface ResultListener<T> {
    void onResult(T result);
}

