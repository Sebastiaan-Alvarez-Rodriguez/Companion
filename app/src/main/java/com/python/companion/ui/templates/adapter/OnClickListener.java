package com.python.companion.ui.templates.adapter;


/**
 * Interface to allow click and long-click callbacks to be sent
 * @param <T> The type of items in the list
 */
public interface OnClickListener<T> {
    void onClick(T t);
    boolean onLongClick(T t);
}
