package com.python.companion.ui.cactus.activity.measurement;

import android.app.Application;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.drawable.Drawable;
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
import com.python.companion.db.constant.MeasurementQuery;
import com.python.companion.db.dao.DAOMeasurement;
import com.python.companion.db.entity.Measurement;
import com.python.companion.ui.cactus.measurement.adapter.MeasurementItem;
import com.python.companion.util.MeasurementUtil;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MeasurementAddActivity extends AppCompatActivity {
    private EditText singular, plural, amount;
    private RecyclerView list;
    private View layout;

    private MeasurementAddViewModel viewmodel;

    private FastAdapter<MeasurementItem> fastAdapter;
    protected SelectExtension<MeasurementItem> selectionExtension;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurement_add);
        viewmodel = new ViewModelProvider(this).get(MeasurementAddViewModel.class);

        findViews();
        setupList();
        setupActionBar();
    }

    private void findViews() {
        layout = findViewById(R.id.activity_measurement_add_layout);
        singular = findViewById(R.id.activity_measurement_add_name_singular);
        plural = findViewById(R.id.activity_measurement_add_name_plural);
        amount = findViewById(R.id.activity_measurement_add_amount);
        list = findViewById(R.id.activity_measurement_add_list);

    }

    private void setupList() {
        ItemAdapter<MeasurementItem> itemAdapter = new ItemAdapter<>();

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
            List<MeasurementItem> items = MeasurementUtil.getDefaultMeasurements().stream().map(MeasurementItem::new).collect(Collectors.toList());
            items.addAll(measurements.stream().map(MeasurementItem::new).collect(Collectors.toList()));
            itemAdapter.set(items);
        });
    }

    private void setupActionBar() {
        Toolbar myToolbar = findViewById(R.id.activity_measurement_add_toolbar);
        setSupportActionBar(myToolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            Drawable icon = myToolbar.getNavigationIcon();
            if (icon != null) {
                icon.setColorFilter(new BlendModeColorFilter(getResources().getColor(R.color.colorWindowBackground, null), BlendMode.SRC_IN));
                myToolbar.setNavigationIcon(icon);
            }
            actionbar.setTitle("Add Jubileum");
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

        Set<MeasurementItem> selected = selectionExtension.getSelectedItems();
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
                finish();
                break;
            case R.id.menu_measurement_add_save:
                if (checkInput()) {
                    String nameSingular = singular.getText().toString(), namePlural = plural.getText().toString(), amountText = amount.getText().toString();
                    long amt = Long.parseLong(amountText);

                    MeasurementItem selected = selectionExtension.getSelectedItems().iterator().next();

                    Duration d = selected.getMeasurement().getDuration().multipliedBy(amt);
                    MeasurementQuery measurementQuery = new MeasurementQuery(this);
                    measurementQuery.isUnique(namePlural, unique -> {
                        if (unique) {
                            measurementQuery.insert(nameSingular, namePlural, d, selected.getMeasurement().getCornerstoneType());
                            finish();
                        } else {
                            Snackbar.make(layout, "Measurement with same plural name already exists", Snackbar.LENGTH_LONG).show();
                        }
                    });
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class MeasurementAddViewModel extends AndroidViewModel {
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
