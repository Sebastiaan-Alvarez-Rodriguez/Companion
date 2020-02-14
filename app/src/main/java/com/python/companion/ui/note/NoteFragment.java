package com.python.companion.ui.note;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.mikepenz.fastadapter.extensions.ExtensionsFactories;
import com.mikepenz.fastadapter.helpers.ActionModeHelper;
import com.mikepenz.fastadapter.select.SelectExtension;
import com.mikepenz.fastadapter.select.SelectExtensionFactory;
import com.python.companion.MainActivity;
import com.python.companion.R;
import com.python.companion.db.constant.NoteQuery;
import com.python.companion.ui.note.activity.NoteViewActivity;
import com.python.companion.ui.note.adapter.NoteItem;
import com.python.companion.ui.note.dialog.CategorySetDialog;

import java.util.stream.Collectors;

// https://github.com/noties/Markwon
//    https://noties.io/Markwon/
//    https://github.com/noties/Markwon/blob/master/sample/src/main/java/io/noties/markwon/sample/latex/LatexActivity.java


// Color picking: https://github.com/martin-stone/hsv-alpha-color-picker-android
// Now for menu redesign
//    https://stackoverflow.com/questions/21329132/android-custom-dropdown-popup-menu
public class NoteFragment extends Fragment implements ActionMode.Callback {

    private NoteViewModel noteViewModel;
    private RecyclerView list;
    private FastAdapter<NoteItem> fastAdapter;
    private SelectExtension<NoteItem> selectionExtension;
    private ActionModeHelper<NoteItem> actionModeHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findViews(view);
        prepareList(view);
    }

    private void findViews(View view) {
        list = view.findViewById(R.id.list);
    }

    private void prepareList(View view) {
        ItemAdapter<NoteItem> itemAdapter = new ItemAdapter<>();
        fastAdapter = FastAdapter.with(itemAdapter);
        ExtensionsFactories.INSTANCE.register(new SelectExtensionFactory());
        list.setAdapter(fastAdapter);
        list.setLayoutManager(new LinearLayoutManager(view.getContext()));
        list.addItemDecoration(new DividerItemDecoration(view.getContext(), LinearLayoutManager.VERTICAL));


        selectionExtension = fastAdapter.getOrCreateExtension(SelectExtension.class);
        assert selectionExtension != null;
        selectionExtension.setSelectable(true);
        selectionExtension.setMultiSelect(true);
        selectionExtension.setSelectOnLongClick(true);
        selectionExtension.setSelectionListener((item, b) -> {
            Log.i("Superb", "Selection of item "+item.getNote().getName()+" changed to: "+b);
//            item.setSelected(b);
        });

//        fastAdapter.addEventHook(new NoteItem.CheckBoxClickEvent());

        fastAdapter.setOnPreClickListener((view12, noteItemIAdapter, noteItem, integer) -> {
            Log.i("OnPreClick", "Tick: "+actionModeHelper.isActive());
            Boolean res = actionModeHelper.onClick(noteItem);
            return res != null ? res : false;
        });

        fastAdapter.setOnClickListener((view1, noteItemIAdapter, noteItem, position) -> {
            Log.i("OnClick", "Tick: "+actionModeHelper.isActive());
            Log.i("OnClick", "SelectedCount: " + selectionExtension.getSelections().size());
            if (!actionModeHelper.isActive()) {
                Intent intent = new Intent(getContext(), NoteViewActivity.class);
                intent.putExtra("name", noteItem.getNote().getName());
                intent.putExtra("content", noteItem.getNote().getContent());
                startActivity(intent);
            } else {
                fastAdapter.notifyItemChanged(position);
            }
            return true; //orig: false
        });


        fastAdapter.setOnPreLongClickListener((view13, noteItemIAdapter, noteItem, position) -> {
            ActionMode actionMode = actionModeHelper.onLongClick((MainActivity) getActivity(), position);
            if (actionMode != null) {
                //we want color our CAB
                getActivity().findViewById(R.id.action_mode_bar).setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
            }
            //if we have no actionMode we do not consume the event
            return actionMode != null;
        });


        actionModeHelper = new ActionModeHelper<>(fastAdapter, R.menu.context_note, this);

        noteViewModel.getNotes().observe(getViewLifecycleOwner(), notes -> itemAdapter.set(notes.stream().map(note -> {
            NoteItem item = new NoteItem();
            item.setNote(note);
            return item;
        }).collect(Collectors.toList())));
    }


//    @Override
//    protected void prepareAdd() {
//        super.prepareAdd();
//        add.setOnClickListener(v -> {
//            Intent intent = new Intent(getContext(), NoteEditActivity.class);
//            startActivityForResult(intent, REQ_ADD);
//        });
//    }
//
//    @Override
//    protected void prepareDelete() {
//        super.prepareDelete();
//        add.setOnClickListener(v -> {
//            final NoteQuery noteQuery = new NoteQuery(getContext());
//            noteQuery.delete(adapter.getSelected(), x -> {});
//        });
//    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //TODO scroll to new item and highlight it?
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_context_note_delete:
                Log.i("CABClick", "Clicked on CAB action item!");
    //            mUndoHelper.remove(findViewById(android.R.id.content), "Item removed", "Undo", Snackbar.LENGTH_LONG, selectExtension.selections)
                final NoteQuery noteQuery = new NoteQuery(getContext());
                noteQuery.delete(selectionExtension.getSelectedItems().stream().map(NoteItem::getNote).collect(Collectors.toList()), x -> {});
                mode.finish();
                break;
            case R.id.menu_context_note_update_category:
                CategorySetDialog categorySetDialog = new CategorySetDialog.Builder().setFinishListener(mode::finish).build();
                categorySetDialog.show(getChildFragmentManager(), null); //https://guides.codepath.com/android/using-dialogfragment
                break;
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {}
}