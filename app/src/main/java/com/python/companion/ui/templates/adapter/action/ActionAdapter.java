package com.python.companion.ui.templates.adapter.action;

import android.view.View;

import androidx.annotation.Nullable;

import com.python.companion.ui.templates.adapter.checkable.CheckableAdapter;

import java.util.List;

/**
 * @see CheckableAdapter
 * Template specialization to allow for action modes, without Android's monstrosities
 * @param <T> The type of items in the list
 */
public abstract class ActionAdapter<T> extends CheckableAdapter<T> {
    private boolean actionMode = false;

    /**
     * Constructor to set an actionlistener
     * @param actionListener the listener to send callbacks to in case of clicks or action mode changes
     */
    public ActionAdapter(ActionListener<T> actionListener) {
        super(null, actionListener);
    }

    /**
     * @see #ActionAdapter(ActionListener)
     * Same function, without having to call with null as argument
     */
    public ActionAdapter() {
        this(null);
    }

    /**
     * @see CheckableAdapter#onClick(View, int)
     * Selects item in case of action mode. Sends click event otherwise.
     */
    @Override
    public void onClick(View view, int pos) {
        if (actionMode) {
            super.onClick(view, pos);

            if (actionMode && !hasSelected()) {
                actionMode = false;
                ((ActionListener) onClickListener).onActionModeChange(false);
            }
        } else {
            onClickListener.onClick(list.get(pos));
        }
    }

    /**
     * @see CheckableAdapter#onLongClick(View, int)
     * Activates actionmode and selects item
     * @return true if the click is consumed, false otherwise
     */
    @Override
    public boolean onLongClick(View view, int pos) {
        if (!actionMode) {
            actionMode = true;
            ((ActionListener) onClickListener).onActionModeChange(true);
        }
        boolean consumed =  super.onLongClick(view, pos);

        if (actionMode && !hasSelected()) {
            actionMode = false;
            ((ActionListener) onClickListener).onActionModeChange(false);
        }
        return consumed;
    }

    /**
     * @return whether actionmode is active or not
     */
    public boolean isActionMode() {
        return actionMode;
    }

    /**
     * @see CheckableAdapter#onChanged(List)
     * Stops action mode
     */
    @Override
    public void onChanged(@Nullable List<T> newList) {
        actionMode = false;
        ((ActionListener) onClickListener).onActionModeChange(false);
        super.onChanged(newList);
    }
}
