package com.python.companion.ui.jubileum.activity;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
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
import com.python.companion.ui.jubileum.MeasurementContainer;
import com.python.companion.ui.jubileum.adapter.MeasurementSortHandler;
import com.python.companion.ui.jubileum.adapter.item.MeasurementItem;

import java.util.List;
import java.util.stream.Collectors;

public class JubileumActivity extends AppCompatActivity implements ActionMode.Callback {

    private RecyclerView list;
    private SearchView searchView;
    private FloatingActionButton fab;
    
    private ItemAdapter<MeasurementItem> itemAdapter;
    private FastAdapter<MeasurementItem> fastAdapter;
    private SelectExtension<MeasurementItem> selectionExtension;
    private ActionModeHelper<MeasurementItem> actionModeHelper;
    private MeasurementSortHandler sortHandler;

    private MeasurementViewModel measurementViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurement);
        measurementViewModel = new ViewModelProvider(this).get(MeasurementViewModel.class);
        findViews();
        prepareAdd();
        setupActionBar();
        prepareList();
    }

    private void findViews() {
        list = findViewById(R.id.fragment_list_list);
        fab = findViewById(R.id.fragment_list_add);
    }

    private void prepareAdd() {
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, JubileumActivity.class);
            startActivity(intent);
        });
    }

    private void setupActionBar() {
        Toolbar myToolbar = findViewById(R.id.activity_measurement_toolbar);
        setSupportActionBar(myToolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            Drawable icon = myToolbar.getNavigationIcon();
            if (icon != null) {
                icon.setColorFilter(new BlendModeColorFilter(getResources().getColor(R.color.colorWindowBackground, null), BlendMode.SRC_IN));
                myToolbar.setNavigationIcon(icon);
            }
            actionbar.setTitle("Measurements");
        }
    }

    private void prepareList() {
        ComparableItemListImpl<MeasurementItem> itemList = new ComparableItemListImpl<>((o1, o2) -> 0);

        itemAdapter = new ItemAdapter<>(itemList);
        sortHandler = new MeasurementSortHandler.Builder()
                .setStrategy(getSharedPreferences(getString(R.string.measurement_preferences), Context.MODE_PRIVATE).getInt("MeasurementSort", MeasurementSortHandler.SORT_ALPHA))
                .setItemList(itemList)
                .build();
        fastAdapter = FastAdapter.with(itemAdapter);
        ExtensionsFactories.INSTANCE.register(new SelectExtensionFactory());
        list.setAdapter(fastAdapter);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));


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
                Measurement m = MeasurementItem.getMeasurement();
                Intent intent = new Intent(this, JubileumEditActivity.class);
                intent.putExtra("measurement", new MeasurementContainer(m));
                startActivity(intent);
            } else {
                fastAdapter.notifyItemChanged(position);
            }
            return true;
        });

        fastAdapter.setOnPreLongClickListener((view1, measurementItemIAdapter, measurementItem, position) -> {
            ActionMode actionMode = actionModeHelper.onLongClick(this, position);
            if (actionMode != null)
                findViewById(R.id.action_mode_bar).setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
            return actionMode != null;
        });

        actionModeHelper = new ActionModeHelper<>(fastAdapter, R.menu.fragment_jubileum_action, this);
        setListUpdates();
    }

    private void setListUpdates() {
        measurementViewModel.getMeasurements().observe(this, measurements -> {
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
            return m.getNameSingular().toLowerCase().contains(sequence) || m.getNamePlural().toLowerCase().contains(sequence);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_jubileum, menu);
        searchView = (SearchView) menu.findItem(R.id.activity_measurement_menu_search).getActionView();
        setListFiltering();
        return true;
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
            final MeasurementQuery measurementQuery = new MeasurementQuery(this);
            measurementQuery.delete(selectionExtension.getSelectedItems().stream().map(MeasurementItem::getMeasurement).collect(Collectors.toList()), () -> {
            });
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
