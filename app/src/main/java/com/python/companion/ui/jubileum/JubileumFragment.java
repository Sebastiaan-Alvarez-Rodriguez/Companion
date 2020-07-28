package com.python.companion.ui.jubileum;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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
import com.python.companion.backend.measurement.MeasurementRepository;
import com.python.companion.db.constant.MeasurementQuery;
import com.python.companion.db.entity.Measurement;
import com.python.companion.db.pojo.measurement.MeasurementWithParentNames;
import com.python.companion.ui.MainActivity;
import com.python.companion.ui.jubileum.activity.JubileumEditActivity;
import com.python.companion.ui.jubileum.adapter.MeasurementSortHandler;
import com.python.companion.ui.jubileum.adapter.item.MeasurementItem;
import com.python.companion.ui.notes.note.adapter.NoteSortHandler;

import java.util.List;
import java.util.stream.Collectors;

public class JubileumFragment extends Fragment implements ActionMode.Callback {

    private MeasurementViewModel jubileumViewModel;

    private RecyclerView list;
    private SearchView searchView;
    private FloatingActionButton fab;

    private ItemAdapter<MeasurementItem> itemAdapter;
    private FastAdapter<MeasurementItem> fastAdapter;
    private SelectExtension<MeasurementItem> selectionExtension;
    private ActionModeHelper<MeasurementItem> actionModeHelper;
    private MeasurementSortHandler sortHandler;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        jubileumViewModel = new ViewModelProvider(this).get(MeasurementViewModel.class);
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
            Intent intent = new Intent(getContext(), JubileumEditActivity.class);
            startActivity(intent);
        });
    }

    private void prepareList(View view) {
        ComparableItemListImpl<MeasurementItem> itemList = new ComparableItemListImpl<>((o1, o2) -> 0);

        itemAdapter = new ItemAdapter<>(itemList);
        sortHandler = new MeasurementSortHandler.Builder()
                .setStrategy(view.getContext().getSharedPreferences(getString(R.string.measurement_preferences), Context.MODE_PRIVATE).getInt("MeasurementSort", MeasurementSortHandler.SORT_ALPHA))
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

        fastAdapter.setOnClickListener((view1, measurementItemIAdapter, measurementItem, position) -> {
            if (!actionModeHelper.isActive()) {
                //TODO: Do something on a regular click
                Measurement m = measurementItem.getMeasurement();
                Log.e("JubileumFragment", "Measurement "+m.getNameSingular()+"(id "+m.getMeasurementID()+"): parent "+m.getParentID()+", nameSingular "+measurementItem.getParentSingular());
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


        actionModeHelper = new ActionModeHelper<>(fastAdapter, R.menu.fragment_jubileum_action, this);
        setListUpdates();
    }

    private void setListUpdates() {
        jubileumViewModel.getMeasurements().observe(getViewLifecycleOwner(), measurements -> {
            List<MeasurementItem> newlist = measurements.stream().map(MeasurementItem::new).collect(Collectors.toList());
            FastAdapterDiffUtil.INSTANCE.set(itemAdapter, newlist, new DiffCallback<MeasurementItem>() {
                @Override
                public boolean areItemsTheSame(MeasurementItem oldItem, MeasurementItem newItem) {
                    return oldItem.getMeasurement().getMeasurementID() == newItem.getMeasurement().getMeasurementID();
                }

                @Override
                public boolean areContentsTheSame(MeasurementItem oldItem, MeasurementItem newItem) {
                    Measurement old = oldItem.getMeasurement(), cur = newItem.getMeasurement();
                    return old.getNameSingular().equals(cur.getNameSingular())
                            && old.getNamePlural().equals(cur.getNamePlural())
                            && old.getAmount() == cur.getAmount()
                            && old.getPrecomputedamount() == cur.getPrecomputedamount()
                            && old.getParentID() == cur.getParentID() // To get update if parent name changes, must ensure parent gets a different ID on update
                            && old.getDuration() == cur.getDuration();
                }

                @Nullable
                @Override
                public Object getChangePayload(MeasurementItem oldItem, int oldPosition, MeasurementItem newItem, int newPosition) {
                    return null;
                }
            });
        });
    }

    private void setListFiltering() {
        itemAdapter.getItemFilter().setFilterPredicate((measurementItem, sequence) -> {
            Measurement m = measurementItem.getMeasurement();
            final CharSequence lsequence = sequence.toString().toLowerCase();
            return m.getNameSingular().toLowerCase().contains(lsequence) || m.getNamePlural().toLowerCase().contains(lsequence);
        });
        itemAdapter.getItemFilter().setItemFilterListener(new ItemFilterListener<MeasurementItem>() {
            @Override
            public void itemsFiltered(@Nullable CharSequence charSequence, @Nullable List<? extends MeasurementItem> list) {}

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
        inflater.inflate(R.menu.fragment_jubileum, menu);
        searchView = (SearchView) menu.findItem(R.id.fragment_jubileum_search).getActionView();
        setListFiltering();
        @IdRes int id;
        if (getContext().getSharedPreferences(getString(R.string.measurement_preferences), Context.MODE_PRIVATE).getInt("jubileumSort", NoteSortHandler.SORT_DATE) == MeasurementSortHandler.SORT_ALPHA)
            id = R.id.fragment_jubileum_sort_alpha;
        else
            id = R.id.fragment_jubileum_sort_duration;
        MenuItem item = menu.findItem(id);
        item.setChecked(true);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        @MeasurementSortHandler.MeasurementSortStrategy int strategy;
        switch (item.getItemId()) {
            case R.id.fragment_jubileum_sort_alpha:
                strategy = MeasurementSortHandler.SORT_ALPHA;
                break;
            case R.id.fragment_jubileum_sort_duration:
                strategy = MeasurementSortHandler.SORT_DURATION;
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        sortHandler.setSortStrategy(strategy);
        item.setChecked(true);
        SharedPreferences.Editor editor = getContext().getSharedPreferences(getString(R.string.measurement_preferences), Context.MODE_PRIVATE).edit();
        editor.putInt("jubileumSort", strategy).apply();
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
        if (item.getItemId() == R.id.fragment_jubileum_action_delete) {
//            mUndoHelper.remove(findViewById(android.R.id.content), "Item removed", "Undo", Snackbar.LENGTH_LONG, selectExtension.selections)
            final MeasurementQuery measurementQuery = new MeasurementQuery(getContext());
            measurementQuery.delete(selectionExtension.getSelectedItems().stream().map(MeasurementItem::getMeasurement).collect(Collectors.toList()), () -> {});
            mode.finish();
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
    }

    public static class MeasurementViewModel extends AndroidViewModel {
        private MeasurementRepository measurementRepository;

        private LiveData<List<MeasurementWithParentNames>> measurements = null;

        public MeasurementViewModel(@NonNull Application application) {
            super(application);
            measurementRepository = new MeasurementRepository(application);
        }


        public LiveData<List<MeasurementWithParentNames>> getMeasurements() {
            if (measurements == null)
                measurements = measurementRepository.getMeasurementsNamed();
            return measurements;
        }
    }
}