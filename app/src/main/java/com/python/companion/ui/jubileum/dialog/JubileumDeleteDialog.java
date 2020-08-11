package com.python.companion.ui.jubileum.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.python.companion.R;
import com.python.companion.db.interact.MeasurementStore;
import com.python.companion.db.pojo.measurement.MeasurementWithParentNames;
import com.python.companion.ui.general.dialog.DialogAcceptListener;
import com.python.companion.ui.general.dialog.DialogCancelListener;

public class JubileumDeleteDialog extends DialogFragment {

    @SuppressWarnings("unused")
    public static class Builder {
        private DialogCancelListener dialogCancelListener = null;
        private DialogAcceptListener dialogAcceptListener = null;

        private String existsString = "", questionString = "", warningString = "";
        private MeasurementWithParentNames measurementWithParentName;

        public Builder setCancelListener(DialogCancelListener dialogCancelListener) {
            this.dialogCancelListener = dialogCancelListener;
            return this;
        }

        public Builder setDeleteListener(DialogAcceptListener dialogAcceptListener) {
            this.dialogAcceptListener = dialogAcceptListener;
            return this;
        }

        public Builder setExistsText(String text) {
            this.existsString = text;
            return this;
        }

        public Builder setQuestionText(String text) {
            this.questionString = text;
            return this;
        }

        public Builder setWarningText(String text) {
            this.warningString = text;
            return this;
        }

        public Builder setMeasurementWithParentName(MeasurementWithParentNames measurementWithParentNames) {
            this.measurementWithParentName = measurementWithParentNames;
            return this;
        }

        public JubileumDeleteDialog build() {
            if (measurementWithParentName == null)
                throw new IllegalStateException("Caller must provide Measurement which will be deleted with builder.setMeasurement(measurement)");
            return new JubileumDeleteDialog(dialogCancelListener, dialogAcceptListener, existsString, questionString, warningString, measurementWithParentName);
        }
    }

    protected TextView existsView, questionView, warningView;
    protected TextView measurementNameView, measurementEqualityView, measurementDefinitionView;
    protected Button cancelButton, overrideButton;

    protected @Nullable DialogCancelListener cancelListener;
    protected @Nullable DialogAcceptListener deleteListener;
    protected String existsText, questionText, warningText;
    protected @NonNull MeasurementWithParentNames measurementWithParentNames;

    protected JubileumDeleteDialog(@Nullable DialogCancelListener cancelListener, @NonNull DialogAcceptListener deleteListener, String existsText, String questionText, String warningText, @NonNull MeasurementWithParentNames measurementWithParentNames) {
        this.cancelListener = cancelListener;
        this.deleteListener = deleteListener;
        this.existsText = existsText;
        this.warningText = warningText;
        this.questionText = questionText;
        this.measurementWithParentNames = measurementWithParentNames;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_measurement_delete, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findGlobalViews(view);
        setText();
        setMeasurement();
        prepareButtons();
    }

    @CallSuper
    protected void findGlobalViews(View view) {
        existsView = view.findViewById(R.id.dialog_measurement_delete_text);
        questionView = view.findViewById(R.id.dialog_measurement_delete_question);
        warningView = view.findViewById(R.id.dialog_measurement_delete_warning);

        measurementNameView = view.findViewById(R.id.item_measurement_name);
        measurementEqualityView = view.findViewById(R.id.item_measurement_equality);
        measurementDefinitionView = view.findViewById(R.id.item_measurement_definition);

        cancelButton = view.findViewById(R.id.dialog_measurement_delete_cancel);
        overrideButton = view.findViewById(R.id.dialog_measurement_delete_override);
    }

    protected void setText() {
        existsView.setText(existsText);
        questionView.setText(questionText);
        warningView.setText(warningText);
    }

    protected void setMeasurement() {
        measurementNameView.setText(measurementWithParentNames.measurement.getNamePlural());
        measurementEqualityView.setText("1 "+ measurementWithParentNames.measurement.getNameSingular()+" =");
        long amount = measurementWithParentNames.measurement.getAmount();
        measurementDefinitionView.setText(amount+" "+ (amount == 1 ? measurementWithParentNames.parentSingular : measurementWithParentNames.parentPlural));
    }

    private void prepareButtons() {
        cancelButton.setOnClickListener(v -> {
            if (cancelListener != null) {
                cancelListener.onCancel();
            }
            dismiss();
        });
        overrideButton.setOnClickListener(v -> {

            if (deleteListener != null)
                MeasurementStore.delete(measurementWithParentNames.measurement, getContext(), () -> deleteListener.onAccept());
            else
                MeasurementStore.delete(measurementWithParentNames.measurement, getContext(), () -> {});
            dismiss();
        });
    }
}
