package com.python.companion.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.python.companion.R;
import com.python.companion.ui.cactus.dialog.TogetherDialog;
import com.python.companion.ui.general.settings.SettingsActivity;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each menu should be considered as top level destinations
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_cactus, R.id.nav_cactus,
                R.id.nav_tools, R.id.nav_share, R.id.nav_send)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        if (!getSharedPreferences(getString(R.string.measurement_preferences), MODE_PRIVATE).contains("together")) {
            TogetherDialog dialog = new TogetherDialog.Builder()
                    .setCancelListener(() -> {
                        Log.e("TOGETHER", "Cancelled period!!!");
                        Snackbar.make(drawer, "Default value '2017-11-08 (yyyy-MM-dd)' set", Snackbar.LENGTH_LONG)
                        .setAction(" Open Settings", v -> startActivity(new Intent(this, SettingsActivity.class)))
                        .show();
                    })
                    .setFinishListener(() -> Snackbar.make(drawer, "Successfully set date", Snackbar.LENGTH_LONG).show()).build();
            dialog.show(getSupportFragmentManager(), null);
        }
    }


    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }
}
