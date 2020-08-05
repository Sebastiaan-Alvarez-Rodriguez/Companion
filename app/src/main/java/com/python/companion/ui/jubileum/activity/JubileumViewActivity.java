package com.python.companion.ui.jubileum.activity;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.mikepenz.fastadapter.diff.DiffCallback;
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil;
import com.mikepenz.fastadapter.extensions.ExtensionsFactories;
import com.mikepenz.fastadapter.helpers.ActionModeHelper;
import com.mikepenz.fastadapter.select.SelectExtension;
import com.mikepenz.fastadapter.select.SelectExtensionFactory;
import com.mikepenz.fastadapter.utils.ComparableItemListImpl;
import com.python.companion.R;
import com.python.companion.db.constant.NotifyQuery;
import com.python.companion.db.entity.Measurement;
import com.python.companion.db.entity.Notify;
import com.python.companion.db.pojo.notify.NotifyWithMeasurementNames;
import com.python.companion.db.repository.NotifyRepository;
import com.python.companion.ui.jubileum.MeasurementContainer;
import com.python.companion.ui.jubileum.adapter.item.NotifyItem;
import com.python.companion.ui.jubileum.dialog.NotifyEditDialog;
import com.python.companion.util.MeasurementUtil;
import com.python.companion.util.ThreadUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class JubileumViewActivity extends AppCompatActivity implements ActionMode.Callback {
    private int REQ_EDIT = 1;

    private View layout;
    private TextView equationView, amountHadView, amountHadNameView, nextAnnounceView, nextDateView, nextDistanceView, notificationsAnnounceView;
    private RecyclerView notificationsView;
    private ImageButton notificationAddButton;
    private FloatingActionButton editButton;

    private ItemAdapter<NotifyItem> itemAdapter;
    private FastAdapter<NotifyItem> fastAdapter;
    private SelectExtension<NotifyItem> selectionExtension;
    private ActionModeHelper<NotifyItem> actionModeHelper;

    private NotifyViewModel notifyViewModel;

    private Measurement measurement;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jubileum_view);
        findViews();

        notifyViewModel = new ViewModelProvider(this).get(NotifyViewModel.class);

        Intent intent = getIntent();
        measurement = ((MeasurementContainer) Objects.requireNonNull(intent.getParcelableExtra("measurement"))).getMeasurement();

        String parentSingular = intent.getStringExtra("parentSingular");
        String parentPlural = intent.getStringExtra("parentPlural");

        setupActionBar(measurement.getNamePlural());
        setMeasurement(measurement, Objects.requireNonNull(parentSingular), Objects.requireNonNull(parentPlural));
        prepareButtons(measurement);
        prepareList();
    }

    private void findViews() {
        layout = findViewById(R.id.activity_jubileum_view_layout);
        equationView = findViewById(R.id.activity_jubileum_view_equation);
        amountHadView = findViewById(R.id.activity_jubileum_view_amount_had);
        amountHadNameView = findViewById(R.id.activity_jubileum_view_amount_had_name);
        nextAnnounceView = findViewById(R.id.activity_jubileum_view_next_announce);
        nextDateView = findViewById(R.id.activity_jubileum_view_next_date);
        nextDistanceView = findViewById(R.id.activity_jubileum_view_next_distance);
        notificationsAnnounceView = findViewById(R.id.activity_jubileum_view_notifications_announce);
        notificationsView = findViewById(R.id.activity_jubileum_view_notifications);
        notificationAddButton = findViewById(R.id.activity_jubileum_view_notifications_add);

        editButton = findViewById(R.id.activity_jubileum_view_edit);
    }

    private void setupActionBar(@NonNull String title) {
        Toolbar myToolbar = findViewById(R.id.activity_jubileum_view_toolbar);
        setSupportActionBar(myToolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setTitle(title);
        }

    }

    private void setMeasurement(@NonNull Measurement measurement, @NonNull String parentSingular, @NonNull String parentPlural) {
        equationView.setText("1 "+measurement.getNameSingular()+" = "+measurement.getAmount()+" "+(measurement.getAmount() == 1 ? parentSingular : parentPlural));
        LocalDate together = MeasurementUtil.getTogether(this);
        Executors.newSingleThreadExecutor().execute(() -> {
            long jubileaHad = MeasurementUtil.distanceCurrent(measurement, together);
            LocalDate nextJubileum = MeasurementUtil.futureInterval(measurement, together, 1);
            long daysToNext = MeasurementUtil.computeDistance(nextJubileum);
            ThreadUtil.runOnUIThread(()-> {
                amountHadView.setText(String.valueOf(jubileaHad));
                nextAnnounceView.setText("Your "+(jubileaHad+1)+MeasurementUtil.getDayOfMonthSuffix((int) (jubileaHad+1))+" jubileum will be on");
                nextDateView.setText(nextJubileum.toString());
                nextDistanceView.setText("which is "+daysToNext+(daysToNext == 1 ? " day" : " days")+" from now");
                amountHadNameView.setText(measurement.getNameSingular() + ((jubileaHad == 1) ? " jubileum" : " jubilea"));
            });
        });
    }

    private void prepareButtons(@NonNull Measurement measurement) {
        editButton.setOnClickListener(v -> {
            if (!measurement.getCanModify()) {
                Snackbar.make(layout, "Cannot edit default type '"+measurement.getNameSingular()+"'!", Snackbar.LENGTH_LONG).show();
                return;
            }
            Intent intent = new Intent(this, JubileumEditActivity.class);
            intent.putExtra("measurement", new MeasurementContainer(measurement));
            startActivityForResult(intent, REQ_EDIT);
        });

        notificationAddButton.setOnClickListener(v -> {
            NotifyEditDialog dialog = new NotifyEditDialog.Builder().setMeasurement(measurement).build();
            dialog.show(getSupportFragmentManager(), null);
        });
    }

    private void prepareList() {
        ComparableItemListImpl<NotifyItem> itemList = new ComparableItemListImpl<>((o1, o2) -> 0);

        itemAdapter = new ItemAdapter<>(itemList);
        itemList.withComparator((o1, o2) -> o1.getNotify().getNotifyDate().compareTo(o2.getNotify().getNotifyDate()));

        fastAdapter = FastAdapter.with(itemAdapter);
        ExtensionsFactories.INSTANCE.register(new SelectExtensionFactory());
        notificationsView.setAdapter(fastAdapter);
        notificationsView.setLayoutManager(new LinearLayoutManager(this));
        notificationsView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));


        selectionExtension = fastAdapter.getOrCreateExtension(SelectExtension.class);
        selectionExtension.setSelectable(true);
        selectionExtension.setMultiSelect(true);
        selectionExtension.setSelectOnLongClick(true);

        fastAdapter.setOnPreClickListener((view1, noteItemIAdapter, noteItem, integer) -> {
            Boolean res = actionModeHelper.onClick(noteItem);
            return res != null ? res : false;
        });

        fastAdapter.setOnClickListener((view1, notifyItemIAdapter, notifyItem, position) -> {
            if (!actionModeHelper.isActive()) {
                new NotifyEditDialog.Builder().setMeasurement(measurement).setPrevious(notifyItem.getNotify()).build().show(getSupportFragmentManager(), null);
            } else {
                fastAdapter.notifyItemChanged(position);
            }
            return true;
        });

        fastAdapter.setOnPreLongClickListener((view1, notifyItemIAdapter, notifyItem, position) -> {
            ActionMode actionMode = actionModeHelper.onLongClick(this, position);
            if (actionMode != null)
                findViewById(R.id.action_mode_bar).setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
            return actionMode != null;
        });


        actionModeHelper = new ActionModeHelper<>(fastAdapter, R.menu.activity_jubileum_view_action, this);
        setListUpdates();
    }

    private void setListUpdates() {
        notifyViewModel.getNotificationsNamed(measurement.getMeasurementID()).observe(this, notifies -> {
            List<NotifyItem> newlist = notifies.stream().map(NotifyItem::new).collect(Collectors.toList());
            FastAdapterDiffUtil.INSTANCE.set(itemAdapter, newlist, new DiffCallback<NotifyItem>() {
                @Override
                public boolean areItemsTheSame(NotifyItem oldItem, NotifyItem newItem) {
                    return oldItem.getNotify().getNotifyID() == newItem.getNotify().getNotifyID();
                }

                @Override
                public boolean areContentsTheSame(NotifyItem oldItem, NotifyItem newItem) {
                    Notify old = oldItem.getNotify(), cur = newItem.getNotify();
                    return old.getAmount() == cur.getAmount()
                            && old.getJubileumID() == cur.getJubileumID()
                            && old.getNotifyDate().equals(cur.getNotifyDate())
                            && old.getMeasurementID() == cur.getMeasurementID();
                }

                @Nullable
                @Override
                public Object getChangePayload(NotifyItem oldItem, int oldPosition, NotifyItem newItem, int newPosition) {
                    return null;
                }
            });

        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQ_EDIT && resultCode == RESULT_OK) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            finish();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (item.getItemId() == R.id.menu_activity_jubileum_view_action_delete) {
            final NotifyQuery notifyQuery = new NotifyQuery(this);
            notifyQuery.delete(selectionExtension.getSelectedItems().stream().map(NotifyItem::getNotify).collect(Collectors.toList()));
            mode.finish();
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {}

    public static class NotifyViewModel extends AndroidViewModel {
        private NotifyRepository notifyRepository;

        private LiveData<List<NotifyWithMeasurementNames>> notifications = null;

        public NotifyViewModel(@NonNull Application application) {
            super(application);
            notifyRepository = new NotifyRepository(application);
        }

        public LiveData<List<NotifyWithMeasurementNames>> getNotificationsNamed(long measurementID) {
            if (notifications == null)
                notifications = notifyRepository.getNotificationsForJubileumNamed(measurementID);
            return notifications;
        }
    }
}
