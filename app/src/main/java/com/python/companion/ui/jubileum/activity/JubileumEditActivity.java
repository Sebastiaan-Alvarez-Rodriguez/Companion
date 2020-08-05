package com.python.companion.ui.jubileum.activity;

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
import com.python.companion.db.dao.DAOMeasurement;
import com.python.companion.db.entity.Measurement;
import com.python.companion.db.interact.MeasurementStore;
import com.python.companion.ui.jubileum.MeasurementContainer;
import com.python.companion.ui.jubileum.adapter.item.JubileumItemSimple;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JubileumEditActivity extends AppCompatActivity {
    private EditText singular, plural, amount;
    private RecyclerView list;
    private View layout;

    private MeasurementAddViewModel viewmodel;

    private FastAdapter<JubileumItemSimple> fastAdapter;
    protected SelectExtension<JubileumItemSimple> selectionExtension;


    private @Nullable Measurement measurement;
    private boolean editMode, selectedParent;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurement_add);
        viewmodel = new ViewModelProvider(this).get(MeasurementAddViewModel.class);
        findViews();
        setupList();

        Intent intent = getIntent();
        editMode = intent.hasExtra("measurement");
        if (editMode) {
            measurement = ((MeasurementContainer) intent.getParcelableExtra("measurement")).getMeasurement();
            singular.setText(measurement.getNameSingular());
            plural.setText(measurement.getNamePlural());
            amount.setText(String.valueOf(measurement.getAmount()));
            selectedParent = false;
        }
        setupActionBar();
    }

    private void findViews() {
        layout = findViewById(R.id.activity_measurement_add_layout);
        singular = findViewById(R.id.activity_measurement_add_name_singular);
        plural = findViewById(R.id.activity_measurement_add_name_plural);
        amount = findViewById(R.id.activity_measurement_add_amount);
        list = findViewById(R.id.activity_measurement_add_list);

    }

    @SuppressWarnings("ConstantConditions")
    private void setupList() {
        ItemAdapter<JubileumItemSimple> itemAdapter = new ItemAdapter<>();

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

        viewmodel.getMeasurements().observe(this, measurements -> {
            List<JubileumItemSimple> items = measurements.stream().map(JubileumItemSimple::new).collect(Collectors.toList());
            itemAdapter.set(items);
            if (editMode && !selectedParent) {
                long pid = measurement.getParentID();
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
        Toolbar myToolbar = findViewById(R.id.activity_measurement_add_toolbar);
        setSupportActionBar(myToolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setTitle((editMode ? "Edit" : "Add") + " Jubileum");
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

        Set<JubileumItemSimple> selected = selectionExtension.getSelectedItems();
        if (selected.size() == 0)
            Snackbar.make(layout, "Please pick a measurement unit to describe this unit in", Snackbar.LENGTH_LONG).show();
        return !nameSingular.isEmpty() && !namePlural.isEmpty() && ! amountText.isEmpty();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_measurement_add, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finishSuccess();
                break;
            case R.id.menu_measurement_add_save:
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
        JubileumItemSimple selected = selectionExtension.getSelectedItems().iterator().next();

        Measurement m = Measurement.createFrom(nameSingular, namePlural, amt, selected.getMeasurement());

        if (!editMode) {
            MeasurementStore.insert(m, getSupportFragmentManager(), getApplicationContext(), new MeasurementStore.StoreCallback() {
                @Override
                public void onSuccess() {
                    finishSuccess();
                }
                @Override
                public void onFailure() {}
            });
        } else {
            MeasurementStore.update(m, measurement, getSupportFragmentManager(), getApplicationContext(), new MeasurementStore.StoreCallback() {
                @Override
                public void onSuccess() {
                    finishSuccess();
                }
                @Override
                public void onFailure() {
                    Snackbar.make(layout, "Cannot make this measurement depend on itself! Pick other measurement", Snackbar.LENGTH_INDEFINITE).show();
                }
            });
        }
    }

    private void finishSuccess() {
        setResult(RESULT_OK);
        finish();
    }

    public static class MeasurementAddViewModel extends AndroidViewModel {
        private DAOMeasurement daoMeasurement;

        private LiveData<List<Measurement>> data;

        public MeasurementAddViewModel(@NonNull Application application) {
            super(application);
            daoMeasurement = Database.getDatabase(application).getDAOMeasurement();
            data = null;
        }

        public LiveData<List<Measurement>> getMeasurements() {
            if (data == null)
                data = daoMeasurement.getAllLive();
            return data;
        }
    }
}
