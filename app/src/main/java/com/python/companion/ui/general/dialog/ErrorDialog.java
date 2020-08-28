package com.python.companion.ui.general.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.python.companion.R;

public class ErrorDialog extends FixedDialogFragment {
    public static class Builder {
        private DialogAcceptListener dialogAcceptListener = null;

        private String titleString = "Error", subtitleString = "", problemString = "", solutionString = "";

        public Builder setAcceptListener(DialogAcceptListener dialogAcceptListener) {
            this.dialogAcceptListener = dialogAcceptListener;
            return this;
        }

        public Builder setTitle(String text) {
            this.titleString = text;
            return this;
        }

        public Builder setSubtitle(String text) {
            this.subtitleString = text;
            return this;
        }

        public Builder setProblem(String text) {
            this.problemString = text;
            return this;
        }

        public Builder setSolution(String text) {
            this.solutionString = text;
            return this;
        }


        public ErrorDialog build() {
            return new ErrorDialog(dialogAcceptListener, titleString, subtitleString, problemString, solutionString);
        }
    }

    protected TextView titleView, subtitleView, problemView, solutionView;
    protected Button acceptButton;

    protected @Nullable DialogAcceptListener acceptListener;
    protected String title, subtitle, problem, solution;

    protected ErrorDialog(@Nullable DialogAcceptListener acceptListener, String title, String subtitle, String problem, String solution) {
        this.acceptListener = acceptListener;
        this.title = title;
        this.subtitle = subtitle;
        this.problem = problem;
        this.solution = solution;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_error, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findGlobalViews(view);
        setText();
        prepareButton();
    }

    @CallSuper
    protected void findGlobalViews(View view) {
        titleView = view.findViewById(R.id.dialog_error_title);
        subtitleView = view.findViewById(R.id.dialog_error_subtitle);
        problemView = view.findViewById(R.id.dialog_error_problem);
        solutionView = view.findViewById(R.id.dialog_error_solution);

        acceptButton = view.findViewById(R.id.dialog_error_accept);
    }

    protected void setText() {
        titleView.setText(title);
        subtitleView.setText(subtitle);
        problemView.setText(problem);
        solutionView.setText(solution);
    }


    private void prepareButton() {
        acceptButton.setOnClickListener(v -> {
            if (acceptListener != null) {
                acceptListener.onAccept();
            }
            dismiss();
        });
    }
}
