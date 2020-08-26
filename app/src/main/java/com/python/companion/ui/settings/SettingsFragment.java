package com.python.companion.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.biometrics.BiometricManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.python.companion.R;
import com.python.companion.db.constant.NoteQuery;
import com.python.companion.security.Guard;
import com.python.companion.security.password.PassGuard;
import com.python.companion.security.password.PassResetDialog;
import com.python.companion.ui.anniversary.dialog.TogetherDialog;
import com.python.companion.ui.settings.port.ExportActivity;
import com.python.companion.ui.settings.port.ImportActivity;
import com.python.companion.util.AnniversaryUtil;

import static android.app.Activity.RESULT_OK;

public class SettingsFragment extends Fragment {
    private static final int REQUEST_CODE_EXPORT = 1;
    private static final int REQUEST_CODE_IMPORT = 2;

    private View layout, importView, exportView, dateView, changepassView, biometricsView, resetPassView;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        findGlobalViews(view);
        setText(view);
        setupClicks();
    }

    private void findGlobalViews(View view) {
        layout = view.findViewById(R.id.activity_settings_layout);
        importView = view.findViewById(R.id.activity_settings_import);
        exportView = view.findViewById(R.id.activity_settings_export);
        dateView = view.findViewById(R.id.activity_settings_date);
        changepassView = view.findViewById(R.id.activity_settings_security_changepass);
        biometricsView = view.findViewById(R.id.activity_settings_security_biometrics);
        resetPassView = view.findViewById(R.id.activity_settings_security_reset);
    }

    private void setText(View view) {
        SharedPreferences preferences = view.getContext().getSharedPreferences(getString(R.string.pass_preferences), Context.MODE_PRIVATE);
        @Guard.Type int current = preferences.getInt("GuardType", Guard.TYPE_PASSGUARD);
        TextView biometricsText = view.findViewById(R.id.activity_settings_txt5);
        biometricsText.setText(current == Guard.TYPE_BIOGUARD ? "Stop using biometrics" : "Use biometrics");
    }

    private void setupClicks() {
        importView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            Intent finalIntent = Intent.createChooser(intent, "Select file to import from");
            startActivityForResult(finalIntent, REQUEST_CODE_IMPORT);
        });
        exportView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            Intent finalIntent = Intent.createChooser(intent, "Select location to export to");
            startActivityForResult(finalIntent, REQUEST_CODE_EXPORT);
        });
        dateView.setOnClickListener(v -> {
            TogetherDialog dialog = new TogetherDialog.Builder()
                    .setStartDate(AnniversaryUtil.getTogether(layout.getContext()))
                    .setFinishListener(() -> Snackbar.make(layout, "Successfully set date", Snackbar.LENGTH_LONG).show()).build();
            dialog.show(getChildFragmentManager(), null);
        });
        changepassView.setOnClickListener(v -> ((PassGuard) Guard.getGuard(Guard.TYPE_PASSGUARD)).setPass(getChildFragmentManager(), layout.getContext(), () -> {}, error -> Snackbar.make(layout, error, Snackbar.LENGTH_LONG).show()));
        biometricsView.setOnClickListener(v -> changeAuthType());
        resetPassView.setOnClickListener(v -> {
            PassResetDialog dialog = new PassResetDialog.Builder()
                    .setAcceptListener(() -> {
                        NoteQuery query = new NoteQuery(v.getContext());
                        query.deleteSecure(v1 -> {
                            v.getContext().getSharedPreferences(getString(R.string.pass_preferences), Context.MODE_PRIVATE).edit().remove(getString(R.string.pass_preferences_key_pass)).apply();
                            Snackbar.make(layout, "Password reset successfully", Snackbar.LENGTH_LONG).show();
                        });
                    })
                    .build();
            dialog.show(getChildFragmentManager(), null);
        });
    }

    private void changeAuthType() {
        SharedPreferences preferences = layout.getContext().getSharedPreferences(getString(R.string.pass_preferences), Context.MODE_PRIVATE);
        final @Guard.Type int current = preferences.getInt("GuardType", Guard.TYPE_PASSGUARD);
        final @Guard.Type int newtype = current == Guard.TYPE_PASSGUARD ? Guard.TYPE_BIOGUARD : Guard.TYPE_PASSGUARD;
        if (newtype == Guard.TYPE_BIOGUARD) { // Check whether biometrics are available before changing to use it
            BiometricManager manager = layout.getContext().getSystemService(BiometricManager.class);
            switch (manager.canAuthenticate()) {
                case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                    Snackbar.make(layout, "Cannot use biometrics. No biometric enrolled!", Snackbar.LENGTH_LONG).show();
                    return;
                case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                    Snackbar.make(layout, "Cannot use biometrics. No biometric hardware available", Snackbar.LENGTH_LONG).show();
                    return;
                case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                    Snackbar.make(layout, "Cannot use biometrics. Biometric hardware is in use at this time", Snackbar.LENGTH_LONG).show();
                    return;
                case BiometricManager.BIOMETRIC_SUCCESS:
                    break;
            }
        }
        if (!PassGuard.passIsSet(layout.getContext())) { // Password is primary fallback. It must be set before changing to other strategies
            ((PassGuard) PassGuard.getGuard(Guard.TYPE_PASSGUARD)).setPass(getChildFragmentManager(), layout.getContext(), () -> {
                    preferences.edit().putInt("GuardType", newtype).apply();
                    Guard.setGuardType(newtype);
                    TextView biometricsText = layout.findViewById(R.id.activity_settings_txt5);
                    biometricsText.setText(newtype == Guard.TYPE_BIOGUARD ? "Stop using biometrics" : "Use biometrics");
                }, error -> Snackbar.make(layout, error, Snackbar.LENGTH_LONG));
        } else {
            preferences.edit().putInt("GuardType", newtype).apply();
            Guard.setGuardType(newtype);
            TextView biometricsText = layout.findViewById(R.id.activity_settings_txt5);
            biometricsText.setText(newtype == Guard.TYPE_BIOGUARD ? "Stop using biometrics" : "Use biometrics");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_EXPORT && resultCode == RESULT_OK && data != null) {
            Uri userChosenUri = data.getData();
            assert userChosenUri != null;
            Intent intent = new Intent(layout.getContext(), ExportActivity.class);
            intent.putExtra("uri", userChosenUri);
            startActivity(intent);
        } else if (requestCode == REQUEST_CODE_IMPORT && resultCode == RESULT_OK && data != null) {
            Uri userChosenUri = data.getData();
            assert userChosenUri != null;
            Intent intent = new Intent(layout.getContext(), ImportActivity.class);
            intent.putExtra("uri", userChosenUri);
            startActivity(intent);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
