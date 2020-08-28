package com.python.companion.ui.notes.category.dialog;

import android.app.Application;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.python.companion.db.constant.NoteQuery;
import com.python.companion.db.entity.Category;
import com.python.companion.db.repository.CategoryRepository;
import com.python.companion.ui.general.dialog.DialogAcceptListener;
import com.python.companion.ui.general.dialog.DialogCancelListener;
import com.python.companion.ui.general.dialog.FixedDialogFragment;
import com.python.companion.ui.notes.category.adapter.CategoryItem;
import com.python.companion.ui.notes.note.adapter.NoteItem;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public class CategorySetDialog extends FixedDialogFragment {
    public static class Builder {
        private Set<NoteItem> selectedNotes = null;
        private DialogCancelListener dialogCancelListener = null;
        private DialogAcceptListener dialogAcceptListener = null;

        public Builder setSelectedNotes(@NonNull Set<NoteItem> selectedNotes) {
            this.selectedNotes = selectedNotes;
            return this;
        }

        public Builder setCancelListener(DialogCancelListener dialogCancelListener) {
            this.dialogCancelListener = dialogCancelListener;
            return this;
        }

        public Builder setFinishListener(DialogAcceptListener dialogAcceptListener) {
            this.dialogAcceptListener = dialogAcceptListener;
            return this;
        }

        public CategorySetDialog build() {
            if (selectedNotes == null)
                throw new IllegalStateException("Setdialog must be constructed with call to setSelectedNotes()");
            return new CategorySetDialog(dialogCancelListener, dialogAcceptListener, selectedNotes);
        }
    }

    protected Set<NoteItem> selectedNotes;
    protected View layout;
    protected TextView amountView;
    protected Button cancelButton, acceptButton;
    protected RecyclerView list;

    protected CategorySetDialogViewModel viewModel;
    protected FastAdapter<CategoryItem> fastAdapter;
    protected SelectExtension<CategoryItem> selectionExtension;

    protected @Nullable DialogCancelListener cancelListener;
    protected @Nullable DialogAcceptListener acceptListener;


    protected CategorySetDialog(@Nullable DialogCancelListener cancelListener, @Nullable DialogAcceptListener acceptListener, @NonNull Set<NoteItem> selectedNotes) {
        this.cancelListener = cancelListener;
        this.acceptListener = acceptListener;
        this.selectedNotes = selectedNotes;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(CategorySetDialogViewModel.class);
        return inflater.inflate(R.layout.dialog_category_set, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findGlobalViews(view);
        setupList();
        setupClicks();
    }

    @CallSuper
    protected void findGlobalViews(View view) {
        amountView = view.findViewById(R.id.dialog_category_set_text);
        list = view.findViewById(R.id.dialog_category_set_list);
        cancelButton = view.findViewById(R.id.dialog_category_set_cancel);
        acceptButton = view.findViewById(R.id.dialog_category_set_accept);
        layout = view.findViewById(R.id.dialog_category_set_layout);
    }

    protected void setupList() {
        ItemAdapter<CategoryItem> itemAdapter = new ItemAdapter<>();
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

        viewModel.getCategories().observe(getViewLifecycleOwner(), categories -> itemAdapter.set(categories.stream().map(category -> {
            CategoryItem item = new CategoryItem();
            item.setCategory(category);
            return item;
        }).collect(Collectors.toList())));
    }

    private void setupClicks() {
        cancelButton.setOnClickListener(v -> {
            if (cancelListener != null)
                cancelListener.onCancel();
            this.dismiss();
        });

        acceptButton.setOnClickListener(v -> {
            Set<CategoryItem> selected = selectionExtension.getSelectedItems();
            if (selected.size() == 0) {
                Snackbar.make(layout, "Please select a category", Snackbar.LENGTH_LONG).show();
            } else {
                CategoryItem selectedItem = selected.iterator().next();
                NoteQuery noteQuery = new NoteQuery(getContext());
                noteQuery.updateCategories(selectedNotes.stream().map(noteItem -> noteItem.getNote().getName()).collect(Collectors.toSet()), selectedItem.getCategory(), v2 -> {});
                if (acceptListener != null)
                    acceptListener.onAccept();
                this.dismiss();
            }
        });
    }

    public static class CategorySetDialogViewModel extends AndroidViewModel {
        private CategoryRepository categoryRepository;

        private LiveData<List<Category>> categories = null;

        public CategorySetDialogViewModel(@NonNull Application application) {
            super(application);
            categoryRepository = new CategoryRepository(application);
        }

        public LiveData<List<Category>> getCategories() {
            if (categories == null)
                categories = categoryRepository.getUniqueCategories();
            return categories;
        }
    }
}
