package com.example.ridedrop;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class RoleSelectionActivity extends AppCompatActivity {

    private Button userButton, driverButton;
    private FirebaseAuth mAuth;

    private static final String TAG = "RoleSelection";

    // rideId and driverUid passed from previous activity
    private String rideId, driverUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        mAuth = FirebaseAuth.getInstance();

        // Check if the user is logged in
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Get extras
        rideId = getIntent().getStringExtra("rideId");
        driverUid = getIntent().getStringExtra("driverUid");

        userButton = findViewById(R.id.buttonUser);
        driverButton = findViewById(R.id.buttonDriver);

        userButton.setOnClickListener(v -> saveRoleAndNavigate("User"));
        driverButton.setOnClickListener(v -> saveRoleAndNavigate("Driver"));
    }

    private void saveRoleAndNavigate(String role) {
        String uid = mAuth.getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(uid);

        ref.child("role").setValue(role).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Role saved as: " + role);
                Toast.makeText(this, "Role saved: " + role, Toast.LENGTH_SHORT).show();

                if (role.equals("User")) {
                    checkUserRideDetails(uid);
                } else if (role.equals("Driver")) {
                    checkDriverRideDetails(uid);
                }

            } else {
                Exception e = task.getException();
                Log.e(TAG, "Failed to save role", e);
                Toast.makeText(this, "Failed to save role: " + (e != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkUserRideDetails(String uid) {
        DatabaseReference userDetailsRef = FirebaseDatabase.getInstance().getReference("userRideDetails").child(uid);

        userDetailsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Intent intent;
                if (dataSnapshot.exists()) {
                    intent = new Intent(RoleSelectionActivity.this, UserDashboardActivity.class);
                } else {
                    intent = new Intent(RoleSelectionActivity.this, SendOtpActivity.class);
                    intent.putExtra("role", "User");
                }

                if (rideId != null) intent.putExtra("rideId", rideId);
                if (driverUid != null) intent.putExtra("driverUid", driverUid);

                startActivity(intent);
                finish();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error checking user ride details", databaseError.toException());
            }
        });
    }

    private void checkDriverRideDetails(String uid) {
        DatabaseReference rideDetailsRef = FirebaseDatabase.getInstance().getReference("rideDetails").child(uid);

        rideDetailsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Intent intent;
                if (dataSnapshot.exists()) {
                    intent = new Intent(RoleSelectionActivity.this, DriverDashboardActivity.class);
                } else {
                    intent = new Intent(RoleSelectionActivity.this, SendOtpActivity.class);
                    intent.putExtra("role", "Driver");
                }

                if (rideId != null) intent.putExtra("rideId", rideId);
                if (driverUid != null) intent.putExtra("driverUid", driverUid);

                startActivity(intent);
                finish();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error checking driver ride details", error.toException());
            }
        });
    }
}
