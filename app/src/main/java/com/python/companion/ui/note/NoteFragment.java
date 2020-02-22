package com.python.companion.ui.note;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.mikepenz.fastadapter.diff.DiffCallback;
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil;
import com.mikepenz.fastadapter.extensions.ExtensionsFactories;
import com.mikepenz.fastadapter.helpers.ActionModeHelper;
import com.mikepenz.fastadapter.listeners.ItemFilterListener;
import com.mikepenz.fastadapter.select.SelectExtension;
import com.mikepenz.fastadapter.select.SelectExtensionFactory;
import com.mikepenz.fastadapter.utils.ComparableItemListImpl;
import com.python.companion.MainActivity;
import com.python.companion.R;
import com.python.companion.db.constant.NoteQuery;
import com.python.companion.db.entity.Note;
import com.python.companion.ui.note.activity.NoteViewActivity;
import com.python.companion.ui.note.activity.edit.note.NoteEditActivity;
import com.python.companion.ui.note.adapter.NoteItem;
import com.python.companion.ui.note.adapter.NoteSortHandler;
import com.python.companion.ui.note.dialog.set.CategorySetDialog;

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

    private RecyclerView list;
    private SearchView searchView;
    private FloatingActionButton fab;

    private ItemAdapter<NoteItem> itemAdapter;
    private FastAdapter<NoteItem> fastAdapter;
    private SelectExtension<NoteItem> selectionExtension;
    private ActionModeHelper<NoteItem> actionModeHelper;
    private NoteSortHandler sortHandler;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
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
        prepareAdd();
        prepareList(view);
    }

    private void findViews(View view) {
        list = view.findViewById(R.id.fragment_list_list);
        fab = view.findViewById(R.id.fragment_list_add);
    }


    private void prepareAdd() {
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), NoteEditActivity.class);
            startActivity(intent);
        });
    }

    private void prepareList(View view) {
        ComparableItemListImpl<NoteItem> itemList = new ComparableItemListImpl<>((o1, o2) -> 0);

        itemAdapter = new ItemAdapter<>(itemList);
        sortHandler = new NoteSortHandler.Builder().setItemList(itemList).build();
        fastAdapter = FastAdapter.with(itemAdapter);
        ExtensionsFactories.INSTANCE.register(new SelectExtensionFactory());
        list.setAdapter(fastAdapter);
        list.setLayoutManager(new LinearLayoutManager(view.getContext()));
        list.addItemDecoration(new DividerItemDecoration(view.getContext(), LinearLayoutManager.VERTICAL));


        selectionExtension = fastAdapter.getOrCreateExtension(SelectExtension.class);
        selectionExtension.setSelectable(true);
        selectionExtension.setMultiSelect(true);
        selectionExtension.setSelectOnLongClick(true);

        fastAdapter.setOnPreClickListener((view1, noteItemIAdapter, noteItem, integer) -> {
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

        fastAdapter.setOnPreLongClickListener((view1, noteItemIAdapter, noteItem, position) -> {
            ActionMode actionMode = actionModeHelper.onLongClick((MainActivity) getActivity(), position);
            if (actionMode != null)
                getActivity().findViewById(R.id.action_mode_bar).setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
            return actionMode != null;
        });


        actionModeHelper = new ActionModeHelper<>(fastAdapter, R.menu.fragment_note_action, this);
        setListUpdates();
    }

    private void setListUpdates() {
        noteViewModel.getNotes().observe(getViewLifecycleOwner(), notes -> {
            List<NoteItem> newlist = notes.stream().map(note -> {
                NoteItem item = new NoteItem();
                item.setNote(note);
                return item;
            }).collect(Collectors.toList());
            FastAdapterDiffUtil.INSTANCE.set(itemAdapter, newlist, new DiffCallback<NoteItem>() {
                @Override
                public boolean areItemsTheSame(NoteItem oldItem, NoteItem newItem) {
                    return oldItem.getNote().getName().equals(newItem.getNote().getName());
                }

                @Override
                public boolean areContentsTheSame(NoteItem oldItem, NoteItem newItem) {
                    Note oldNote = oldItem.getNote(), newNote = newItem.getNote();
                    return oldNote.getModified().equals(newNote.getModified()) && oldNote.getCategory().getCategoryColor() == newNote.getCategory().getCategoryColor();
                }

                @Nullable
                @Override
                public Object getChangePayload(NoteItem oldItem, int oldPosition, NoteItem newItem, int newPosition) {
                    Note oldNote = oldItem.getNote(), newNote = newItem.getNote();
                    if (!oldNote.getModified().equals(newNote.getModified()))
                        return newNote.getModified();
                    else
                        return newNote.getCategory().getCategoryColor();
                }
            });

        });
    }

    private void setListFiltering() {
        itemAdapter.getItemFilter().setFilterPredicate((noteItem, charSequence) -> noteItem.getNote().getName().toLowerCase().contains(charSequence.toString().toLowerCase()));
        itemAdapter.getItemFilter().setItemFilterListener(new ItemFilterListener<NoteItem>() {
            @Override
            public void itemsFiltered(@Nullable CharSequence charSequence, @Nullable List<? extends NoteItem> list) {
            }

            @Override
            public void onReset() {//TODO: Bug: Set sorting to alpha. Type 'oof' in searchview. Set sorting to date. Press back arrow on searchview. Sort mode says date, is alpha.
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //TODO scroll to new item and highlight it?
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_note, menu);
        MenuItem item = menu.findItem(sortHandler.getSortStrategy() == NoteSortHandler.SORT_ALPHA ? R.id.fragment_note_menu_sort_alpha : R.id.fragment_note_menu_sort_date);
        item.setChecked(true);
        searchView = (SearchView) menu.findItem(R.id.fragment_note_menu_search).getActionView();
        setListFiltering();
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.fragment_note_menu_sort_alpha:
                sortHandler.setSortStrategy(NoteSortHandler.SORT_ALPHA);
                item.setChecked(true);
                break;
            case R.id.fragment_note_menu_sort_date:
                sortHandler.setSortStrategy(NoteSortHandler.SORT_DATE);
                item.setChecked(true);
                break;
        }
        return super.onOptionsItemSelected(item);
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
                categorySetDialog.show(getChildFragmentManager(), null);
                break;
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
    }
}