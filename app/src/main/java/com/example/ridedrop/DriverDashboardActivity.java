package com.example.ridedrop;
import com.example.ridedrop.DriverRidesFragment;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DriverDashboardActivity extends AppCompatActivity {

    BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_dashboard);

        bottomNav = findViewById(R.id.driver_bottom_nav);

        // Load default fragment
        loadFragment(new DriverRidesFragment());

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_rides) {
                selectedFragment = new DriverRidesFragment();

            } else if (itemId == R.id.nav_history) {
                selectedFragment = new DriverHistoryFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new DriverProfileFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }

            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.driver_fragment_container, fragment)
                .commit();
    }
}