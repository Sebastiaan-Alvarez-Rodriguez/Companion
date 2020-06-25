package com.python.companion.ui.cactus.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.RadioGroup;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.mikepenz.fastadapter.diff.DiffCallback;
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil;
import com.mikepenz.fastadapter.extensions.ExtensionsFactories;
import com.mikepenz.fastadapter.listeners.ItemFilterListener;
import com.mikepenz.fastadapter.select.SelectExtensionFactory;
import com.mikepenz.fastadapter.utils.ComparableItemListImpl;
import com.python.companion.R;
import com.python.companion.db.entity.Measurement;
import com.python.companion.ui.cactus.fragment.CactusViewModel;
import com.python.companion.ui.cactus.measurement.adapter.CactusSortHandler;
import com.python.companion.ui.cactus.measurement.adapter.item.CactusItem;
import com.python.companion.ui.cactus.measurement.adapter.item.CactusItemRegular;
import com.python.companion.util.measurement.MeasurementUtil;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

public class CactusJubileumActivity extends AppCompatActivity {
    private RecyclerView list;
    private EditText amountView;
    private RadioGroup displayGroup;

    private SearchView searchView;


    private ItemAdapter<CactusItem> itemAdapter;
    private FastAdapter<CactusItem> fastAdapter;
    private CactusSortHandler sortHandler;

    private CactusViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cactus_result_global);
        viewModel = new ViewModelProvider(this).get(CactusViewModel.class);
        findViews();
        prepareList();
    }

    private void findViews() {
        amountView = findViewById(R.id.activity_cactus_result_global_amount);
        displayGroup = findViewById(R.id.activity_cactus_result_global_radiogroup);
        list = findViewById(R.id.activity_cactus_result_global_list);
    }

    private void prepareList() {
        ComparableItemListImpl<CactusItem> itemList = new ComparableItemListImpl<>((o1, o2) -> 0);
        itemAdapter = new ItemAdapter<>(itemList);
        fastAdapter = FastAdapter.with(itemAdapter);

        sortHandler = new CactusSortHandler.Builder()
                .setStrategy(getSharedPreferences(getString(R.string.measurement_preferences), Context.MODE_PRIVATE).getInt("MeasurementSort", CactusSortHandler.SORT_DURATION))
                .setItemList(itemList)
                .build();
        ExtensionsFactories.INSTANCE.register(new SelectExtensionFactory());
        list.setAdapter(fastAdapter);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

        setListUpdates();
    }


    private long computeDistance(Measurement measurement, LocalDate together) {
        return ChronoUnit.DAYS.between(LocalDate.now(), MeasurementUtil.futureInterval(measurement, together));
    }

    private boolean showDates() {
        return displayGroup.getCheckedRadioButtonId() == R.id.activity_cactus_result_global_dates;
    }

    /** @return xth interval from present moment the user wants to get results for */
    private long getInterval() {
        return Long.parseLong(amountView.getText().toString());
    }

    private void setListUpdates() {
        SharedPreferences preferences = getSharedPreferences(getString(R.string.measurement_preferences), Context.MODE_PRIVATE);
        LocalDate together = LocalDate.parse(preferences.getString("together", "2017-11-08"));

        long requestedInterval = getInterval();
        List<CactusItem> defaultList = MeasurementUtil.getDefaultMeasurements().stream().map(measurement -> new CactusItemRegular(measurement, MeasurementUtil.futureInterval(measurement, together, requestedInterval))).collect(Collectors.toList());

        viewModel.getMeasurements().observe(this, measurements -> {
            List<CactusItem> newlist = measurements.stream().map(measurement -> new CactusItemRegular(measurement, MeasurementUtil.futureInterval(measurement, together, requestedInterval))).collect(Collectors.toList());

            newlist.addAll(defaultList);
            FastAdapterDiffUtil.INSTANCE.set(itemAdapter, newlist, new DiffCallback<CactusItem>() {
                @Override
                public boolean areItemsTheSame(CactusItem oldItem, CactusItem newItem) {
                    return oldItem.getMeasurement().getNamePlural().equals(newItem.getMeasurement().getNamePlural());
                }

                @Override
                public boolean areContentsTheSame(CactusItem oldItem, CactusItem newItem) {
                    return oldItem.getDisplayValue().equals(newItem.getDisplayValue()) && oldItem.getDisplayMeasurement().equals(newItem.getDisplayMeasurement());
                }

                @Nullable
                @Override
                public Object getChangePayload(CactusItem oldItem, int oldPosition, CactusItem newItem, int newPosition) {
                    return oldItem.getDisplayValue().equals(newItem.getDisplayValue()) ? newItem.getDisplayMeasurement() : newItem.getDisplayValue();
                }
            });
        });
    }

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
}
