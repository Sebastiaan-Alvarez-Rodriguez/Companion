package com.python.companion.ui.cactus.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.RadioGroup;

import androidx.annotation.IdRes;
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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import com.python.companion.ui.cactus.adapter.CactusSortHandler;
import com.python.companion.ui.cactus.adapter.item.CactusItem;
import com.python.companion.ui.jubileum.MeasurementContainer;
import com.python.companion.ui.jubileum.Type;
import com.python.companion.ui.jubileum.activity.JubileumEditActivity;
import com.python.companion.util.MeasurementUtil;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CactusCalculatorActivity extends AppCompatActivity {
    private RecyclerView list;
    private EditText amountView;
    private RadioGroup displayGroup;
    private FloatingActionButton addButton;
    private SearchView searchView;

    private ItemAdapter<CactusItem> itemAdapter;
    private FastAdapter<CactusItem> fastAdapter;
    private CactusSortHandler sortHandler;

    private CactusViewModel viewModel;

    private long userInterval;
    private List<Measurement> others;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cactus_jubileum);
        viewModel = new ViewModelProvider(this).get(CactusViewModel.class);

        ArrayList<MeasurementContainer> items = getIntent().getParcelableArrayListExtra("chosen");
        others = items.stream().map(MeasurementContainer::getMeasurement).collect(Collectors.toList());
        findViews();
        setupActionBar();
        prepareButtons();
        prepareList();
    }

    private void findViews() {
        amountView = findViewById(R.id.activity_cactus_jubileum_amount);
        displayGroup = findViewById(R.id.activity_cactus_jubileum_radiogroup);
        list = findViewById(R.id.activity_cactus_jubileum_list);
        addButton = findViewById(R.id.activity_cactus_jubileum_add);
    }

    private void setupActionBar() {
        Toolbar myToolbar = findViewById(R.id.activity_cactus_jubileum_toolbar);
        setSupportActionBar(myToolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            Drawable icon = myToolbar.getNavigationIcon();
            if (icon != null) {
                icon.setColorFilter(new BlendModeColorFilter(getResources().getColor(R.color.colorWindowBackground, null), BlendMode.SRC_IN));
                myToolbar.setNavigationIcon(icon);
            }
            actionbar.setTitle("Cactulator");
        }
    }

    private void prepareButtons() {
        displayGroup.setOnCheckedChangeListener((group, checkedId) -> {
            final Type t = checkedId == R.id.activity_cactus_jubileum_dates ? Type.DATE : Type.DISTANCE;
            for (CactusItem x : itemAdapter.getAdapterItems())
                x.onTypeChange(t);
            fastAdapter.notifyAdapterDataSetChanged();
        });
        amountView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @SuppressWarnings("ConstantConditions")
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                userInterval = getInterval(s);
                SharedPreferences preferences = getSharedPreferences(getString(R.string.cactus_preferences), Context.MODE_PRIVATE);
                LocalDate together = LocalDate.parse(preferences.getString(getString(R.string.cactus_preferences_key_together), "2017-11-08"));
                for (int x = 0; x < fastAdapter.getItemCount(); ++x) {
                    CactusItem item = fastAdapter.getItem(x);
                    final int w = x;
                    MeasurementUtil.futureIntertwinedInterval(item.getMeasurement(), together, others, userInterval, date -> {
                        item.onDateChange(date);
                        runOnUiThread(() -> fastAdapter.notifyAdapterItemChanged(w));
                    }, error -> item.onDateError(userInterval >= 0 ? "Very far away" : "Very long ago"));
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, JubileumEditActivity.class);
            startActivity(intent);
        });
    }

    private void prepareList() {
        ComparableItemListImpl<CactusItem> itemList = new ComparableItemListImpl<>((o1, o2) -> 0);
        itemAdapter = new ItemAdapter<>(itemList);
        fastAdapter = FastAdapter.with(itemAdapter);

        sortHandler = new CactusSortHandler.Builder()
                .setStrategy(getSharedPreferences(getString(R.string.cactus_preferences), Context.MODE_PRIVATE).getInt("CactusCalculatorSort", CactusSortHandler.SORT_DURATION))
                .setItemList(itemList)
                .build();
        ExtensionsFactories.INSTANCE.register(new SelectExtensionFactory());
        list.setAdapter(fastAdapter);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

        setListUpdates();
    }

    private void setListUpdates() {
        SharedPreferences preferences = getSharedPreferences(getString(R.string.cactus_preferences), Context.MODE_PRIVATE);
        LocalDate together = LocalDate.parse(preferences.getString(getString(R.string.cactus_preferences_key_together), "2017-11-08"));

        List<CactusItem> defaultList = MeasurementUtil.getDefaultMeasurements().parallelStream().map(measurement -> {
            try {
                return new CactusItem(measurement, MeasurementUtil.futureIntertwinedInterval(measurement, together, others));
            } catch (DateTimeException e) {
                return new CactusItem(measurement, userInterval >= 0 ? "Very far away" : "Very long ago");
            }
        }).collect(Collectors.toList());

        for (CactusItem x : defaultList)
            x.setSelectable(false);

        viewModel.getMeasurements().observe(this, measurements -> {
            List<CactusItem> newlist = measurements.parallelStream().map(measurement -> {
                try {
                    return new CactusItem(measurement, MeasurementUtil.futureIntertwinedInterval(measurement, together, others));
                } catch (DateTimeException e) {
                    return new CactusItem(measurement, userInterval >= 0 ? "Very far away" : "Very long ago");
                }
            }).collect(Collectors.toList());

            newlist.addAll(defaultList);
            FastAdapterDiffUtil.INSTANCE.set(itemAdapter, newlist, new DiffCallback<CactusItem>() {
                @Override
                public boolean areItemsTheSame(CactusItem oldItem, CactusItem newItem) {
                    return oldItem.getMeasurement().getNamePlural().equals(newItem.getMeasurement().getNamePlural());
                }
                @Override
                public boolean areContentsTheSame(CactusItem oldItem, CactusItem newItem) {
                    return oldItem.getDisplayValue().equals(newItem.getDisplayValue()) && oldItem.getMeasurementName().equals(newItem.getMeasurementName());
                }
                @Nullable
                @Override
                public Object getChangePayload(CactusItem oldItem, int oldPosition, CactusItem newItem, int newPosition) {
                    return oldItem.getDisplayValue().equals(newItem.getDisplayValue()) ? newItem.getMeasurementName() : newItem.getDisplayValue();
                }
            });
        });
    }

    private long getInterval(CharSequence text) {
        if (text.length() == 0)
            return 1;
        try {
            return Long.parseLong(text.toString());
        } catch (NumberFormatException e) {
            amountView.setError("Not a number");
        } catch (Exception e) {
            amountView.setError("Number overflow/underflow (pick less extreme number)");
        }
        return 1;
    }

    /** @return interval number the user wants to get results for */
    private long getInterval() {
        return getInterval(amountView.getText().toString());
    }

    private void setListFiltering() {
        itemAdapter.getItemFilter().setFilterPredicate((MeasurementItem, charSequence) -> {
            CharSequence lower = charSequence.toString().toLowerCase();
            Measurement measurement = MeasurementItem.getMeasurement();
            return measurement.getNameSingular().toLowerCase().contains(lower) || measurement.getNamePlural().toLowerCase().contains(lower);
        });
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
        getMenuInflater().inflate(R.menu.activity_cactus_jubileum, menu);
        searchView = (SearchView) menu.findItem(R.id.activity_cactus_jubileum_search).getActionView();
        setListFiltering();

        @IdRes int id;
        switch (getSharedPreferences(getString(R.string.cactus_preferences), Context.MODE_PRIVATE).getInt("CactusCalculatorSort", CactusSortHandler.SORT_DURATION)) {
            case CactusSortHandler.SORT_ALPHA:
                id = R.id.activity_cactus_jubileum_sort_alpha;
                break;
            case CactusSortHandler.SORT_DURATION:
            default:
                id = R.id.activity_cactus_jubileum_sort_duration;
        }
        MenuItem item = menu.findItem(id);
        item.setChecked(true);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        @CactusSortHandler.CactusSortStrategy int strategy;
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return super.onOptionsItemSelected(item);
            case R.id.activity_cactus_jubileum_sort_alpha:
                strategy = CactusSortHandler.SORT_ALPHA;
                break;
            case R.id.activity_cactus_jubileum_sort_duration:
                strategy = CactusSortHandler.SORT_DURATION;
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        sortHandler.setSortStrategy(strategy);
        item.setChecked(true);
        SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.cactus_preferences), Context.MODE_PRIVATE).edit();
        editor.putInt("CactusCalculatorSort", strategy).apply();
        return super.onOptionsItemSelected(item);
    }
}
