package com.python.companion.ui.cactus.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.mikepenz.fastadapter.helpers.ActionModeHelper;
import com.mikepenz.fastadapter.listeners.ItemFilterListener;
import com.mikepenz.fastadapter.select.SelectExtension;
import com.python.companion.R;
import com.python.companion.db.constant.MeasurementQuery;
import com.python.companion.db.entity.Measurement;
import com.python.companion.ui.cactus.activity.measurement.MeasurementAddActivity;
import com.python.companion.ui.cactus.fragment.CactusViewModel;
import com.python.companion.ui.cactus.measurement.adapter.CactusSortHandler;
import com.python.companion.ui.cactus.measurement.adapter.item.CactusItem;
import com.python.companion.ui.cactus.measurement.Type;

import java.util.List;
import java.util.stream.Collectors;

public class CactusDistanceActivity extends AppCompatActivity implements ActionMode.Callback {
    private RecyclerView list;
    private SearchView searchView;
    private FloatingActionButton fab;

    private ItemAdapter<CactusItem> itemAdapter;
    private FastAdapter<CactusItem> fastAdapter;
    private SelectExtension<CactusItem> selectionExtension;
    private ActionModeHelper<CactusItem> actionModeHelper;
    private CactusSortHandler sortHandler;

    private CactusViewModel viewModel;

    private Type requestType;
    private List<Measurement> others;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.fragment_list);
//        viewModel = new ViewModelProvider(this).get(CactusViewModel.class);
//        requestType = Type.valueOf(getIntent().getStringExtra("type"));
//        if (requestType == Type.FUTURE_INTERTWINED) {
//            ArrayList<MeasurementItem> items = getIntent().getParcelableArrayListExtra("chosen");
//            others = items.stream().map(MeasurementItem::getMeasurement).collect(Collectors.toList());
//        }
//        findViews();
//        prepareAdd();
//        prepareList();
    }

    private void findViews() {
        list = findViewById(R.id.fragment_list_list);
        fab = findViewById(R.id.fragment_list_add);
    }

    private void prepareAdd() {
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, MeasurementAddActivity.class);
            startActivity(intent);
        });
    }

//    private void prepareList() {
//        ComparableItemListImpl<CactusItem> itemList = new ComparableItemListImpl<>((o1, o2) -> 0);
//        itemAdapter = new ItemAdapter<>(itemList);
//        fastAdapter = FastAdapter.with(itemAdapter);
//
//        sortHandler = new CactusSortHandler.Builder()
//                .setStrategy(getSharedPreferences(getString(R.string.measurement_preferences), Context.MODE_PRIVATE).getInt("MeasurementSort", CactusSortHandler.SORT_DURATION))
//                .setItemList(itemList)
//                .build();
//        ExtensionsFactories.INSTANCE.register(new SelectExtensionFactory());
//        list.setAdapter(fastAdapter);
//        list.setLayoutManager(new LinearLayoutManager(this));
//        list.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
//
//
//        selectionExtension = fastAdapter.getOrCreateExtension(SelectExtension.class);
//        selectionExtension.setSelectable(true);
//        selectionExtension.setMultiSelect(true);
//        selectionExtension.setSelectOnLongClick(true);
//
//        fastAdapter.setOnPreClickListener((view1, MeasurementItemIAdapter, MeasurementItem, integer) -> {
//            Boolean res = actionModeHelper.onClick(MeasurementItem);
//            return res != null ? res : false;
//        });
//
//        fastAdapter.setOnClickListener((view1, MeasurementItemIAdapter, MeasurementItem, position) -> {
//            if (!actionModeHelper.isActive()) {
////                    Intent intent = new Intent(getContext(), MeasurementViewActivity.class);
////                    intent.putExtra("name", MeasurementItem.getMeasurement().getName());
////                    intent.putExtra("duration", MeasurementItem.getMeasurement().getDuration());
////                    startActivity(intent);
//            } else {
//                fastAdapter.notifyItemChanged(position);
//            }
//            return true;
//        });
//
//        fastAdapter.setOnPreLongClickListener((view1, MeasurementItemIAdapter, MeasurementItem, position) -> {
//            ActionMode actionMode = actionModeHelper.onLongClick(this, position);
//            if (actionMode != null)
//                findViewById(R.id.action_mode_bar).setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
//            return actionMode != null;
//        });
//
//
//        actionModeHelper = new ActionModeHelper<>(fastAdapter, R.menu.fragment_cactus_action, this);
//
//        setListUpdates();
//    }


