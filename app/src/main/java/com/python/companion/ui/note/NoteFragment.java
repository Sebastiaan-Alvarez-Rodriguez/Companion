package com.python.companion.ui.note;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.python.companion.R;
import com.python.companion.ui.note.activity.NoteViewActivity;
import com.python.companion.ui.note.adapter.NoteItem;

import java.util.stream.Collectors;

// https://github.com/wasabeef/richeditor-android
//    https://github.com/wasabeef/richeditor-android/blob/master/sample/src/main/res/layout/activity_main.xml
//    https://github.com/wasabeef/richeditor-android/blob/master/sample/src/main/java/jp/wasabeef/sample/MainActivity.java

// https://stackoverflow.com/questions/53414053/bold-and-italics-in-edittext
// https://stackoverflow.com/questions/14371092/how-to-make-a-specific-text-on-textview-bold/14371107

// https://github.com/federicoiosue/Omni-Notes


// Or markdown?
// https://github.com/noties/Markwon
//    https://noties.io/Markwon/
//    https://github.com/noties/Markwon/blob/master/sample/src/main/java/io/noties/markwon/sample/latex/LatexActivity.java
// https://github.com/signalapp/Signal-Android/issues/5534


// Color picking: https://github.com/martin-stone/hsv-alpha-color-picker-android
// Now for menu redesign
//    https://stackoverflow.com/questions/21329132/android-custom-dropdown-popup-menu
public class NoteFragment extends Fragment {

    private NoteViewModel noteViewModel;
    private RecyclerView list;
    private FastAdapter<NoteItem> fastAdapter;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findViews(view);
        prepareList(view);
    }

    private void findViews(View view) {
        list = view.findViewById(R.id.list);
    }

    private void prepareList(View view) {
        ItemAdapter<NoteItem> itemAdapter = new ItemAdapter<>();
        fastAdapter = FastAdapter.with(itemAdapter);


        fastAdapter.setOnClickListener((view1, noteItemIAdapter, noteItem, integer) -> {
            final String name = noteItem.getNote().getName();
            Log.i("Clicked item", "Note name: "+name);
//            final NoteQuery noteQuery = new NoteQuery(getContext());
//            noteQuery.getContent(name, content -> {
//                Intent intent = new Intent(getContext(), NoteViewActivity.class);
//                intent.putExtra("name", name);
//                intent.putExtra("content", content);
//                startActivity(intent);
//            });
            Intent intent = new Intent(getContext(), NoteViewActivity.class);
            intent.putExtra("name", name);
            intent.putExtra("content", noteItem.getNote().getContent());
            startActivity(intent);
            return true;
        });

        noteViewModel.getNotes().observe(getViewLifecycleOwner(), notes -> itemAdapter.set(notes.stream().map(note -> {
            NoteItem item = new NoteItem();
            item.setNote(note);
            return item;
        }).collect(Collectors.toList())));

        list.setAdapter(fastAdapter);
        list.setLayoutManager(new LinearLayoutManager(view.getContext()));
        list.addItemDecoration(new DividerItemDecoration(view.getContext(), LinearLayoutManager.VERTICAL));
    }


//    @Override
//    protected void prepareAdd() {
//        super.prepareAdd();
//        add.setOnClickListener(v -> {
//            Intent intent = new Intent(getContext(), NoteEditActivity.class);
//            startActivityForResult(intent, REQ_ADD);
//        });
//    }
//
//    @Override
//    protected void prepareDelete() {
//        super.prepareDelete();
//        add.setOnClickListener(v -> {
//            final NoteQuery noteQuery = new NoteQuery(getContext());
//            noteQuery.delete(adapter.getSelected(), x -> {});
//        });
//    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //TODO scroll to new item and highlight it?
    }
}