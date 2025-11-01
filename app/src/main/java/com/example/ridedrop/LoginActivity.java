package com.example.ridedrop;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private FirebaseAuth mAuth;
    private TextView registerTextView;

    // Variables to hold rideId and driverUid (nullable)
    private String rideId, driverUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);
        loginButton = findViewById(R.id.buttonLogin);
        registerTextView = findViewById(R.id.textViewRegister);

        // Receive rideId and driverUid from previous activity if passed
        Intent intent = getIntent();
        rideId = intent.getStringExtra("rideId");
        driverUid = intent.getStringExtra("driverUid");

        Log.d(TAG, "Received rideId = " + rideId + ", driverUid = " + driverUid);

        // Do NOT disable login if rideId or driverUid are null
        // This way login works both when those extras are passed or not

        loginButton.setOnClickListener(v -> loginUser());

        registerTextView.setOnClickListener(v -> navigateToRegisterActivity());
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(LoginActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Login successful");

                        // Prepare intent to next activity (RoleSelectionActivity)
                        Intent intent = new Intent(LoginActivity.this, RoleSelectionActivity.class);

                        // Pass rideId and driverUid only if they exist (not null)
                        if (rideId != null && driverUid != null) {
                            intent.putExtra("rideId", rideId);
                            intent.putExtra("driverUid", driverUid);
                        }

                        startActivity(intent);
                        finish();
                    } else {
                        Log.w(TAG, "Authentication failed", task.getException());
                        Toast.makeText(LoginActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToRegisterActivity() {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }
}
