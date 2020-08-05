package com.python.companion.ui.general.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.biometrics.BiometricManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.snackbar.Snackbar;
import com.python.companion.R;
import com.python.companion.db.constant.NoteQuery;
import com.python.companion.security.Guard;
import com.python.companion.security.password.PassGuard;
import com.python.companion.security.password.PassResetDialog;
import com.python.companion.ui.general.settings.port.ExportActivity;
import com.python.companion.ui.general.settings.port.ImportActivity;
import com.python.companion.ui.jubileum.dialog.TogetherDialog;
import com.python.companion.util.MeasurementUtil;

public class SettingsActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_EXPORT = 1;
    private static final int REQUEST_CODE_IMPORT = 2;

    private View layout, importView, exportView, dateView, changepassView, biometricsView, resetPassView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        findGlobalViews();
        setText();
        setupClicks();
        setupActionBar();
    }

    private void findGlobalViews() {
        layout = findViewById(R.id.activity_settings_layout);
        importView = findViewById(R.id.activity_settings_import);
        exportView = findViewById(R.id.activity_settings_export);
        dateView = findViewById(R.id.activity_settings_date);
        changepassView = findViewById(R.id.activity_settings_security_changepass);
        biometricsView = findViewById(R.id.activity_settings_security_biometrics);
        resetPassView = findViewById(R.id.activity_settings_security_reset);
    }

    private void setText() {
        SharedPreferences preferences = getSharedPreferences(getString(R.string.pass_preferences), Context.MODE_PRIVATE);
        @Guard.Type int current = preferences.getInt("GuardType", Guard.TYPE_PASSGUARD);
        TextView biometricsText = findViewById(R.id.activity_settings_txt5);
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
        dateView.setOnClickListener(v -> { //TODO: Test! what is right sharedprefs register and key?
            TogetherDialog dialog = new TogetherDialog.Builder()
                    .setStartDate(MeasurementUtil.getTogether(this))
                    .setFinishListener(() -> Snackbar.make(layout, "Successfully set date", Snackbar.LENGTH_LONG).show()).build();
            dialog.show(getSupportFragmentManager(), null);
        });
        changepassView.setOnClickListener(v -> ((PassGuard) Guard.getGuard(Guard.TYPE_PASSGUARD)).setPass(getSupportFragmentManager(), this, () -> {}, error -> Snackbar.make(layout, error, Snackbar.LENGTH_LONG).show()));
        biometricsView.setOnClickListener(v -> changeAuthType());
        resetPassView.setOnClickListener(v -> {
            PassResetDialog dialog = new PassResetDialog.Builder()
                    .setAcceptListener(() -> {
                        NoteQuery query = new NoteQuery(SettingsActivity.this);
                        query.deleteSecure(v1 -> {
                            getSharedPreferences(getString(R.string.pass_preferences), Context.MODE_PRIVATE).edit().remove(getString(R.string.pass_preferences_key_pass)).apply();
                            Snackbar.make(layout, "Password reset successfully", Snackbar.LENGTH_LONG).show();
                        });
                    })
                    .build();
            dialog.show(getSupportFragmentManager(), null);
        });
    }

    private void setupActionBar() {
        Toolbar myToolbar = findViewById(R.id.activity_settings_toolbar);
        setSupportActionBar(myToolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
actionbar.setTitle("Categories");
        }
    }

    private void changeAuthType() {
        SharedPreferences preferences = getSharedPreferences(getString(R.string.pass_preferences), Context.MODE_PRIVATE);
        final @Guard.Type int current = preferences.getInt("GuardType", Guard.TYPE_PASSGUARD);
        final @Guard.Type int newtype = current == Guard.TYPE_PASSGUARD ? Guard.TYPE_BIOGUARD : Guard.TYPE_PASSGUARD;
        if (newtype == Guard.TYPE_BIOGUARD) { // Check whether biometrics are available before changing to use it
            BiometricManager manager = getSystemService(BiometricManager.class);
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
        if (!PassGuard.passIsSet(this)) { // Password is primary fallback. It must be set before changing to other strategies
            ((PassGuard) PassGuard.getGuard(Guard.TYPE_PASSGUARD)).setPass(getSupportFragmentManager(), this, () -> {
                    preferences.edit().putInt("GuardType", newtype).apply();
                    Guard.setGuardType(newtype);
                    TextView biometricsText = findViewById(R.id.activity_settings_txt5);
                    biometricsText.setText(newtype == Guard.TYPE_BIOGUARD ? "Stop using biometrics" : "Use biometrics");
                }, error -> Snackbar.make(layout, error, Snackbar.LENGTH_LONG));
        } else {
            preferences.edit().putInt("GuardType", newtype).apply();
            Guard.setGuardType(newtype);
            TextView biometricsText = findViewById(R.id.activity_settings_txt5);
            biometricsText.setText(newtype == Guard.TYPE_BIOGUARD ? "Stop using biometrics" : "Use biometrics");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_EXPORT && resultCode == RESULT_OK && data != null) {
            Uri userChosenUri = data.getData();
            assert userChosenUri != null;
            Intent intent = new Intent(this, ExportActivity.class);
            intent.putExtra("uri", userChosenUri);
            startActivity(intent);
        } else if (requestCode == REQUEST_CODE_IMPORT && resultCode == RESULT_OK && data != null) {
            Uri userChosenUri = data.getData();
            assert userChosenUri != null;
            Intent intent = new Intent(this, ImportActivity.class);
            intent.putExtra("uri", userChosenUri);
            startActivity(intent);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            finish();
        return super.onOptionsItemSelected(item);
    }
}
