package com.python.companion.ui.cactus.activity;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.python.companion.R;
import com.python.companion.ui.cactus.fragment.CactusViewModel;
import com.python.companion.ui.cactus.measurement.adapter.CactusItem;
import com.python.companion.ui.cactus.measurement.adapter.CactusSortHandler;

public class CactusActivity extends AppCompatActivity {
    private RecyclerView list;
    private SearchView searchView;
    private FloatingActionButton fab;
    private BottomAppBar bar;

    private ItemAdapter<CactusItem> itemAdapter;
    private FastAdapter<CactusItem> fastAdapter;

    private CactusSortHandler sortHandler;

    private CactusViewModel viewModel;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cactus);
        viewModel = new ViewModelProvider(this).get(CactusViewModel.class);

        findGlobalViews();
        setupActionBar();

    }

    private void findGlobalViews() {
        list = findViewById(R.id.activity_cactus_measurements);
        bar = findViewById(R.id.activity_cactus_bottombar);
    }

    private void setupActionBar() {
        setSupportActionBar(bar);
        ActionBar actionbar = getSupportActionBar();

        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            Drawable icon = bar.getNavigationIcon();
            if (icon != null) {
                icon.setColorFilter(getResources().getColor(R.color.colorWindowBackground, null), PorterDuff.Mode.SRC_IN);
                bar.setNavigationIcon(icon);
            }
        }
    }
//
//    private void prepareList(View view) {
//        ComparableItemListImpl<MeasurementItem> itemList = new ComparableItemListImpl<>((o1, o2) -> 0);
//
//        itemAdapter = new ItemAdapter<>(itemList);
//        sortHandler = new MeasurementSortHandler.Builder()
//                .setStrategy(getContext().getSharedPreferences(getString(R.string.note_preferences), Context.MODE_PRIVATE).getInt("noteSort", MeasurementSortHandler.SORT_DURATION))
//                .setItemList(itemList)
//                .build();
//        fastAdapter = FastAdapter.with(itemAdapter);
//        ExtensionsFactories.INSTANCE.register(new SelectExtensionFactory());
//        list.setAdapter(fastAdapter);
//        list.setLayoutManager(new LinearLayoutManager(view.getContext()));
//        list.addItemDecoration(new DividerItemDecoration(view.getContext(), LinearLayoutManager.VERTICAL));
//
//
//        selectionExtension = fastAdapter.getOrCreateExtension(SelectExtension.class);
//        selectionExtension.setSelectable(true);
//        selectionExtension.setMultiSelect(true);
//        selectionExtension.setSelectOnLongClick(true);
//
//        fastAdapter.setOnPreClickListener((view1, noteItemIAdapter, noteItem, integer) -> {
//            Boolean res = actionModeHelper.onClick(noteItem);
//            return res != null ? res : false;
//        });
//
//        fastAdapter.setOnClickListener((view1, noteItemIAdapter, noteItem, position) -> {
//            if (!actionModeHelper.isActive()) {
//                if (noteItem.getNote().isSecure()) {
//                    Guard.decryptKeystore(noteItem.getNote().getContent(), noteItem.getNote().getIv(), noteItem.getNote().getName(), getContext(), plaintext -> {
//                        Intent intent = new Intent(getContext(), NoteViewActivity.class);
//                        intent.putExtra("name", noteItem.getNote().getName());
//                        intent.putExtra("content", plaintext);
//                        startActivity(intent);
//                    });
//                } else {
//                    Intent intent = new Intent(getContext(), NoteViewActivity.class);
//                    intent.putExtra("name", noteItem.getNote().getName());
//                    intent.putExtra("content", noteItem.getNote().getContent());
//                    startActivity(intent);
//                }
//            } else {
//                fastAdapter.notifyItemChanged(position);
//            }
//            return true;
//        });
//
//        fastAdapter.setOnPreLongClickListener((view1, noteItemIAdapter, noteItem, position) -> {
//            ActionMode actionMode = actionModeHelper.onLongClick((MainActivity) getActivity(), position);
//            if (actionMode != null)
//                getActivity().findViewById(R.id.action_mode_bar).setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
//            return actionMode != null;
//        });
//
//
//        actionModeHelper = new ActionModeHelper<>(fastAdapter, R.menu.fragment_note_action, this);
//        setListUpdates();
//    }
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case android.R.id.home:
//                finish();
//                break;
//            case R.id.activity_cactus_menu_search:
//                break;
//            case R.id.activity_cactus_menu_sort:
//                break;
//            case R.id.activity_cactus_menu_settings:
//                break;
//        }
//        return super.onOptionsItemSelected(item);
//    }
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.fragment_cactus, menu); //https://material.io/develop/android/components/bottom-app-bar/
//        return true;
//    }
}
