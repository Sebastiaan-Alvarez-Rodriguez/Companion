package com.python.companion.ui.jubileum.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.mikepenz.fastadapter.diff.DiffCallback;
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil;
import com.mikepenz.fastadapter.extensions.ExtensionsFactories;
import com.mikepenz.fastadapter.listeners.ItemFilterListener;
import com.mikepenz.fastadapter.select.SelectExtension;
import com.mikepenz.fastadapter.select.SelectExtensionFactory;
import com.mikepenz.fastadapter.utils.ComparableItemListImpl;
import com.python.companion.R;
import com.python.companion.ui.cactus.activity.CactusViewModel;
import com.python.companion.ui.jubileum.MeasurementContainer;
import com.python.companion.ui.jubileum.adapter.item.MeasurementItemSimple;
import com.python.companion.util.MeasurementUtil;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JubileumSelectActivity extends AppCompatActivity {
    private View layout;
    private RecyclerView list;
    private SearchView searchView;

    private ItemAdapter<MeasurementItemSimple> itemAdapter;
    private FastAdapter<MeasurementItemSimple> fastAdapter;
    private SelectExtension<MeasurementItemSimple> selectionExtension;

    private CactusViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurement_select);
        viewModel = new ViewModelProvider(this).get(CactusViewModel.class);
        findViews();
        prepareList();
        setupActionBar();
        setResult(RESULT_CANCELED);
    }

    private void findViews() {
        layout = findViewById(R.id.activity_measurement_select_layout);
        list = findViewById(R.id.activity_measurement_select_list);
    }


    private void prepareList() {
        ComparableItemListImpl<MeasurementItemSimple> itemList = new ComparableItemListImpl<>((o1, o2) -> 0);
        itemAdapter = new ItemAdapter<>(itemList);
        fastAdapter = FastAdapter.with(itemAdapter);

        ExtensionsFactories.INSTANCE.register(new SelectExtensionFactory());
        list.setAdapter(fastAdapter);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));


        selectionExtension = fastAdapter.getOrCreateExtension(SelectExtension.class);
        selectionExtension.setSelectable(true);
        selectionExtension.setMultiSelect(true);
        selectionExtension.setSelectOnLongClick(false);

        fastAdapter.setOnClickListener((view1, MeasurementItemIAdapter, MeasurementItem, position) -> {
            fastAdapter.notifyItemChanged(position);
            return true;
        });

        setListUpdates();
    }

    private void setListUpdates() {
        SharedPreferences preferences = getSharedPreferences(getString(R.string.cactus_preferences), Context.MODE_PRIVATE);
        LocalDate together = LocalDate.parse(preferences.getString(getString(R.string.cactus_preferences_key_together), "2017-11-08"));

        List<MeasurementItemSimple> defaultList = MeasurementUtil.getDefaultMeasurements().stream().map(MeasurementItemSimple::new).collect(Collectors.toList());

        viewModel.getMeasurements().observe(this, measurements -> {
            List<MeasurementItemSimple> newlist = measurements.stream().sorted((o1, o2) -> o1.getNamePlural().compareTo(o2.getNamePlural())).map(MeasurementItemSimple::new).collect(Collectors.toList());

            newlist.addAll(defaultList);
            FastAdapterDiffUtil.INSTANCE.set(itemAdapter, newlist, new DiffCallback<MeasurementItemSimple>() {
                @Override
                public boolean areItemsTheSame(MeasurementItemSimple oldItem, MeasurementItemSimple newItem) {
                    return oldItem.getMeasurement().getNamePlural().equals(newItem.getMeasurement().getNamePlural());
                }

                @Override
                public boolean areContentsTheSame(MeasurementItemSimple oldItem, MeasurementItemSimple newItem) {
                    return oldItem.getMeasurement().getNamePlural().equals(newItem.getMeasurement().getNamePlural());
                }

                @NotNull
                @Override
                public Object getChangePayload(MeasurementItemSimple oldItem, int oldPosition, MeasurementItemSimple newItem, int newPosition) {
                    return newItem.getMeasurement().getNamePlural();
                }
            });
        });
    }

    private void setListFiltering() {
        itemAdapter.getItemFilter().setFilterPredicate((MeasurementItem, charSequence) -> MeasurementItem.getMeasurement().getNameSingular().toLowerCase().contains(charSequence.toString().toLowerCase()));
        itemAdapter.getItemFilter().setItemFilterListener(new ItemFilterListener<MeasurementItemSimple>() {
            @Override
            public void itemsFiltered(@Nullable CharSequence charSequence, @Nullable List<? extends MeasurementItemSimple> list) {
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
                return true;
            }
        });
    }

    private void setupActionBar() {
        Toolbar myToolbar = findViewById(R.id.activity_measurement_select_toolbar);
        setSupportActionBar(myToolbar);

        ActionBar actionbar = getSupportActionBar();

        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            Drawable icon = myToolbar.getNavigationIcon();
            if (icon != null) {
                icon.setColorFilter(getResources().getColor(R.color.colorWindowBackground, null), PorterDuff.Mode.SRC_IN);
                myToolbar.setNavigationIcon(icon);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_measurement_select, menu);
        searchView = (SearchView) menu.findItem(R.id.activity_measurement_select_search).getActionView();
        setListFiltering();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: finish(); break;
            case R.id.activity_measurement_select_ok:
                if (selectionExtension.getSelectedItems().isEmpty()) {
                    Snackbar.make(layout, "Select at least one item to compute shared interval for", Snackbar.LENGTH_LONG).show();
                } else {
                    Intent data = new Intent();
                    ArrayList<MeasurementContainer> c = selectionExtension.getSelectedItems().stream().map(itemsimple -> new MeasurementContainer(itemsimple.getMeasurement())).collect(Collectors.toCollection(ArrayList::new));
                    data.putParcelableArrayListExtra("chosen", c);
                    setResult(RESULT_OK, data);
                    finish();
                }
                break;
        }
        return true;
    }
}
