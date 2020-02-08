package com.python.companion.ui.note;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.python.companion.R;
import com.python.companion.db.constant.NoteQuery;
import com.python.companion.db.entity.Note;
import com.python.companion.ui.note.activity.edit.NoteEditActivity;
import com.python.companion.ui.note.activity.NoteViewActivity;
import com.python.companion.ui.note.list.NoteSearcher;
import com.python.companion.ui.note.list.adapter.NoteAdapterAction;
import com.python.companion.ui.templates.Fragment;
import com.python.companion.ui.templates.adapter.action.ActionListener;
import com.python.companion.ui.templates.search.Searcher;

import java.util.List;

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
public class NoteFragment extends Fragment<Note> implements ActionListener<Note> {

    private NoteViewModel noteViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
    }

    @Override
    protected void prepareSearch() {
        search.setOnQueryTextListener(new NoteSearcher(new Searcher.EventListener<Note>() {
            @NonNull
            @Override
            public List<Note> onBeginSearch() {
                add.setVisibility(View.INVISIBLE);
                return adapter.getItems();
            }

            @Override
            public void onFinishSearch(List<Note> initial) {
                adapter.replaceAll(initial);
                add.setVisibility(View.VISIBLE);
            }

            @Override
            public void onReceiveFilteredContent(List<Note> filtered) {
                adapter.replaceAll(filtered);
                list.scrollToPosition(0);
            }
        }));
    }

    @Override
    protected void prepareList(View view) {
        RecyclerView list = view.findViewById(R.id.list);

        adapter = new NoteAdapterAction(this);
        noteViewModel.getNotes().observe(this, adapter);

        list.setAdapter(adapter);
        list.setLayoutManager(new LinearLayoutManager(view.getContext()));
        list.addItemDecoration(new DividerItemDecoration(view.getContext(), LinearLayoutManager.VERTICAL));
    }

    @Override
    public void onClick(Note note) {
        final String name = note.getName();
        Log.i("Clicked item", "Note name: "+name);
        final NoteQuery noteQuery = new NoteQuery(getContext());
        noteQuery.getContent(name, content -> {
            Intent intent = new Intent(getContext(), NoteViewActivity.class);
            intent.putExtra("name", name);
            intent.putExtra("content", content);
            startActivity(intent);
        });

    }

    @Override
    public boolean onLongClick(Note note) {
        return super.onLongClick(note);
    }

    @Override
    protected void prepareAdd(View view, boolean actionMode) {
        super.prepareAdd(view, actionMode);
        Log.i("NoteFragment", "Cliked add");
        if (!actionMode) {
            add.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), NoteEditActivity.class);
                startActivityForResult(intent, REQ_ADD);
//              Sample normal
//                final Markwon markwon = Markwon.create(getContext());// parse markdown and create styled text
//                final Spanned markdown = markwon.toMarkdown("**Hi there!** How *are you*?");// use it
//                Toast.makeText(getContext(), markdown, Toast.LENGTH_LONG).show();
//              Sample latex
//                final Markwon markwon = Markwon.builder(getContext()).usePlugin(JLatexMathPlugin.create(10)).build();
//                final Spanned markdown = markwon.toMarkdown("$$x^2\\text{a \\rightarrow a}$$");// use it
//                Toast.makeText(getContext(), markdown, Toast.LENGTH_LONG).show();
            });
        }
    }
}