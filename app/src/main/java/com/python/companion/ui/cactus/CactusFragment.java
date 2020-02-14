package com.python.companion.ui.cactus;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.python.companion.R;

public class CactusFragment extends Fragment {

    private CactusViewModel cactusViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        cactusViewModel = new ViewModelProvider(this).get(CactusViewModel.class);
        View root = inflater.inflate(R.layout.fragment_cactus, container, false);
//        final TextView textView = root.findViewById(R.id.text_gallery);
//        cactusViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
//            @Override
//            public void onChanged(@Nullable String s) {
//                textView.setText(s);
//            }
//        });
        return root;
    }
}