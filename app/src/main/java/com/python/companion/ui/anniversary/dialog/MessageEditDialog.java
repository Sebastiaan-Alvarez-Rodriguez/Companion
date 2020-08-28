package com.python.companion.ui.anniversary.dialog;

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
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
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
import com.python.companion.db.entity.Anniversary;
import com.python.companion.db.entity.Message;
import com.python.companion.db.interact.MessageStore;
import com.python.companion.db.repository.AnniversaryRepository;
import com.python.companion.ui.anniversary.adapter.item.AnniversaryItemSimple;
import com.python.companion.ui.general.dialog.DialogAcceptListener;
import com.python.companion.ui.general.dialog.DialogCancelListener;
import com.python.companion.ui.general.dialog.FixedDialogFragment;
import com.python.companion.util.AnniversaryUtil;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public class MessageEditDialog extends FixedDialogFragment {
    public static class Builder {
        private DialogCancelListener dialogCancelListener = null;
        private DialogAcceptListener dialogAcceptListener = null;

        private Anniversary anniversary;

        private @Nullable
        Message previous;

        public Builder setCancelListener(DialogCancelListener dialogCancelListener) {
            this.dialogCancelListener = dialogCancelListener;
            return this;
        }

        public Builder setAcceptListener(DialogAcceptListener dialogAcceptListener) {
            this.dialogAcceptListener = dialogAcceptListener;
            return this;
        }

        public Builder setAnniversary(Anniversary anniversary) {
            this.anniversary = anniversary;
            return this;
        }

        public Builder setPrevious(@NonNull Message message) {
            this.previous = message;
            return this;
        }

        public MessageEditDialog build() {
            if (anniversary == null)
                throw new IllegalStateException("Caller must provide Anniversary which is set with builder.setAnniversary(anniversary)");
            return new MessageEditDialog(dialogCancelListener, dialogAcceptListener, anniversary, previous);
        }
    }

    protected View layout, beforeLayout;
    protected Spinner optionSpinner;
    protected EditText beforeAmount;
    protected RecyclerView list;
    protected Button cancelButton, acceptButton;

    protected @NonNull Anniversary anniversary;

    protected @Nullable DialogCancelListener cancelListener;
    protected @Nullable DialogAcceptListener overrideListener;

    protected FastAdapter<AnniversaryItemSimple> fastAdapter;
    protected SelectExtension<AnniversaryItemSimple> selectionExtension;

    protected @Nullable Message previous;
    protected boolean editMode, selectedInitial;

    protected MessageEditDialog(@Nullable DialogCancelListener cancelListener, @Nullable DialogAcceptListener overrideListener, @NonNull Anniversary anniversary, @Nullable Message previous) {
        this.cancelListener = cancelListener;
        this.overrideListener = overrideListener;
        this.anniversary = anniversary;
        this.previous = previous;
        this.editMode = previous != null;
        this.selectedInitial = false;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_message_edit, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findGlobalViews(view);
        setMainOptions();
        prepareButtons();
        prepareTextchangeListener();
        setAnniversaryOptions();
        if (editMode)
            restorePrevious();
    }

    protected void findGlobalViews(View view) {
        layout = view.findViewById(R.id.dialog_message_layout);
        optionSpinner = view.findViewById(R.id.dialog_message_option_spinner);
        beforeLayout = view.findViewById(R.id.dialog_message_before_layout);
        beforeAmount = beforeLayout.findViewById(R.id.dialog_message_before_amount);
        list = beforeLayout.findViewById(R.id.dialog_message_before_anniversary_list);

        cancelButton = view.findViewById(R.id.dialog_message_cancel);
        acceptButton = view.findViewById(R.id.dialog_message_accept);
    }

    protected void setMainOptions() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(), R.array.message_add_dialog_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        optionSpinner.setAdapter(adapter);
    }

    @SuppressWarnings("ConstantConditions")
    private void setAnniversaryOptions() {
        ItemAdapter<AnniversaryItemSimple> itemAdapter = new ItemAdapter<>();

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


        List<AnniversaryItemSimple> items = AnniversaryUtil.getBaseAnniversaries().stream().map(AnniversaryItemSimple::new).collect(Collectors.toList());
        itemAdapter.set(items);
        if (editMode && !selectedInitial) {
            long pid = AnniversaryUtil.chronoUnitToID(previous.getType()); // id of previously selected base anniversary
            int location = fastAdapter.getPosition(pid);
            if (location != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                selectionExtension.toggleSelection(location);
                fastAdapter.getItem(location).setSelected(true);
                fastAdapter.notifyItemChanged(location);
                selectedInitial = true;
            }
        }
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
                        AnniversaryItemSimple item = fastAdapter.getItem(x);
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
            Set<AnniversaryItemSimple> selected = selectionExtension.getSelectedItems();
            if (selected.size() == 0) {
                Snackbar.make(layout, "Please pick an anniversary unit to describe this unit in", Snackbar.LENGTH_LONG).show();
                return false;
            }
        }
        return true;
    }

    private void save() {
        if (!checkInput())
            return;

        if (optionSpinner.getSelectedItemPosition() == 0) { // User wants notification on anniversary date
            Message n = Message.from(getContext(), anniversary);
            MessageStore.insert(n, getContext(), this::finishSuccess, error -> Snackbar.make(layout, error, Snackbar.LENGTH_LONG).show());
        } else { // User wants notification to happen some time before anniversary date
            long amount = Long.parseLong(beforeAmount.getText().toString());
            Anniversary selected = selectionExtension.getSelectedItems().iterator().next().getAnniversary();
            if (optionSpinner.getSelectedItemPosition() == 1) {
                Message n = Message.from(getContext(), anniversary, amount, selected);
                MessageStore.insert(n, getContext(), this::finishSuccess, error -> Snackbar.make(layout, error, Snackbar.LENGTH_LONG).show());
            } else {
                LocalDate together = AnniversaryUtil.getTogether(getContext());
                LocalDate anniversaryDate = AnniversaryUtil.futureInterval(anniversary, together, 1);
                LocalDate specified = anniversaryDate.minus(amount, selected);
                long between = ChronoUnit.DAYS.between(specified, anniversaryDate);
                for (long x = 0; x <= between; ++x) {
                    Message n = Message.from(getContext(), anniversary, x, AnniversaryUtil.getBaseAnniversary(ChronoUnit.DAYS));
                    MessageStore.upsert(n, getContext(), this::finishSuccess);
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

    public static class AnniversaryViewModel extends AndroidViewModel {
        private AnniversaryRepository anniversaryRepository;

        private LiveData<List<Anniversary>> anniversaries = null;

        public AnniversaryViewModel(@NonNull Application application) {
            super(application);
            anniversaryRepository = new AnniversaryRepository(application);
        }


        public LiveData<List<Anniversary>> getAnniversarys() {
            if (anniversaries == null)
                anniversaries = anniversaryRepository.getAnniversarys();
            return anniversaries;
        }
    }
}