//    private long computeDistance(Type type, Measurement measurement, LocalDate together) {
//        switch (type) {
//            case FUTURE_INTERTWINED:
//                return ChronoUnit.DAYS.between(LocalDate.now(), MeasurementUtil.futureIntertwinedInterval(measurement, together, others));
//            default:
//            case FUTURE:
//                return ChronoUnit.DAYS.between(LocalDate.now(), MeasurementUtil.futureInterval(measurement, together));
//        }
//    }

//    private void setListUpdates() {
//        SharedPreferences preferences = getSharedPreferences(getString(R.string.measurement_preferences), Context.MODE_PRIVATE);
//        LocalDate together = LocalDate.parse(preferences.getString("together", "2017-11-08"));
//
//        List<CactusItem> defaultList = MeasurementUtil.getDefaultMeasurements().stream().map(measurement -> {
//            long distance = computeDistance(requestType, measurement, together);
//            return new CactusItem(measurement, String.valueOf(distance), distance == 1 ? measurement.getNameSingular() : measurement.getNamePlural(), true);
//        }).collect(Collectors.toList());
//        for (CactusItem x : defaultList)
//            x.setSelectable(false);
//
//        viewModel.getMeasurements().observe(this, measurements -> {
//            List<CactusItem> newlist = measurements.stream().map(measurement -> {
//                long distance = computeDistance(requestType, measurement, together);
//                return new CactusItem(measurement, String.valueOf(distance), distance == 1 ? measurement.getNameSingular() : measurement.getNamePlural(), true);
//            }).collect(Collectors.toList());
//
//            newlist.addAll(defaultList);
//            FastAdapterDiffUtil.INSTANCE.set(itemAdapter, newlist, new DiffCallback<CactusItem>() {
//                @Override
//                public boolean areItemsTheSame(CactusItem oldItem, CactusItem newItem) {
//                    return oldItem.getMeasurement().getNamePlural().equals(newItem.getMeasurement().getNamePlural());
//                }
//
//                @Override
//                public boolean areContentsTheSame(CactusItem oldItem, CactusItem newItem) {
//                    return oldItem.getDisplayValue().equals(newItem.getDisplayValue()) && oldItem.getDisplayMeasurement().equals(newItem.getDisplayMeasurement());
//                }
//
//                @Nullable
//                @Override
//                public Object getChangePayload(CactusItem oldItem, int oldPosition, CactusItem newItem, int newPosition) {
//                    return oldItem.getDisplayValue().equals(newItem.getDisplayValue()) ? newItem.getDisplayMeasurement() : newItem.getDisplayValue();
//                }
//            });
//        });
//    }

    private void setListFiltering() {
        itemAdapter.getItemFilter().setFilterPredicate((MeasurementItem, charSequence) -> MeasurementItem.getMeasurement().getNameSingular().toLowerCase().contains(charSequence.toString().toLowerCase()));
        itemAdapter.getItemFilter().setItemFilterListener(new ItemFilterListener<CactusItem>() {
            @Override
            public void itemsFiltered(@Nullable CharSequence charSequence, @Nullable List<? extends CactusItem> list) {
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.fragment_cactus, menu);
        searchView = (SearchView) menu.findItem(R.id.fragment_cactus_menu_search).getActionView();
        setListFiltering();

        @IdRes int id;
        switch (getSharedPreferences(getString(R.string.measurement_preferences), Context.MODE_PRIVATE).getInt("MeasurementSort", CactusSortHandler.SORT_DURATION)) {
            case CactusSortHandler.SORT_ALPHA:
                id = R.id.fragment_cactus_menu_sort_alpha;
                break;
            case CactusSortHandler.SORT_DURATION:
            default:
                id = R.id.fragment_cactus_menu_sort_duration;
        }
        MenuItem item = menu.findItem(id);
        item.setChecked(true);
        return super.onCreateOptionsMenu(menu);
    }



    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        @CactusSortHandler.MeasurementSortStrategy int strategy;
        switch (item.getItemId()) {
            case R.id.fragment_cactus_menu_sort_alpha:
                strategy = CactusSortHandler.SORT_ALPHA;
                break;
            case R.id.fragment_cactus_menu_sort_duration:
                strategy = CactusSortHandler.SORT_DURATION;
                break;
            case R.id.fragment_cactus_menu_settings:
//                Intent intent = new Intent(getContext(), MeasurementSettingsActivity.class);
//                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);
        }
        sortHandler.setSortStrategy(strategy);
        item.setChecked(true);
        SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.measurement_preferences), Context.MODE_PRIVATE).edit();
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
                final MeasurementQuery MeasurementQuery = new MeasurementQuery(this);
                MeasurementQuery.delete(selectionExtension.getSelectedItems().stream().map(CactusItem::getMeasurement).collect(Collectors.toList()), x -> {
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
