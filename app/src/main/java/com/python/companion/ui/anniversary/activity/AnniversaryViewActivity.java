package com.python.companion.ui.anniversary.activity;

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
import com.python.companion.db.entity.Anniversary;
import com.python.companion.db.entity.Message;
import com.python.companion.db.interact.MessageStore;
import com.python.companion.db.repository.MessageRepository;
import com.python.companion.ui.anniversary.AnniversaryContainer;
import com.python.companion.ui.anniversary.adapter.item.MessageItem;
import com.python.companion.ui.anniversary.dialog.MessageEditDialog;
import com.python.companion.util.AnniversaryUtil;
import com.python.companion.util.ThreadUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class AnniversaryViewActivity extends AppCompatActivity implements ActionMode.Callback {
    private int REQ_EDIT = 1;

    private View layout;
    private TextView equationView, amountHadView, amountHadNameView, nextAnnounceView, nextDateView, nextDistanceView;
    private RecyclerView notificationsView;
    private ImageButton notificationAddButton;
    private FloatingActionButton editButton;

    private ItemAdapter<MessageItem> itemAdapter;
    private FastAdapter<MessageItem> fastAdapter;
    private SelectExtension<MessageItem> selectionExtension;
    private ActionModeHelper<MessageItem> actionModeHelper;

    private MessageViewModel messageViewModel;

    private Anniversary anniversary;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anniversary_view);
        findViews();

        messageViewModel = new ViewModelProvider(this).get(MessageViewModel.class);

        Intent intent = getIntent();
        anniversary = ((AnniversaryContainer) Objects.requireNonNull(intent.getParcelableExtra("anniversary"))).getAnniversary();

        String parentSingular = intent.getStringExtra("parentSingular");
        String parentPlural = intent.getStringExtra("parentPlural");

        setupActionBar(anniversary.getNamePlural());
        setAnniversary(anniversary, Objects.requireNonNull(parentSingular), Objects.requireNonNull(parentPlural));
        prepareButtons(anniversary);
        prepareList();
    }

    private void findViews() {
        layout = findViewById(R.id.activity_anniversary_view_layout);
        equationView = findViewById(R.id.activity_anniversary_view_equation);
        amountHadView = findViewById(R.id.activity_anniversary_view_amount_had);
        amountHadNameView = findViewById(R.id.activity_anniversary_view_amount_had_name);
        nextAnnounceView = findViewById(R.id.activity_anniversary_view_next_announce);
        nextDateView = findViewById(R.id.activity_anniversary_view_next_date);
        nextDistanceView = findViewById(R.id.activity_anniversary_view_next_distance);
        notificationsView = findViewById(R.id.activity_anniversary_view_notifications);
        notificationAddButton = findViewById(R.id.activity_anniversary_view_notifications_add);

        editButton = findViewById(R.id.activity_anniversary_view_edit);
    }

    private void setupActionBar(@NonNull String title) {
        Toolbar myToolbar = findViewById(R.id.activity_anniversary_view_toolbar);
        setSupportActionBar(myToolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setTitle(title);
        }

    }

    private void setAnniversary(@NonNull Anniversary anniversary, @NonNull String parentSingular, @NonNull String parentPlural) {
        equationView.setText("1 "+ anniversary.getNameSingular()+" = "+ anniversary.getAmount()+" "+(anniversary.getAmount() == 1 ? parentSingular : parentPlural));
        LocalDate together = AnniversaryUtil.getTogether(this);
        Executors.newSingleThreadExecutor().execute(() -> {
            long anniversariesHad = AnniversaryUtil.distanceCurrent(anniversary, together);
            LocalDate nextAnniversary = AnniversaryUtil.futureInterval(anniversary, together, 1);
            long daysToNext = AnniversaryUtil.computeDistance(nextAnniversary);
            ThreadUtil.runOnUIThread(()-> {
                amountHadView.setText(String.valueOf(anniversariesHad));
                nextAnnounceView.setText("Your "+(anniversariesHad+1)+ AnniversaryUtil.getDayOfMonthSuffix((int) (anniversariesHad+1))+" anniversary will be on");
                nextDateView.setText(nextAnniversary.toString());
                nextDistanceView.setText("which is "+daysToNext+(daysToNext == 1 ? " day" : " days")+" from now");
                amountHadNameView.setText(anniversary.getNameSingular() + ((anniversariesHad == 1) ? " anniversary" : " anniversaries"));
            });
        });
    }

    private void prepareButtons(@NonNull Anniversary anniversary) {
        editButton.setOnClickListener(v -> {
            if (!anniversary.getCanModify()) {
                Snackbar.make(layout, "Cannot edit default type '"+ anniversary.getNameSingular()+"'!", Snackbar.LENGTH_LONG).show();
                return;
            }
            Intent intent = new Intent(this, AnniversaryEditActivity.class);
            intent.putExtra("anniversary", new AnniversaryContainer(anniversary));
            startActivityForResult(intent, REQ_EDIT);
        });

        notificationAddButton.setOnClickListener(v -> {
            MessageEditDialog dialog = new MessageEditDialog.Builder().setAnniversary(anniversary).build();
            dialog.show(getSupportFragmentManager(), null);
        });
    }

    private void prepareList() {
        ComparableItemListImpl<MessageItem> itemList = new ComparableItemListImpl<>((o1, o2) -> 0);

        itemAdapter = new ItemAdapter<>(itemList);
        itemList.withComparator((o1, o2) -> o1.getMessage().getMessageDate().compareTo(o2.getMessage().getMessageDate()));

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

        fastAdapter.setOnClickListener((view1, messageItemIAdapter, messageItem, position) -> {
            if (!actionModeHelper.isActive()) {
                new MessageEditDialog.Builder().setAnniversary(anniversary).setPrevious(messageItem.getMessage()).build().show(getSupportFragmentManager(), null);
            } else {
                fastAdapter.notifyItemChanged(position);
            }
            return true;
        });

        fastAdapter.setOnPreLongClickListener((view1, messageItemIAdapter, messageItem, position) -> {
            ActionMode actionMode = actionModeHelper.onLongClick(this, position);
            if (actionMode != null)
                findViewById(R.id.action_mode_bar).setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
            return actionMode != null;
        });


        actionModeHelper = new ActionModeHelper<>(fastAdapter, R.menu.activity_anniversary_view_action, this);
        setListUpdates();
    }

    private void setListUpdates() {
        messageViewModel.getNotificationsNamed(anniversary.getAnniversaryID()).observe(this, notifies -> {
            List<MessageItem> newlist = notifies.stream().map(MessageItem::new).collect(Collectors.toList());
            FastAdapterDiffUtil.INSTANCE.set(itemAdapter, newlist, new DiffCallback<MessageItem>() {
                @Override
                public boolean areItemsTheSame(MessageItem oldItem, MessageItem newItem) {
                    return oldItem.getMessage().getMessageID() == newItem.getMessage().getMessageID();
                }

                @Override
                public boolean areContentsTheSame(MessageItem oldItem, MessageItem newItem) {
                    Message old = oldItem.getMessage(), cur = newItem.getMessage();
                    return old.getAmount() == cur.getAmount()
                            && old.getAnniversaryID() == cur.getAnniversaryID()
                            && old.getMessageDate().equals(cur.getMessageDate())
                            && old.getType().equals(cur.getType());
                }

                @Nullable
                @Override
                public Object getChangePayload(MessageItem oldItem, int oldPosition, MessageItem newItem, int newPosition) {
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
        if (item.getItemId() == R.id.menu_activity_anniversary_view_action_delete) {
            MessageStore.delete(selectionExtension.getSelectedItems().stream().map(MessageItem::getMessage).collect(Collectors.toList()), this, () -> {});
            mode.finish();
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {}

    public static class MessageViewModel extends AndroidViewModel {
        private MessageRepository messageRepository;

        private LiveData<List<Message>> notifications = null;

        public MessageViewModel(@NonNull Application application) {
            super(application);
            messageRepository = new MessageRepository(application);
        }

        public LiveData<List<Message>> getNotificationsNamed(long anniversaryID) {
            if (notifications == null)
                notifications = messageRepository.getMessagesForAnniversary(anniversaryID);
            return notifications;
        }
    }
}
