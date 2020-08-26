package com.python.companion.ui.anniversary.activity;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.mikepenz.fastadapter.extensions.ExtensionsFactories;
import com.mikepenz.fastadapter.select.SelectExtension;
import com.mikepenz.fastadapter.select.SelectExtensionFactory;
import com.python.companion.R;
import com.python.companion.db.Database;
import com.python.companion.db.dao.DAOAnniversary;
import com.python.companion.db.entity.Anniversary;
import com.python.companion.db.interact.AnniversaryStore;
import com.python.companion.ui.anniversary.AnniversaryContainer;
import com.python.companion.ui.anniversary.adapter.item.AnniversaryItemSimple;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AnniversaryEditActivity extends AppCompatActivity {
    private EditText singular, plural, amount;
    private RecyclerView list;
    private View layout;

    private AnniversaryAddViewModel viewmodel;

    private FastAdapter<AnniversaryItemSimple> fastAdapter;
    protected SelectExtension<AnniversaryItemSimple> selectionExtension;


    private @Nullable
    Anniversary anniversary;
    private boolean editMode, selectedParent;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anniversary_edit);
        viewmodel = new ViewModelProvider(this).get(AnniversaryAddViewModel.class);
        findViews();
        setupList();

        Intent intent = getIntent();
        editMode = intent.hasExtra("anniversary");
        if (editMode) {
            anniversary = ((AnniversaryContainer) intent.getParcelableExtra("anniversary")).getAnniversary();
            singular.setText(anniversary.getNameSingular());
            plural.setText(anniversary.getNamePlural());
            amount.setText(String.valueOf(anniversary.getAmount()));
            selectedParent = false;
        }
        setupActionBar();
    }

    private void findViews() {
        layout = findViewById(R.id.activity_anniversary_add_layout);
        singular = findViewById(R.id.activity_anniversary_add_name_singular);
        plural = findViewById(R.id.activity_anniversary_add_name_plural);
        amount = findViewById(R.id.activity_anniversary_add_amount);
        list = findViewById(R.id.activity_anniversary_add_list);

    }

    @SuppressWarnings("ConstantConditions")
    private void setupList() {
        ItemAdapter<AnniversaryItemSimple> itemAdapter = new ItemAdapter<>();

        fastAdapter = FastAdapter.with(itemAdapter);
        ExtensionsFactories.INSTANCE.register(new SelectExtensionFactory());
        list.setAdapter(fastAdapter);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));


        selectionExtension = fastAdapter.getOrCreateExtension(SelectExtension.class);
        assert selectionExtension != null;
        selectionExtension.setSelectable(true);
        selectionExtension.setMultiSelect(false);
        selectionExtension.setSelectOnLongClick(false);

        fastAdapter.setOnClickListener((view, categoryItemIAdapter, categoryItem, position) -> {
            selectionExtension.toggleSelection(position);
            categoryItem.setSelected(!categoryItem.isSelected());
            fastAdapter.notifyItemChanged(position);
            return true;
        });

        viewmodel.getAnniversarys().observe(this, anniversaries -> {
            List<AnniversaryItemSimple> items = anniversaries.stream().map(AnniversaryItemSimple::new).collect(Collectors.toList());
            itemAdapter.set(items);
            if (editMode && !selectedParent) {
                long pid = anniversary.getParentID();
                int location = fastAdapter.getPosition(pid);
                if (location != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                    selectionExtension.toggleSelection(location);
                    fastAdapter.getItem(location).setSelected(true);
                    fastAdapter.notifyItemChanged(location);
                    selectedParent = true;
                }
            }
        });
    }

    private void setupActionBar() {
        Toolbar myToolbar = findViewById(R.id.activity_anniversary_add_toolbar);
        setSupportActionBar(myToolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setTitle((editMode ? "Edit" : "Add") + " Anniversary");
        }
    }

    private boolean checkInput() {
        String nameSingular = singular.getText().toString(), namePlural = plural.getText().toString(), amountText = amount.getText().toString();
        if (nameSingular.isEmpty())
            singular.setError("Please fill in this field");
        if (namePlural.isEmpty())
            plural.setError("Please fill in this field");
        if (amountText.isEmpty())
            amount.setError("Please fill in this field");
        if (Long.parseLong(amountText) < 1)
            amount.setError("Minimum value is 1");

        Set<AnniversaryItemSimple> selected = selectionExtension.getSelectedItems();
        if (selected.size() == 0)
            Snackbar.make(layout, "Please pick an anniversary unit to describe this unit in", Snackbar.LENGTH_LONG).show();
        return !nameSingular.isEmpty() && !namePlural.isEmpty() && ! amountText.isEmpty();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_anniversary_add, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finishSuccess();
                break;
            case R.id.menu_anniversary_add_save:
                save();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("ConstantConditions")
    private void save() {
        if (!checkInput())
            return;

        String nameSingular = singular.getText().toString(), namePlural = plural.getText().toString(), amountText = amount.getText().toString();
        long amt = Long.parseLong(amountText);
        AnniversaryItemSimple selected = selectionExtension.getSelectedItems().iterator().next();

        Anniversary m = Anniversary.createFrom(nameSingular, namePlural, amt, selected.getAnniversary());

        if (!editMode) {
            AnniversaryStore.insert(m, getSupportFragmentManager(), getApplicationContext(), new AnniversaryStore.StoreCallback() {
                @Override
                public void onSuccess() {
                    finishSuccess();
                }
                @Override
                public void onFailure() {}
            });
        } else {
            AnniversaryStore.update(m, anniversary, getSupportFragmentManager(), getApplicationContext(), new AnniversaryStore.StoreCallback() {
                @Override
                public void onSuccess() {
                    finishSuccess();
                }
                @Override
                public void onFailure() {
                    Snackbar.make(layout, "Cannot make this anniversary depend on itself! Pick other anniversary", Snackbar.LENGTH_INDEFINITE).show();
                }
            });
        }
    }

    private void finishSuccess() {
        setResult(RESULT_OK);
        finish();
    }

    public static class AnniversaryAddViewModel extends AndroidViewModel {
        private DAOAnniversary daoAnniversary;

        private LiveData<List<Anniversary>> data;

        public AnniversaryAddViewModel(@NonNull Application application) {
            super(application);
            daoAnniversary = Database.getDatabase(application).getDAOAnniversary();
            data = null;
        }

        public LiveData<List<Anniversary>> getAnniversarys() {
            if (data == null)
                data = daoAnniversary.getAllLive();
            return data;
        }
    }
}
