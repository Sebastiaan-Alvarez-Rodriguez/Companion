package com.python.companion.ui.cactus.fragment;

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

import com.google.android.material.bottomappbar.BottomAppBar;
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
import com.python.companion.db.constant.MeasurementQuery;
import com.python.companion.db.entity.Measurement;
import com.python.companion.ui.MainActivity;
import com.python.companion.ui.cactus.activity.measurement.MeasurementAddActivity;
import com.python.companion.ui.cactus.measurement.adapter.MeasurementItem;
import com.python.companion.ui.cactus.measurement.adapter.MeasurementSortHandler;

import java.util.List;
import java.util.stream.Collectors;

public class CactusFragment extends Fragment implements ActionMode.Callback {
    private RecyclerView list;
    private SearchView searchView;
    private FloatingActionButton fab;
    private BottomAppBar bar;

    private ItemAdapter<MeasurementItem> itemAdapter;
    private FastAdapter<MeasurementItem> fastAdapter;
    private SelectExtension<MeasurementItem> selectionExtension;
    private ActionModeHelper<MeasurementItem> actionModeHelper;
    private MeasurementSortHandler sortHandler;

    private CactusViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        viewModel = new ViewModelProvider(this).get(CactusViewModel.class);
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
            Intent intent = new Intent(getContext(), MeasurementAddActivity.class);
            startActivity(intent);
        });
    }

    private void prepareList(View view) {
        ComparableItemListImpl<MeasurementItem> itemList = new ComparableItemListImpl<>((o1, o2) -> 0);

        itemAdapter = new ItemAdapter<>(itemList);
        sortHandler = new MeasurementSortHandler.Builder()
                .setStrategy(getContext().getSharedPreferences(getString(R.string.measurement_preferences), Context.MODE_PRIVATE).getInt("MeasurementSort", MeasurementSortHandler.SORT_DURATION))
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

        fastAdapter.setOnPreClickListener((view1, MeasurementItemIAdapter, MeasurementItem, integer) -> {
            Boolean res = actionModeHelper.onClick(MeasurementItem);
            return res != null ? res : false;
        });

        fastAdapter.setOnClickListener((view1, MeasurementItemIAdapter, MeasurementItem, position) -> {
            if (!actionModeHelper.isActive()) {
//                    Intent intent = new Intent(getContext(), MeasurementViewActivity.class);
//                    intent.putExtra("name", MeasurementItem.getMeasurement().getName());
//                    intent.putExtra("duration", MeasurementItem.getMeasurement().getDuration());
//                    startActivity(intent);
            } else {
                fastAdapter.notifyItemChanged(position);
            }
            return true;
        });

        fastAdapter.setOnPreLongClickListener((view1, MeasurementItemIAdapter, MeasurementItem, position) -> {
            ActionMode actionMode = actionModeHelper.onLongClick((MainActivity) getActivity(), position);
            if (actionMode != null)
                getActivity().findViewById(R.id.action_mode_bar).setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
            return actionMode != null;
        });


        actionModeHelper = new ActionModeHelper<>(fastAdapter, R.menu.fragment_cactus_action, this);
        setListUpdates();
    }

    private void setListUpdates() {
        viewModel.getMeasurements().observe(getViewLifecycleOwner(), Measurements -> {
            List<MeasurementItem> newlist = Measurements.stream().map(Measurement -> {
                MeasurementItem item = new MeasurementItem();
                item.setMeasurement(Measurement);
                return item;
            }).collect(Collectors.toList());
            FastAdapterDiffUtil.INSTANCE.set(itemAdapter, newlist, new DiffCallback<MeasurementItem>() {
                @Override
                public boolean areItemsTheSame(MeasurementItem oldItem, MeasurementItem newItem) {
                    return oldItem.getMeasurement().getNameSingular().equals(newItem.getMeasurement().getNameSingular());
                }

                @Override
                public boolean areContentsTheSame(MeasurementItem oldItem, MeasurementItem newItem) {
                    return oldItem.getMeasurement().getDuration().equals(newItem.getMeasurement().getDuration());
                }

                @Nullable
                @Override
                public Object getChangePayload(MeasurementItem oldItem, int oldPosition, MeasurementItem newItem, int newPosition) {
                    Measurement oldMeasurement = oldItem.getMeasurement(), newMeasurement = newItem.getMeasurement();
                    if (!oldMeasurement.getNameSingular().equals(newMeasurement.getNameSingular()))
                        return newMeasurement.getNameSingular();
                    else
                        return newMeasurement.getDuration();
                }
            });

        });
    }

    private void setListFiltering() {
        itemAdapter.getItemFilter().setFilterPredicate((MeasurementItem, charSequence) -> MeasurementItem.getMeasurement().getNameSingular().toLowerCase().contains(charSequence.toString().toLowerCase()));
        itemAdapter.getItemFilter().setItemFilterListener(new ItemFilterListener<MeasurementItem>() {
            @Override
            public void itemsFiltered(@Nullable CharSequence charSequence, @Nullable List<? extends MeasurementItem> list) {
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
        inflater.inflate(R.menu.fragment_cactus, menu);
        searchView = (SearchView) menu.findItem(R.id.fragment_cactus_menu_search).getActionView();
        setListFiltering();

        @IdRes int id;
        switch (getContext().getSharedPreferences(getString(R.string.measurement_preferences), Context.MODE_PRIVATE).getInt("MeasurementSort", MeasurementSortHandler.SORT_DURATION)) {
            case MeasurementSortHandler.SORT_ALPHA:
                id = R.id.fragment_cactus_menu_sort_alpha;
                break;
            case MeasurementSortHandler.SORT_DURATION:
            default:
                id = R.id.fragment_cactus_menu_sort_duration;
        }
        MenuItem item = menu.findItem(id);
        item.setChecked(true);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        @MeasurementSortHandler.MeasurementSortStrategy int strategy;
        switch (item.getItemId()) {
            case R.id.fragment_cactus_menu_sort_alpha:
                strategy = MeasurementSortHandler.SORT_ALPHA;
                break;
            case R.id.fragment_cactus_menu_sort_duration:
                strategy = MeasurementSortHandler.SORT_DURATION;
                break;
            case R.id.fragment_cactus_menu_settings:
//                Intent intent = new Intent(getContext(), MeasurementSettingsActivity.class);
//                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);
        }
        sortHandler.setSortStrategy(strategy);
        item.setChecked(true);
        SharedPreferences.Editor editor = getContext().getSharedPreferences(getString(R.string.measurement_preferences), Context.MODE_PRIVATE).edit();
        editor.putInt("MeasurementSort", strategy).apply();
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
            case R.id.menu_fragment_cactus_action_delete:
                //            mUndoHelper.remove(findViewById(android.R.id.content), "Item removed", "Undo", Snackbar.LENGTH_LONG, selectExtension.selections)
                final MeasurementQuery MeasurementQuery = new MeasurementQuery(getContext());
                MeasurementQuery.delete(selectionExtension.getSelectedItems().stream().map(MeasurementItem::getMeasurement).collect(Collectors.toList()), x -> {
                });
                mode.finish();
                break;
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
    }
}