package com.example.ridedrop;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private ImageView logoImageView, carImageView;
    private TextView appNameTextView, sloganTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Splash layout

        // Initialize views
        logoImageView = findViewById(R.id.logoImageView);
        appNameTextView = findViewById(R.id.appNameTextView);
        sloganTextView = findViewById(R.id.sloganTextView);
        carImageView = findViewById(R.id.carImageView);

        // Initially hide all except car (car will animate from left)
        logoImageView.setAlpha(0f);
        appNameTextView.setAlpha(0f);
        sloganTextView.setAlpha(0f);

        logoImageView.setTranslationY(-100f);
        appNameTextView.setTranslationY(100f);
        sloganTextView.setTranslationY(100f);

        // Animate logo fade in and slide down
        logoImageView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(200)
                .start();

        // Animate app name fade in and slide up
        appNameTextView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(800)
                .start();

        // Animate slogan fade in and slide up, then start car animation
        sloganTextView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(1200)
                .withEndAction(() -> animateCar())
                .start();
    }

    private void animateCar() {
        // Get screen width to move car across
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;

        // Start car just off left side
        carImageView.setTranslationX(-carImageView.getWidth());

        // Animate car moving across screen over 3 seconds
        carImageView.animate()
                .translationX(screenWidth)
                .setDuration(3000)
                .withEndAction(() -> {
                    // After animation ends, move to UserDashboardActivity after 500ms
                    new Handler().postDelayed(() -> {
                        startActivity(new Intent(MainActivity.this, UserDashboardActivity.class));
                        finish();
                    }, 500);
                })
                .start();
    }
}
