package com.python.companion.ui.templates.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.view.ViewStub;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.CallSuper;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.python.companion.R;
import com.python.companion.ui.templates.adapter.Adapter;

import java.util.List;

@SuppressWarnings("WeakerAccess")
public abstract class OverrideDialog<T> {
    protected TextView textExists, textRelations;
    protected ViewStub conflictItem;
    protected Button cancelButton, overrideButton;

    protected Dialog dialog;
    protected RecyclerView list;

    protected Activity parent;

    public OverrideDialog(Activity activity) { parent = activity; }

    public void showDialog(T conflicting, long conflictID, OverrideListener onOverrideListener) {
        dialog = new Dialog(parent);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_override);

        findGlobalViews(dialog);
        prepareList(conflictID);

        setupButtons(dialog, onOverrideListener);

        inflateViews(conflicting, conflictID);
        setTextViews(conflicting);
    }

    protected <V> void setList(Adapter<V> adapter, List<V> itemList) {
        if (itemList.isEmpty()) {
            list.setVisibility(View.GONE);
            textRelations.setVisibility(View.GONE);
        } else {
            adapter.onChanged(itemList);
            list.setLayoutManager(new LinearLayoutManager(parent));
            list.setAdapter(adapter);
            list.addItemDecoration(new DividerItemDecoration(parent, LinearLayoutManager.VERTICAL));
        }
        parent.runOnUiThread(dialog::show);
    }

    abstract protected void prepareList(long conflictID);
    abstract protected void inflateViews(T conflict, long conflictID);
    abstract protected void setTextViews(T conflict);

    @CallSuper
    protected void findGlobalViews(Dialog dialog) {
        textExists = dialog.findViewById(R.id.dialog_conflict_text);
        textRelations = dialog.findViewById(R.id.dialog_conflict_text_relations);
        conflictItem = dialog.findViewById(R.id.dialog_conflict_item);
        cancelButton = dialog.findViewById(R.id.dialog_conflict_cancel);
        overrideButton = dialog.findViewById(R.id.dialog_conflict_override);
        list = dialog.findViewById(R.id.dialog_conflict_relationlist);
    }

    private void setupButtons(Dialog dialog, OverrideListener onOverrideListener) {
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        overrideButton.setOnClickListener(v -> {
            dialog.dismiss();
            onOverrideListener.onOverride();
        });
    }

}
