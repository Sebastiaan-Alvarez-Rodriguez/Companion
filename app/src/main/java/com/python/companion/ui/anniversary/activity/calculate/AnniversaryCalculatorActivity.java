package com.python.companion.ui.anniversary.activity.calculate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.mikepenz.fastadapter.diff.DiffCallback;
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil;
import com.mikepenz.fastadapter.extensions.ExtensionsFactories;
import com.mikepenz.fastadapter.listeners.ItemFilterListener;
import com.mikepenz.fastadapter.select.SelectExtensionFactory;
import com.mikepenz.fastadapter.utils.ComparableItemListImpl;
import com.python.companion.R;
import com.python.companion.db.entity.Anniversary;
import com.python.companion.db.interact.AnniversaryStore;
import com.python.companion.ui.general.customviews.ContextMenuRecyclerView;
import com.python.companion.ui.anniversary.AnniversaryContainer;
import com.python.companion.ui.anniversary.Type;
import com.python.companion.ui.anniversary.activity.AnniversaryEditActivity;
import com.python.companion.ui.anniversary.adapter.AnniversaryCalculatorSortHandler;
import com.python.companion.ui.anniversary.adapter.item.AnniversaryCalculatorItem;
import com.python.companion.ui.anniversary.viewmodel.AnniversaryViewModel;
import com.python.companion.util.AnniversaryUtil;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class AnniversaryCalculatorActivity extends AppCompatActivity {
    private View layout;
    private ContextMenuRecyclerView list;
    private EditText amountView;
    private RadioGroup displayGroup;
    private FloatingActionButton addButton;
    private SearchView searchView;

    private ItemAdapter<AnniversaryCalculatorItem> itemAdapter;
    private FastAdapter<AnniversaryCalculatorItem> fastAdapter;
    private AnniversaryCalculatorSortHandler sortHandler;

    private AnniversaryViewModel viewModel;

    private long userInterval;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anniversary_calculator);
        viewModel = new ViewModelProvider(this).get(AnniversaryViewModel.class);
        findViews();
        setupActionBar();
        userInterval = getInterval();
        prepareList();
        prepareButtons();
    }

    private void findViews() {
        layout = findViewById(R.id.activity_cactus_anniversary_layout);
        amountView = findViewById(R.id.activity_cactus_anniversary_amount);
        displayGroup = findViewById(R.id.activity_cactus_anniversary_radiogroup);
        list = findViewById(R.id.activity_cactus_anniversary_list);
        addButton = findViewById(R.id.activity_cactus_anniversary_add);
    }

    private void setupActionBar() {
        Toolbar myToolbar = findViewById(R.id.activity_cactus_anniversary_toolbar);
        setSupportActionBar(myToolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setTitle("Cactus");
        }
    }

    private void prepareButtons() {
        displayGroup.setOnCheckedChangeListener((group, checkedId) -> {
            final Type t = checkedId == R.id.activity_cactus_anniversary_dates ? Type.DATE : Type.DISTANCE;
            for (AnniversaryCalculatorItem x : itemAdapter.getAdapterItems())
                x.onTypeChange(t);
            fastAdapter.notifyAdapterDataSetChanged();
        });
        LocalDate together = AnniversaryUtil.getTogether(this);
        amountView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                userInterval = getInterval(s);
                for (int x = 0; x < fastAdapter.getItemCount(); ++x) {
                    AnniversaryCalculatorItem item = fastAdapter.getItem(x);
                    final int w = x;
                    AnniversaryUtil.futureInterval(item.getAnniversary(), together, userInterval, date -> {
                        item.onDateChange(date);
                        runOnUiThread(() -> fastAdapter.notifyAdapterItemChanged(w));
                    }, error -> item.onDateError(userInterval >= 0 ? "Very far away" : "Very long ago"));
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AnniversaryEditActivity.class);
            startActivity(intent);
        });
    }

    private void prepareList() {
        ComparableItemListImpl<AnniversaryCalculatorItem> itemList = new ComparableItemListImpl<>((o1, o2) -> 0);
        itemAdapter = new ItemAdapter<>(itemList);

        registerForContextMenu(list);

        fastAdapter = FastAdapter.with(itemAdapter);

        sortHandler = new AnniversaryCalculatorSortHandler.Builder()
                .setStrategy(getSharedPreferences(getString(R.string.cactus_preferences), Context.MODE_PRIVATE).getInt("CactusAnniversarySort", AnniversaryCalculatorSortHandler.SORT_DURATION))
                .setItemList(itemList)
                .build();
        ExtensionsFactories.INSTANCE.register(new SelectExtensionFactory());
        list.setAdapter(fastAdapter);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

        fastAdapter.setOnLongClickListener((view, cactusItemIAdapter, cactusItem, integer) -> false);

        setListUpdates();
    }

    private void setListUpdates() {
        LocalDate together = AnniversaryUtil.getTogether(this);

        viewModel.getAnniversaries().observe(this, anniversaries -> {
            List<AnniversaryCalculatorItem> list = anniversaries.parallelStream().map(anniversary -> {
                try {
                    return new AnniversaryCalculatorItem(anniversary, AnniversaryUtil.futureInterval(anniversary, together, userInterval));
                } catch (DateTimeException e) {
                    return new AnniversaryCalculatorItem(anniversary, userInterval >= 0 ? "Very far away" : "Very long ago");
                }
            }).collect(Collectors.toList());

            FastAdapterDiffUtil.INSTANCE.set(itemAdapter, list, new DiffCallback<AnniversaryCalculatorItem>() {
                @Override
                public boolean areItemsTheSame(AnniversaryCalculatorItem oldItem, AnniversaryCalculatorItem newItem) {
                    return oldItem.getAnniversary().getNamePlural().equals(newItem.getAnniversary().getNamePlural());
                }
                @Override
                public boolean areContentsTheSame(AnniversaryCalculatorItem oldItem, AnniversaryCalculatorItem newItem) {
                    return oldItem.getDisplayValue().equals(newItem.getDisplayValue()) && oldItem.getAnniversaryName().equals(newItem.getAnniversaryName());
                }
                @Nullable
                @Override
                public Object getChangePayload(AnniversaryCalculatorItem oldItem, int oldPosition, AnniversaryCalculatorItem newItem, int newPosition) {
                    return oldItem.getDisplayValue().equals(newItem.getDisplayValue()) ? newItem.getAnniversaryName() : newItem.getDisplayValue();
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
        itemAdapter.getItemFilter().setFilterPredicate((AnniversaryItem, charSequence) -> {
            CharSequence lower = charSequence.toString().toLowerCase();
            Anniversary anniversary = AnniversaryItem.getAnniversary();
            return anniversary.getNameSingular().toLowerCase().contains(lower) || anniversary.getNamePlural().toLowerCase().contains(lower);
        });
        itemAdapter.getItemFilter().setItemFilterListener(new ItemFilterListener<AnniversaryCalculatorItem>() {
            @Override
            public void itemsFiltered(@Nullable CharSequence charSequence, @Nullable List<? extends AnniversaryCalculatorItem> list) {
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
        getMenuInflater().inflate(R.menu.activity_cactus_anniversary, menu);
        searchView = (SearchView) menu.findItem(R.id.activity_cactus_anniversary_search).getActionView();
        setListFiltering();

        @IdRes int id;
        switch (getSharedPreferences(getString(R.string.cactus_preferences), Context.MODE_PRIVATE).getInt("CactusAnniversarySort", AnniversaryCalculatorSortHandler.SORT_DURATION)) {
            case AnniversaryCalculatorSortHandler.SORT_ALPHA:
                id = R.id.activity_cactus_anniversary_sort_alpha;
                break;
            case AnniversaryCalculatorSortHandler.SORT_DURATION:
            default:
                id = R.id.activity_cactus_anniversary_sort_duration;
        }
        MenuItem item = menu.findItem(id);
        item.setChecked(true);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        @AnniversaryCalculatorSortHandler.CactusSortStrategy int strategy;
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return super.onOptionsItemSelected(item);
            case R.id.activity_cactus_anniversary_sort_alpha:
                strategy = AnniversaryCalculatorSortHandler.SORT_ALPHA;
                break;
            case R.id.activity_cactus_anniversary_sort_duration:
                strategy = AnniversaryCalculatorSortHandler.SORT_DURATION;
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        sortHandler.setSortStrategy(strategy);
        item.setChecked(true);
        SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.cactus_preferences), Context.MODE_PRIVATE).edit();
        editor.putInt("CactusAnniversarySort", strategy).apply();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_cactus_anniversary_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ContextMenuRecyclerView.RecyclerViewContextMenuInfo info = (ContextMenuRecyclerView.RecyclerViewContextMenuInfo) item.getMenuInfo();
        AnniversaryCalculatorItem clicked = fastAdapter.getItem(info.position);

        switch (item.getItemId()) {
            case R.id.menu_context_anniversary_edit:
                if (clicked.getAnniversary().getCanModify()) {
                    Intent intent = new Intent(this, AnniversaryEditActivity.class);
                    intent.putExtra("anniversary", new AnniversaryContainer(clicked.getAnniversary()));
                    startActivity(intent);
                } else {
                    Snackbar.make(layout, "Cannot edit default type '"+clicked.getAnniversary().getNameSingular()+"'!", Snackbar.LENGTH_LONG).show();
                }
                break;
            case R.id.menu_context_anniversary_delete:
                if (clicked.getAnniversary().getCanModify()) {
                    AnniversaryStore.delete(clicked.getAnniversary(), this, () -> {});
                } else {
                    Snackbar.make(layout, "Cannot delete default type '"+clicked.getAnniversary().getNameSingular()+"'!", Snackbar.LENGTH_LONG).show();
                }
                break;
        }
        return super.onContextItemSelected(item);
    }
}

