package com.python.companion.ui.measurement.dialog;

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
import com.python.companion.db.constant.MeasurementQuery;
import com.python.companion.db.entity.Measurement;
import com.python.companion.ui.general.dialog.DialogAcceptListener;
import com.python.companion.ui.general.dialog.DialogCancelListener;
import com.python.companion.util.ThreadUtil;

import java.util.concurrent.Executors;

public class MeasurementDeleteDialog extends DialogFragment {

    @SuppressWarnings("unused")
    public static class Builder {
        private DialogCancelListener dialogCancelListener = null;
        private DialogAcceptListener dialogAcceptListener = null;

        private String existsString = "", questionString = "", warningString = "";
        private Measurement measurement;

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

        public Builder setMeasurement(Measurement measurement) {
            this.measurement = measurement;
            return this;
        }

        public MeasurementDeleteDialog build() {
            if (measurement == null)
                throw new IllegalStateException("Caller must provide Measurement which will be deleted with builder.setMeasurement(measurement)");
            return new MeasurementDeleteDialog(dialogCancelListener, dialogAcceptListener, existsString, questionString, warningString, measurement);
        }
    }

    protected TextView existsView, questionView, warningView;
    protected TextView measurementNameView, measurementEqualityView, measurementDefinitionView;
    protected Button cancelButton, overrideButton;

    protected @Nullable DialogCancelListener cancelListener;
    protected @Nullable DialogAcceptListener overrideListener;
    protected String existsText, questionText, warningText;
    protected @NonNull Measurement measurement;

    protected MeasurementDeleteDialog(@Nullable DialogCancelListener cancelListener, @Nullable DialogAcceptListener overrideListener, String existsText, String questionText, String warningText, @NonNull Measurement measurement) {
        this.cancelListener = cancelListener;
        this.overrideListener = overrideListener;
        this.existsText = existsText;
        this.warningText = warningText;
        this.questionText = questionText;
        this.measurement = measurement;
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
        measurementNameView.setText(measurement.getNamePlural());
        measurementEqualityView.setText("1 "+measurement.getNameSingular()+" =");
        Executors.newSingleThreadExecutor().execute(() -> {
            String prelude = (measurement.getCornerstoneType().isDurationEstimated()) ? "approx. " : "";
            long amount = measurement.getAmount();
            MeasurementQuery query = new MeasurementQuery(getContext());
            Measurement parent = query.findByID(measurement.getParentID());
            String parentname = amount == 1 ? parent.getNameSingular() : parent.getNamePlural();
            ThreadUtil.runOnUIThread(() -> measurementDefinitionView.setText(prelude+amount+" "+parentname));
        });
    }

    private void prepareButtons() {
        cancelButton.setOnClickListener(v -> {
            if (cancelListener != null) {
                cancelListener.onCancel();
            }
            dismiss();
        });
        overrideButton.setOnClickListener(v -> {
            if (overrideListener != null) {
                overrideListener.onAccept();
            }
            dismiss();
        });
    }
}
