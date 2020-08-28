package com.python.companion.ui.anniversary;

import android.app.Activity;
import android.app.Application;
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
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
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
import com.python.companion.R;
import com.python.companion.db.entity.Anniversary;
import com.python.companion.db.interact.AnniversaryStore;
import com.python.companion.db.pojo.anniversary.AnniversaryWithParentNames;
import com.python.companion.db.repository.AnniversaryRepository;
import com.python.companion.ui.MainActivity;
import com.python.companion.ui.anniversary.activity.AnniversaryEditActivity;
import com.python.companion.ui.anniversary.activity.AnniversarySelectActivity;
import com.python.companion.ui.anniversary.activity.AnniversaryViewActivity;
import com.python.companion.ui.anniversary.activity.calculate.AnniversaryCalculatorActivity;
import com.python.companion.ui.anniversary.activity.calculate.AnniversaryCalculatorSharedActivity;
import com.python.companion.ui.anniversary.adapter.AnniversarySortHandler;
import com.python.companion.ui.anniversary.adapter.item.AnniversaryItem;

import java.util.List;
import java.util.stream.Collectors;

public class AnniversaryFragment extends Fragment implements ActionMode.Callback {
    private int REQ_SELECT = 1;

    private AnniversaryViewModel anniversaryViewModel;

    private RecyclerView list;
    private SearchView searchView;
    private FloatingActionButton calculatorSharedButton, calculatorButton, addButton;

    private ItemAdapter<AnniversaryItem> itemAdapter;
    private FastAdapter<AnniversaryItem> fastAdapter;
    private SelectExtension<AnniversaryItem> selectionExtension;
    private ActionModeHelper<AnniversaryItem> actionModeHelper;
    private AnniversarySortHandler sortHandler;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        anniversaryViewModel = new ViewModelProvider(this).get(AnniversaryViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_anniversary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findViews(view);
        prepareButtons();
        prepareList(view);
    }

    private void findViews(View view) {
        list = view.findViewById(R.id.fragment_anniversary_list);
        addButton = view.findViewById(R.id.fragment_anniversary_add);
        calculatorButton = view.findViewById(R.id.fragment_anniversary_jcalculator);
        calculatorSharedButton = view.findViewById(R.id.fragment_anniversary_jcalculator_shared);
    }

