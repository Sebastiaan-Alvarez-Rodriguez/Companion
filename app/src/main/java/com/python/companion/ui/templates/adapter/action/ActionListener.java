package com.python.companion.ui.templates.adapter.action;

import com.python.companion.ui.templates.adapter.OnClickListener;

/**
 * @see OnClickListener
 * Specialized template to additionally allow for actionmode change callbacks
 * @param <T> The type from items from the list
 */
public interface ActionListener<T> extends OnClickListener<T> {
        void onActionModeChange(boolean actionMode);
    }
