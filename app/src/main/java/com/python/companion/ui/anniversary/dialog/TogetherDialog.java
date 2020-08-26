package com.python.companion.ui.anniversary.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.python.companion.R;
import com.python.companion.ui.general.dialog.DialogAcceptListener;
import com.python.companion.ui.general.dialog.DialogCancelListener;
import com.python.companion.ui.general.picker.GoodDatePicker;
import com.python.companion.util.AnniversaryUtil;

import java.time.LocalDate;

@SuppressWarnings("WeakerAccess")
public class TogetherDialog extends DialogFragment {
    @SuppressWarnings("unused")
    public static class Builder {
        private DialogCancelListener dialogCancelListener = null;
        private DialogAcceptListener dialogAcceptListener = null;

        private LocalDate date;

        public Builder setCancelListener(DialogCancelListener dialogCancelListener) {
            this.dialogCancelListener = dialogCancelListener;
            return this;
        }

        public Builder setFinishListener(DialogAcceptListener dialogAcceptListener) {
            this.dialogAcceptListener = dialogAcceptListener;
            return this;
        }

        public Builder setStartDate(LocalDate date) {
            this.date = date;
            return this;
        }

        public TogetherDialog build() {
            return new TogetherDialog(dialogCancelListener, dialogAcceptListener, date);
        }
    }

    protected GoodDatePicker picker;
    protected Button cancelButton, acceptButton;


    protected @Nullable DialogCancelListener cancelListener;
    protected @Nullable DialogAcceptListener acceptListener;

    protected @Nullable LocalDate date;

    protected TogetherDialog(@Nullable DialogCancelListener cancelListener, @Nullable DialogAcceptListener acceptListener, @Nullable LocalDate startDate) {
        this.cancelListener = cancelListener;
        this.acceptListener = acceptListener;
        this.date = startDate;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (cancelListener != null)
            return new Dialog(getActivity(), getTheme()) {
                @Override
                public void onBackPressed() {
                    super.onBackPressed();
                    cancelListener.onCancel();
                }
            };
        return super.onCreateDialog(savedInstanceState);

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_together, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        findGlobalViews(view);
        setupClicks();
        if (date != null)
            picker.updateDate(date);
        else
            picker.updateDate(2017, 11, 8);

    }

    @CallSuper
    protected void findGlobalViews(View view) {
        picker = view.findViewById(R.id.dialog_together_picker);
        cancelButton = view.findViewById(R.id.dialog_together_cancel);
        acceptButton = view.findViewById(R.id.dialog_together_accept);
    }


    private void setupClicks() {
        cancelButton.setOnClickListener(v -> {
            if (cancelListener != null)
                cancelListener.onCancel();
            dismiss();
        });

        acceptButton.setOnClickListener(v -> {
            AnniversaryUtil.setTogether(picker.getDate(), getContext());
            if (acceptListener != null)
                acceptListener.onAccept();
            dismiss();
        });
    }


}