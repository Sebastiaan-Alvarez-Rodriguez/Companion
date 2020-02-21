package com.python.companion.ui.note;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
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
import com.mikepenz.fastadapter.listeners.ItemFilterListener;
import com.mikepenz.fastadapter.select.SelectExtension;
import com.mikepenz.fastadapter.select.SelectExtensionFactory;
import com.mikepenz.fastadapter.utils.ComparableItemListImpl;
import com.python.companion.MainActivity;
import com.python.companion.R;
import com.python.companion.db.constant.NoteQuery;
import com.python.companion.ui.note.activity.NoteViewActivity;
import com.python.companion.ui.note.activity.edit.note.NoteEditActivity;
import com.python.companion.ui.note.adapter.NoteItem;
import com.python.companion.ui.note.adapter.NoteSortHandler;
import com.python.companion.ui.note.dialog.CategorySetDialog;

import java.util.List;
import java.util.stream.Collectors;

// https://github.com/noties/Markwon
//    https://noties.io/Markwon/
//    https://github.com/noties/Markwon/blob/master/sample/src/main/java/io/noties/markwon/sample/latex/LatexActivity.java


// Color picking: https://github.com/martin-stone/hsv-alpha-color-picker-android
// Now for menu redesign
//    https://stackoverflow.com/questions/21329132/android-custom-dropdown-popup-menu
public class NoteFragment extends Fragment implements ActionMode.Callback {

    private NoteViewModel noteViewModel;

    private ImageView sortButton, addButton;
    private RecyclerView list;
    private SearchView searchView;

