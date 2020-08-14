package com.python.companion.ui.jubileum.dialog;

import android.app.Application;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
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
import com.python.companion.db.entity.Measurement;
import com.python.companion.db.entity.Notify;
import com.python.companion.db.interact.NotifyStore;
import com.python.companion.db.repository.MeasurementRepository;
import com.python.companion.ui.general.dialog.DialogAcceptListener;
import com.python.companion.ui.general.dialog.DialogCancelListener;
import com.python.companion.ui.jubileum.adapter.item.JubileumItemSimple;
import com.python.companion.util.MeasurementUtil;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public class NotifyEditDialog extends DialogFragment {

    @SuppressWarnings("unused")
    public static class Builder {
        private DialogCancelListener dialogCancelListener = null;
        private DialogAcceptListener dialogAcceptListener = null;

        private String questionString = "";
        private Measurement measurement;

        private @Nullable Notify previous;

        public Builder setCancelListener(DialogCancelListener dialogCancelListener) {
            this.dialogCancelListener = dialogCancelListener;
            return this;
        }

        public Builder setAcceptListener(DialogAcceptListener dialogAcceptListener) {
            this.dialogAcceptListener = dialogAcceptListener;
            return this;
        }

        public Builder setMeasurement(Measurement measurement) {
            this.measurement = measurement;
            return this;
        }

        public Builder setPrevious(@NonNull Notify notify) {
            this.previous = notify;
            return this;
        }

        public NotifyEditDialog build() {
            if (measurement == null)
                throw new IllegalStateException("Caller must provide Measurement which is set with builder.setMeasurement(measurement)");
            return new NotifyEditDialog(dialogCancelListener, dialogAcceptListener, measurement, previous);
        }
    }

    protected View layout, beforeLayout;
    protected Spinner optionSpinner;
    protected EditText beforeAmount;
    protected RecyclerView list;
    protected Button cancelButton, acceptButton;

    protected @NonNull Measurement measurement;

    protected @Nullable DialogCancelListener cancelListener;
    protected @Nullable DialogAcceptListener overrideListener;

    protected MeasurementViewModel viewmodel;

    protected FastAdapter<JubileumItemSimple> fastAdapter;
    protected SelectExtension<JubileumItemSimple> selectionExtension;

    protected @Nullable Notify previous;
    protected boolean editMode, selectedItem;

    protected NotifyEditDialog(@Nullable DialogCancelListener cancelListener, @Nullable DialogAcceptListener overrideListener, @NonNull Measurement measurement, @Nullable Notify previous) {
        this.cancelListener = cancelListener;
        this.overrideListener = overrideListener;
        this.measurement = measurement;
        this.previous = previous;
        this.editMode = previous != null;
        this.selectedItem = false;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewmodel = new ViewModelProvider(this).get(MeasurementViewModel.class);
        return inflater.inflate(R.layout.dialog_notify_edit, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findGlobalViews(view);
        setMainOptions();
        prepareButtons();
        prepareTextchangeListener();
        setMeasurementOptions();
        if (editMode)
            restorePrevious();
    }

    protected void findGlobalViews(View view) {
        layout = view.findViewById(R.id.dialog_notify_layout);
        optionSpinner = view.findViewById(R.id.dialog_notify_option_spinner);
        beforeLayout = view.findViewById(R.id.dialog_notify_before_layout);
        beforeAmount = beforeLayout.findViewById(R.id.dialog_notify_before_amount);
        list = beforeLayout.findViewById(R.id.dialog_notify_before_measurement_list);

        cancelButton = view.findViewById(R.id.dialog_notify_cancel);
        acceptButton = view.findViewById(R.id.dialog_notify_accept);
    }

    protected void setMainOptions() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(), R.array.notify_add_dialog_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        optionSpinner.setAdapter(adapter);
    }

    @SuppressWarnings("ConstantConditions")
    private void setMeasurementOptions() {
        ItemAdapter<JubileumItemSimple> itemAdapter = new ItemAdapter<>();

        fastAdapter = FastAdapter.with(itemAdapter);
        ExtensionsFactories.INSTANCE.register(new SelectExtensionFactory());
        list.setAdapter(fastAdapter);
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        list.addItemDecoration(new DividerItemDecoration(getContext(), LinearLayoutManager.VERTICAL));


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

        viewmodel.getMeasurements().observe(getViewLifecycleOwner(), measurements -> {
            List<JubileumItemSimple> items = measurements.stream().map(JubileumItemSimple::new).collect(Collectors.toList());
            itemAdapter.set(items);
            if (editMode && !selectedItem) {
                long pid = previous.getMeasurementID(); // id of previously selected measurements
                int location = fastAdapter.getPosition(pid);
                if (location != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                    selectionExtension.toggleSelection(location);
                    fastAdapter.getItem(location).setSelected(true);
                    fastAdapter.notifyItemChanged(location);
                    selectedItem = true;
                }
            }
        });
    }

    private void prepareButtons() {
        cancelButton.setOnClickListener(v -> {
            if (cancelListener != null) {
                cancelListener.onCancel();
            }
            dismiss();
        });
        acceptButton.setOnClickListener(v -> save());
        optionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                beforeLayout.setVisibility((position == 0) ? View.INVISIBLE : View.VISIBLE);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @SuppressWarnings("ConstantConditions")
    private void restorePrevious() {
        beforeAmount.setText(String.valueOf(previous.getAmount()));
        if (previous.getAmount() > 0) {
            optionSpinner.setSelection(1);
            beforeLayout.setVisibility(View.VISIBLE);
        }
    }

    private boolean nowPlural = true;
    private void prepareTextchangeListener() {
        beforeAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                long userInterval = getInterval(s);
                boolean displayPlural = userInterval != 1;
                if (nowPlural != displayPlural) {
                    for (int x = 0; x < fastAdapter.getItemCount(); ++x) {
                        JubileumItemSimple item = fastAdapter.getItem(x);
                        item.setDisplayPlural(displayPlural);
                        fastAdapter.notifyAdapterItemChanged(x);
                    }
                    nowPlural = displayPlural;
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private long getInterval(CharSequence text) {
        if (text.length() == 0)
            return 1;
        try {
            return Long.parseLong(text.toString());
        } catch (NumberFormatException e) {
            beforeAmount.setError("Not a number");
        } catch (Exception e) {
            beforeAmount.setError("Number overflow/underflow (pick less extreme number)");
        }
        return 1;
    }

    private boolean checkInput() {
        if (optionSpinner.getSelectedItemPosition() == AdapterView.INVALID_POSITION) {
            Snackbar.make(layout, "Please pick an option", Snackbar.LENGTH_LONG).show();
            return false;
        }

        if (optionSpinner.getSelectedItemPosition() != 0) {
            long amount = getInterval(beforeAmount.getText());
            if (beforeAmount.getText().length() == 0) {
                beforeAmount.setError("Please fill in this field");
                return false;
            }
            try {
                Long.parseLong(beforeAmount.getText().toString());
            } catch (Exception e) {
                return false;
            }
            if (amount < 1) {
                beforeAmount.setError("Must be at least 1");
                return false;
            }
            Set<JubileumItemSimple> selected = selectionExtension.getSelectedItems();
            if (selected.size() == 0) {
                Snackbar.make(layout, "Please pick a measurement unit to describe this unit in", Snackbar.LENGTH_LONG).show();
                return false;
            }
        }
        return true;
    }

    private void save() {
        if (!checkInput())
            return;

        if (optionSpinner.getSelectedItemPosition() == 0) { // User wants notification on jubileum date
            Notify n = Notify.from(getContext(), measurement);
            NotifyStore.insert(n, getContext(), this::finishSuccess, error -> Snackbar.make(layout, error, Snackbar.LENGTH_LONG).show());
        } else { // User wants notification to happen some time before jubileum date
            long amount = Long.parseLong(beforeAmount.getText().toString());
            Measurement selected = selectionExtension.getSelectedItems().iterator().next().getMeasurement();
            if (optionSpinner.getSelectedItemPosition() == 1) {
                Notify n = Notify.from(getContext(), measurement, amount, selected);
                NotifyStore.insert(n, getContext(), this::finishSuccess, error -> Snackbar.make(layout, error, Snackbar.LENGTH_LONG).show());
            } else {
                LocalDate together = MeasurementUtil.getTogether(getContext());
                LocalDate jubileumDate = MeasurementUtil.futureInterval(measurement, together, 1);
                LocalDate specified = jubileumDate.minus(amount, selected);
                long between = ChronoUnit.DAYS.between(specified, jubileumDate);
                for (long x = 0; x <= between; ++x) {
                    Notify n = Notify.from(getContext(), measurement, x, MeasurementUtil.getBaseMeasurement(ChronoUnit.DAYS));
                    NotifyStore.upsert(n, getContext(), this::finishSuccess);
                }
            }
        }
    }

    private void finishSuccess() {
        if (overrideListener != null) {
            overrideListener.onAccept();
        }
        dismiss();
    }

    public static class MeasurementViewModel extends AndroidViewModel {
        private MeasurementRepository measurementRepository;

        private LiveData<List<Measurement>> measurements = null;

        public MeasurementViewModel(@NonNull Application application) {
            super(application);
            measurementRepository = new MeasurementRepository(application);
        }


        public LiveData<List<Measurement>> getMeasurements() {
            if (measurements == null)
                measurements = measurementRepository.getMeasurements();
            return measurements;
        }
    }
}