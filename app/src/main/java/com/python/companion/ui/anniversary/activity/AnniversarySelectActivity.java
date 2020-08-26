package com.python.companion.ui.anniversary.activity;

import android.content.Intent;
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
import com.python.companion.ui.anniversary.AnniversaryContainer;
import com.python.companion.ui.anniversary.adapter.item.AnniversaryItemSimple;
import com.python.companion.ui.anniversary.viewmodel.AnniversaryViewModel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AnniversarySelectActivity extends AppCompatActivity {
    private View layout;
    private RecyclerView list;
    private SearchView searchView;

    private ItemAdapter<AnniversaryItemSimple> itemAdapter;
    private FastAdapter<AnniversaryItemSimple> fastAdapter;
    private SelectExtension<AnniversaryItemSimple> selectionExtension;

    private AnniversaryViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anniversary_select);
        viewModel = new ViewModelProvider(this).get(AnniversaryViewModel.class);
        findViews();
        prepareList();
        setupActionBar();
        setResult(RESULT_CANCELED);
    }

    private void findViews() {
        layout = findViewById(R.id.activity_anniversary_select_layout);
        list = findViewById(R.id.activity_anniversary_select_list);
    }


    private void prepareList() {
        ComparableItemListImpl<AnniversaryItemSimple> itemList = new ComparableItemListImpl<>((o1, o2) -> 0);
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

        fastAdapter.setOnClickListener((view1, AnniversaryItemIAdapter, AnniversaryItem, position) -> {
            fastAdapter.notifyItemChanged(position);
            return true;
        });

        setListUpdates();
    }

    private void setListUpdates() {

        viewModel.getAnniversaries().observe(this, anniversaries -> {
            List<AnniversaryItemSimple> list = anniversaries.stream().sorted((o1, o2) -> o1.getNamePlural().compareTo(o2.getNamePlural())).map(AnniversaryItemSimple::new).collect(Collectors.toList());

            FastAdapterDiffUtil.INSTANCE.set(itemAdapter, list, new DiffCallback<AnniversaryItemSimple>() {
                @Override
                public boolean areItemsTheSame(AnniversaryItemSimple oldItem, AnniversaryItemSimple newItem) {
                    return oldItem.getAnniversary().getNamePlural().equals(newItem.getAnniversary().getNamePlural());
                }

                @Override
                public boolean areContentsTheSame(AnniversaryItemSimple oldItem, AnniversaryItemSimple newItem) {
                    return oldItem.getAnniversary().getNamePlural().equals(newItem.getAnniversary().getNamePlural());
                }

                @NotNull
                @Override
                public Object getChangePayload(AnniversaryItemSimple oldItem, int oldPosition, AnniversaryItemSimple newItem, int newPosition) {
                    return newItem.getAnniversary().getNamePlural();
                }
            });
        });
    }

    private void setListFiltering() {
        itemAdapter.getItemFilter().setFilterPredicate((AnniversaryItem, charSequence) -> AnniversaryItem.getAnniversary().getNameSingular().toLowerCase().contains(charSequence.toString().toLowerCase()));
        itemAdapter.getItemFilter().setItemFilterListener(new ItemFilterListener<AnniversaryItemSimple>() {
            @Override
            public void itemsFiltered(@Nullable CharSequence charSequence, @Nullable List<? extends AnniversaryItemSimple> list) {
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
        Toolbar myToolbar = findViewById(R.id.activity_anniversary_select_toolbar);
        setSupportActionBar(myToolbar);

        ActionBar actionbar = getSupportActionBar();

        if (actionbar != null)
            actionbar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_anniversary_select, menu);
        searchView = (SearchView) menu.findItem(R.id.activity_anniversary_select_search).getActionView();
        setListFiltering();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: finish(); break;
            case R.id.activity_anniversary_select_ok:
                if (selectionExtension.getSelectedItems().isEmpty()) {
                    Snackbar.make(layout, "Select at least one item to compute shared interval for", Snackbar.LENGTH_LONG).show();
                } else {
                    Intent data = new Intent();
                    ArrayList<AnniversaryContainer> c = selectionExtension.getSelectedItems().stream().map(itemsimple -> new AnniversaryContainer(itemsimple.getAnniversary())).collect(Collectors.toCollection(ArrayList::new));
                    data.putParcelableArrayListExtra("chosen", c);
                    setResult(RESULT_OK, data);
                    finish();
                }
                break;
        }
        return true;
    }
}
