package com.python.companion.ui.templates;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.python.companion.R;
import com.python.companion.ui.templates.adapter.Adapter;
import com.python.companion.ui.templates.adapter.action.ActionAdapter;
import com.python.companion.ui.templates.adapter.action.ActionListener;

import static android.app.Activity.RESULT_OK;

@SuppressWarnings("WeakerAccess")
public abstract class Fragment<T> extends androidx.fragment.app.Fragment implements ActionListener<T> {
    protected static final int REQ_ADD = 0, REQ_UPDATE = 1;

    protected SearchView search;
    protected ImageView add, sort;
    protected RecyclerView list;

    protected ActionAdapter<T> adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        search = view.findViewById(R.id.searchview);
        sort = view.findViewById(R.id.sort_button);
        list = view.findViewById(R.id.list);
        add = view.findViewById(R.id.add);
        prepareList(view);
        prepareAdd(view, false);
        prepareSearch();
        prepareSort();
    }

    abstract protected void prepareSearch();

    protected void prepareSort() {
        sort.setOnClickListener(v -> adapter.sort(adapter.getSortStrategy() == Adapter.SortBy.NAME ? Adapter.SortBy.DATE : Adapter.SortBy.NAME));
    }
    abstract protected void prepareList(View view);

    @CallSuper
    protected void prepareAdd(View view, boolean actionMode) {
        add.setImageResource(actionMode ? R.drawable.ic_delete : R.drawable.ic_add);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        View v = getView();
        switch (requestCode) {
            case REQ_ADD:
                if (resultCode == RESULT_OK && v != null)
                    Snackbar.make(v, "Item added!", Snackbar.LENGTH_SHORT).show();
                break;
            case REQ_UPDATE:
                if (resultCode == RESULT_OK && v != null)
                    Snackbar.make(v, "Item not added!", Snackbar.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public boolean onLongClick(T t) {
        return true;
    }

    @Override
    public void onActionModeChange(boolean actionMode) {
        prepareAdd(getView(), actionMode);
    }

}