    private FastAdapter<NoteItem> fastAdapter;
    private SelectExtension<NoteItem> selectionExtension;
    private ActionModeHelper<NoteItem> actionModeHelper;
    private NoteSortHandler sortHandler;


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
        prepareSort();
        prepareAdd();
    }

    private void findViews(View view) {
        sortButton = view.findViewById(R.id.fragment_list_sort);
        addButton = view.findViewById(R.id.fragment_list_add);
        list = view.findViewById(R.id.fragment_list_list);
        searchView = view.findViewById(R.id.fragment_list_searchview);

    }

    private void prepareSort() {
        registerForContextMenu(sortButton);
        sortButton.setOnClickListener(v -> sortButton.showContextMenu(sortButton.getX(), sortButton.getY()));
    }

    private void prepareAdd() {
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), NoteEditActivity.class);
            startActivity(intent);
        });
    }

    private void prepareList(View view) {
        ComparableItemListImpl<NoteItem> itemList = new ComparableItemListImpl<>(null);

        ItemAdapter<NoteItem> itemAdapter = new ItemAdapter<>(itemList);
        sortHandler = new NoteSortHandler.Builder().setStrategy(NoteSortHandler.SORT_ALPHA).setItemList(itemList).build();
        fastAdapter = FastAdapter.with(itemAdapter);
        ExtensionsFactories.INSTANCE.register(new SelectExtensionFactory());
        list.setAdapter(fastAdapter);
        list.setLayoutManager(new LinearLayoutManager(view.getContext()));
        list.addItemDecoration(new DividerItemDecoration(view.getContext(), LinearLayoutManager.VERTICAL));


        selectionExtension = fastAdapter.getOrCreateExtension(SelectExtension.class);
        selectionExtension.setSelectable(true);
        selectionExtension.setMultiSelect(true);
        selectionExtension.setSelectOnLongClick(true);
        selectionExtension.setSelectionListener((item, b) -> {
            Log.i("Superb", "Selection of item " + item.getNote().getName() + " changed to: " + b);
//            item.setSelected(b);
        });

        fastAdapter.setOnPreClickListener((view12, noteItemIAdapter, noteItem, integer) -> {
            Log.i("OnPreClick", "Tick: " + actionModeHelper.isActive());
            Boolean res = actionModeHelper.onClick(noteItem);
            return res != null ? res : false;
        });

        fastAdapter.setOnClickListener((view1, noteItemIAdapter, noteItem, position) -> {
            Log.i("OnClick", "Tick: " + actionModeHelper.isActive());
            Log.i("OnClick", "SelectedCount: " + selectionExtension.getSelections().size());
            if (!actionModeHelper.isActive()) {
                Intent intent = new Intent(getContext(), NoteViewActivity.class);
                intent.putExtra("name", noteItem.getNote().getName());
                intent.putExtra("content", noteItem.getNote().getContent());
                startActivity(intent);
            } else {
                fastAdapter.notifyItemChanged(position);
            }
            return true;
        });


        fastAdapter.setOnPreLongClickListener((view13, noteItemIAdapter, noteItem, position) -> {
            ActionMode actionMode = actionModeHelper.onLongClick((MainActivity) getActivity(), position);
            if (actionMode != null)
                getActivity().findViewById(R.id.action_mode_bar).setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
            return actionMode != null;
        });


        actionModeHelper = new ActionModeHelper<>(fastAdapter, R.menu.fragment_note_action, this);

        noteViewModel.getNotes().observe(getViewLifecycleOwner(), notes -> itemAdapter.set(notes.stream().map(note -> {
            NoteItem item = new NoteItem();
            item.setNote(note);
            return item;
        }).collect(Collectors.toList())));

        // Filtering
        itemAdapter.getItemFilter().setFilterPredicate((noteItem, charSequence) -> noteItem.getNote().getName().toLowerCase().contains(charSequence.toString().toLowerCase()));
        itemAdapter.getItemFilter().setItemFilterListener(new ItemFilterListener<NoteItem>() {
            @Override
            public void itemsFiltered(@org.jetbrains.annotations.Nullable CharSequence charSequence, @org.jetbrains.annotations.Nullable List<? extends NoteItem> list) {

            }

            @Override
            public void onReset() {

            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                itemAdapter.filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                itemAdapter.filter(query);
                return false;
            }
        });
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
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenu.ContextMenuInfo menuInfo) {
        Log.i("Context", "Inflate");
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.fragment_note_context, menu);
        MenuItem item = menu.findItem(sortHandler.getSortStrategy() == NoteSortHandler.SORT_ALPHA ? R.id.fragment_note_context_menu_alpha : R.id.fragment_note_context_menu_date);
        item.setChecked(true);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.fragment_note_context_menu_alpha:
                sortHandler.setSortStrategy(NoteSortHandler.SORT_ALPHA);
                break;
            case R.id.fragment_note_context_menu_date:
                sortHandler.setSortStrategy(NoteSortHandler.SORT_DATE);
                break;
        }
        item.setChecked(true);
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuItem deleteItem = menu.findItem(R.id.menu_context_note_delete);
        MenuItem categoryItem = menu.findItem(R.id.menu_context_note_update_category);
        deleteItem.getIcon().setColorFilter(getResources().getColor(R.color.colorWindowBackground, null), PorterDuff.Mode.SRC_IN);
        categoryItem.getIcon().setColorFilter(getResources().getColor(R.color.colorWindowBackground, null), PorterDuff.Mode.SRC_IN);
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
                //            mUndoHelper.remove(findViewById(android.R.id.content), "Item removed", "Undo", Snackbar.LENGTH_LONG, selectExtension.selections)
                final NoteQuery noteQuery = new NoteQuery(getContext());
                noteQuery.delete(selectionExtension.getSelectedItems().stream().map(NoteItem::getNote).collect(Collectors.toList()), x -> {
                });
                mode.finish();
                break;
            case R.id.menu_context_note_update_category:
                CategorySetDialog categorySetDialog = new CategorySetDialog.Builder()
                        .setSelectedNotes(selectionExtension.getSelectedItems())
                        .setFinishListener(mode::finish).build();
                categorySetDialog.show(getChildFragmentManager(), null); //https://guides.codepath.com/android/using-dialogfragment
                break;
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
    }
}