    private void prepareButtons() {
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AnniversaryEditActivity.class);
            startActivity(intent);
        });

        calculatorButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AnniversaryCalculatorActivity.class);
            startActivity(intent);
        });

        calculatorSharedButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AnniversarySelectActivity.class);
            startActivityForResult(intent, REQ_SELECT);
        });
    }

    private void prepareList(View view) {
        ComparableItemListImpl<AnniversaryItem> itemList = new ComparableItemListImpl<>((o1, o2) -> 0);

        itemAdapter = new ItemAdapter<>(itemList);
        sortHandler = new AnniversarySortHandler.Builder()
                .setStrategy(view.getContext().getSharedPreferences(getString(R.string.anniversary_preferences), Context.MODE_PRIVATE).getInt("AnniversarySort", AnniversarySortHandler.SORT_ALPHA))
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

        fastAdapter.setOnClickListener((view1, anniversaryItemIAdapter, anniversaryItem, position) -> {
            if (!actionModeHelper.isActive()) {
                Intent intent = new Intent(getContext(), AnniversaryViewActivity.class);
                intent.putExtra("anniversary", new AnniversaryContainer(anniversaryItem.getAnniversary()));
                intent.putExtra("parentSingular", anniversaryItem.getParentSingular());
                intent.putExtra("parentPlural", anniversaryItem.getParentPlural());
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


        actionModeHelper = new ActionModeHelper<>(fastAdapter, R.menu.fragment_anniversary_action, this);
        setListUpdates();
    }

    private void setListUpdates() {
        anniversaryViewModel.getAnniversarys().observe(getViewLifecycleOwner(), anniversaries -> {
            List<AnniversaryItem> list = anniversaries.parallelStream().map(AnniversaryItem::new).collect(Collectors.toList());

            FastAdapterDiffUtil.INSTANCE.set(itemAdapter, list, new DiffCallback<AnniversaryItem>() {
                @Override
                public boolean areItemsTheSame(AnniversaryItem oldItem, AnniversaryItem newItem) {
                    return oldItem.getAnniversary().getAnniversaryID() == newItem.getAnniversary().getAnniversaryID();
                }

                @Override
                public boolean areContentsTheSame(AnniversaryItem oldItem, AnniversaryItem newItem) {
                    Anniversary old = oldItem.getAnniversary(), cur = newItem.getAnniversary();
                    return old.getNameSingular().equals(cur.getNameSingular())
                            && old.getNamePlural().equals(cur.getNamePlural())
                            && old.getAmount() == cur.getAmount()
                            && old.getPrecomputedamount() == cur.getPrecomputedamount()
                            && old.getParentID() == cur.getParentID() // To get update if parent name changes, must ensure parent gets a different ID on update
                            && old.getDuration() == cur.getDuration();
                }

                @Nullable
                @Override
                public Object getChangePayload(AnniversaryItem oldItem, int oldPosition, AnniversaryItem newItem, int newPosition) {
                    return null;
                }
            });
        });
    }

    private void setListFiltering() {
        itemAdapter.getItemFilter().setFilterPredicate((anniversaryItem, sequence) -> {
            Anniversary m = anniversaryItem.getAnniversary();
            final CharSequence lsequence = sequence.toString().toLowerCase();
            return m.getNameSingular().toLowerCase().contains(lsequence) || m.getNamePlural().toLowerCase().contains(lsequence);
        });
        itemAdapter.getItemFilter().setItemFilterListener(new ItemFilterListener<AnniversaryItem>() {
            @Override
            public void itemsFiltered(@Nullable CharSequence charSequence, @Nullable List<? extends AnniversaryItem> list) {}

            @Override
            public void onReset() {}
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
        inflater.inflate(R.menu.fragment_anniversary, menu);
        searchView = (SearchView) menu.findItem(R.id.fragment_anniversary_search).getActionView();
        setListFiltering();
        @IdRes int id;
        @AnniversarySortHandler.AnniversarySortStrategy int strategy = searchView.getContext().getSharedPreferences(getString(R.string.anniversary_preferences), Context.MODE_PRIVATE).getInt("AnniversarySort", AnniversarySortHandler.SORT_ALPHA);
        if (strategy == AnniversarySortHandler.SORT_ALPHA)
            id = R.id.fragment_anniversary_sort_alpha;
        else
            id = R.id.fragment_anniversary_sort_duration;
        MenuItem item = menu.findItem(id);
        item.setChecked(true);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        @AnniversarySortHandler.AnniversarySortStrategy int strategy;
        switch (item.getItemId()) {
            case R.id.fragment_anniversary_sort_alpha:
                strategy = AnniversarySortHandler.SORT_ALPHA;
                break;
            case R.id.fragment_anniversary_sort_duration:
                strategy = AnniversarySortHandler.SORT_DURATION;
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        sortHandler.setSortStrategy(strategy);
        item.setChecked(true);
        SharedPreferences.Editor editor = getContext().getSharedPreferences(getString(R.string.anniversary_preferences), Context.MODE_PRIVATE).edit();
        editor.putInt("anniversarySort", strategy).apply();
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
        if (item.getItemId() == R.id.fragment_anniversary_action_delete) {
//            mUndoHelper.remove(findViewById(android.R.id.content), "Item removed", "Undo", Snackbar.LENGTH_LONG, selectExtension.selections)
            AnniversaryStore.delete(selectionExtension.getSelectedItems().stream().map(AnniversaryItem::getAnniversary).collect(Collectors.toList()), getContext(), () -> {});
            mode.finish();
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQ_SELECT && resultCode == Activity.RESULT_OK && data != null) {
            Intent intent = new Intent(getContext(), AnniversaryCalculatorSharedActivity.class);
            intent.putParcelableArrayListExtra("chosen", data.getParcelableArrayListExtra("chosen"));
            startActivity(intent);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public static class AnniversaryViewModel extends AndroidViewModel {
        private AnniversaryRepository anniversaryRepository;

        private LiveData<List<AnniversaryWithParentNames>> anniversaries = null;

        public AnniversaryViewModel(@NonNull Application application) {
            super(application);
            anniversaryRepository = new AnniversaryRepository(application);
        }


        public LiveData<List<AnniversaryWithParentNames>> getAnniversarys() {
            if (anniversaries == null)
                anniversaries = anniversaryRepository.getAnniversarysNamed();
            return anniversaries;
        }
    }
}