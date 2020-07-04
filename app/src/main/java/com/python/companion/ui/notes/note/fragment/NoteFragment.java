package com.python.companion.ui.notes.note.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
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
import com.google.android.material.snackbar.Snackbar;
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
import com.python.companion.R;
import com.python.companion.db.constant.CategoryQuery;
import com.python.companion.db.constant.NoteQuery;
import com.python.companion.db.entity.Note;
import com.python.companion.security.DecryptedCallback;
import com.python.companion.security.Guard;
import com.python.companion.ui.MainActivity;
import com.python.companion.ui.general.settings.SettingsActivity;
import com.python.companion.ui.notes.category.dialog.set.CategorySetDialog;
import com.python.companion.ui.notes.note.NoteContainer;
import com.python.companion.ui.notes.note.activity.edit.NoteEditActivity;
import com.python.companion.ui.notes.note.activity.view.NoteViewActivity;
import com.python.companion.ui.notes.note.adapter.NoteItem;
import com.python.companion.ui.notes.note.adapter.NoteSortHandler;

import java.util.List;
import java.util.stream.Collectors;

// https://github.com/noties/Markwon
//    https://noties.io/Markwon/
//    https://github.com/noties/Markwon/blob/master/sample/src/main/java/io/noties/markwon/sample/latex/LatexActivity.java

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
        sortHandler = new NoteSortHandler.Builder()
                .setStrategy(getContext().getSharedPreferences(getString(R.string.note_preferences), Context.MODE_PRIVATE).getInt("noteSort", NoteSortHandler.SORT_DATE))
                .setItemList(itemList)
                .build();
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
            Boolean res = actionModeHelper.onClick(noteItem);
            return res != null ? res : false;
        });

        fastAdapter.setOnClickListener((view1, noteItemIAdapter, noteItem, position) -> {
            if (!actionModeHelper.isActive()) {
                Note n = noteItem.getNote();
                Intent intent = new Intent(getContext(), NoteViewActivity.class);
                if (n.isSecure()) {
                    Guard.getGuard().decrypt(n.getContent(), n.getIv(), n.getName(), getChildFragmentManager(), getContext(), new DecryptedCallback() {
                        @Override
                        public void onFinish(@NonNull String plaintext) {
//                            intent.putExtra("note", new NoteContainer(new Note(n.getName(), plaintext, n.getCategory(), true, n.getIv(), n.getType())));
                            intent.putExtra("note", new NoteContainer(n));
                            intent.putExtra("plaintext", plaintext);
                            startActivity(intent);
                        }
                        @Override
                        public void onFailure() {
                            Snackbar.make(list, "Could not unlock note", Snackbar.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    intent.putExtra("note", new NoteContainer(n));
                    startActivity(intent);
                }
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
                    return oldNote.getModified().equals(newNote.getModified())
                            && oldNote.getCategory().getCategoryName().equals(newNote.getCategory().getCategoryName())
                            && oldNote.getCategory().getCategoryColor() == newNote.getCategory().getCategoryColor()
                            && oldNote.getType() == newNote.getType()
                            && oldNote.isSecure() == newNote.isSecure() && oldNote.isFavorite() == newNote.isFavorite();
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
            public void onReset() {
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                itemAdapter.getItemFilter().filterItems(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                itemAdapter.getItemFilter().filterItems(query);
                sortHandler.forceReSort();
                return true;
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_note, menu);
        searchView = (SearchView) menu.findItem(R.id.fragment_note_menu_search).getActionView();
        setListFiltering();

        @IdRes int id;
        switch (getContext().getSharedPreferences(getString(R.string.note_preferences), Context.MODE_PRIVATE).getInt("noteSort", NoteSortHandler.SORT_DATE)) {
            case NoteSortHandler.SORT_ALPHA:
                id = R.id.fragment_note_menu_sort_alpha;
                break;
            case NoteSortHandler.SORT_CATEGORY:
                id = R.id.fragment_note_menu_sort_category;
                break;
            case NoteSortHandler.SORT_LOCK:
                id = R.id.fragment_note_menu_sort_lock;
                break;
            case NoteSortHandler.SORT_DATE:
            default:
                id = R.id.fragment_note_menu_sort_date;
        }
        MenuItem item = menu.findItem(id);
        item.setChecked(true);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        @NoteSortHandler.NoteSortStrategy int strategy;
        switch (item.getItemId()) {
            case R.id.fragment_note_menu_sort_alpha:
                strategy = NoteSortHandler.SORT_ALPHA;
                break;
            case R.id.fragment_note_menu_sort_category:
                strategy = NoteSortHandler.SORT_CATEGORY;
                break;
            case R.id.fragment_note_menu_sort_lock:
                strategy = NoteSortHandler.SORT_LOCK;
                break;
            case R.id.fragment_note_menu_sort_date:
                strategy = NoteSortHandler.SORT_DATE;
                break;
            case R.id.fragment_note_menu_settings:
                Intent intent = new Intent(getContext(), SettingsActivity.class);
                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);
        }
        sortHandler.setSortStrategy(strategy);
        item.setChecked(true);
        SharedPreferences.Editor editor = getContext().getSharedPreferences(getString(R.string.note_preferences), Context.MODE_PRIVATE).edit();
        editor.putInt("noteSort", strategy).apply();
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
            case R.id.menu_fragment_note_action_delete:
                //            mUndoHelper.remove(findViewById(android.R.id.content), "Item removed", "Undo", Snackbar.LENGTH_LONG, selectExtension.selections)
                final NoteQuery noteQuery = new NoteQuery(getContext());
                noteQuery.delete(selectionExtension.getSelectedItems().stream().map(NoteItem::getNote).collect(Collectors.toList()), x -> {
                });
                mode.finish();
                break;
            case R.id.menu_fragment_note_action_update_category:
                new CategoryQuery(getContext()).count(count -> {
                    if (count == 0) {
                        Snackbar.make(list, "You can only use this when there is >1 category. Making a category for 1 note first.", Snackbar.LENGTH_LONG).show();
                    } else {
                        CategorySetDialog categorySetDialog = new CategorySetDialog.Builder()
                                .setSelectedNotes(selectionExtension.getSelectedItems())
                                .setFinishListener(mode::finish).build();
                        categorySetDialog.show(getChildFragmentManager(), null);
                    }
                });
                break;
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
    }
}