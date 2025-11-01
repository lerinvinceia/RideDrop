package com.example.ridedrop;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;

public class UserDetailsActivity extends AppCompatActivity {

    private TextView tvName, tvPhone, tvCarBrand, tvVehicleNo, tvStartPoint, tvDestination,
            tvDate, tvStartTime, tvDuration, tvPassengerCount, tvPrice, tvRoute, tvBookingStatus;

    private EditText etUserName, etPassengerCount, etUserPhone;
    private Button btnConfirmBooking, btnSeeHistory;

    private String driverUid, rideId, currentUserUid;
    private int totalPassengerCount, bookedPassengerCount;

    private static final int SMS_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        // Find views
        tvName = findViewById(R.id.tvName);
        tvPhone = findViewById(R.id.tvPhone);
        tvCarBrand = findViewById(R.id.tvCarBrand);
        tvVehicleNo = findViewById(R.id.tvVehicleNo);
        tvStartPoint = findViewById(R.id.tvStartPoint);
        tvDestination = findViewById(R.id.tvDestination);
        tvDate = findViewById(R.id.tvDate);
        tvStartTime = findViewById(R.id.tvStartTime);
        tvDuration = findViewById(R.id.tvDuration);
        tvPassengerCount = findViewById(R.id.tvPassengerCount);
        tvPrice = findViewById(R.id.tvPrice);
        tvRoute = findViewById(R.id.tvRoute);
        tvBookingStatus = findViewById(R.id.tvBookingStatus);

        etUserName = findViewById(R.id.etUserName);
        etPassengerCount = findViewById(R.id.etPassengerCount);
        etUserPhone = findViewById(R.id.etUserPhone);

        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);
        btnSeeHistory = findViewById(R.id.btnSeeHistory);

        driverUid = getIntent().getStringExtra("driverUid");
        rideId = getIntent().getStringExtra("rideId");

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) currentUserUid = currentUser.getUid();

        if (driverUid == null || rideId == null || currentUserUid == null) {
            Toast.makeText(this, "Missing ride or user details", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fetchRideDetails();
        checkBookingStatus();

        btnSeeHistory.setOnClickListener(v -> {
            Intent intent = new Intent(UserDetailsActivity.this, UserHistoryActivity.class);
            intent.putExtra("userName", etUserName.getText().toString().trim());
            intent.putExtra("userPhone", etUserPhone.getText().toString().trim());
            startActivity(intent);
        });
    }

    private void fetchRideDetails() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("rideDetails")
                .child(driverUid).child(rideId);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    tvName.setText("Name: " + snapshot.child("name").getValue(String.class));
                    tvPhone.setText("Phone: " + snapshot.child("phone").getValue(String.class));
                    tvCarBrand.setText("Car Brand: " + snapshot.child("carBrand").getValue(String.class));
                    tvVehicleNo.setText("Vehicle No: " + snapshot.child("vehicleNo").getValue(String.class));
                    tvStartPoint.setText("Start Point: " + snapshot.child("startPoint").getValue(String.class));
                    tvDestination.setText("Destination: " + snapshot.child("destination").getValue(String.class));
                    tvDate.setText("Date: " + snapshot.child("date").getValue(String.class));
                    tvStartTime.setText("Start Time: " + snapshot.child("startTime").getValue(String.class));
                    tvDuration.setText("Duration: " + snapshot.child("duration").getValue(String.class));
                    tvPassengerCount.setText("Total Passengers: " + snapshot.child("passengerCount").getValue(String.class));
                    tvPrice.setText("Price: " + snapshot.child("price").getValue(String.class));
                    tvRoute.setText("Route: " + snapshot.child("route").getValue(String.class));

                    String totalCountStr = snapshot.child("passengerCount").getValue(String.class);
                    totalPassengerCount = totalCountStr != null ? Integer.parseInt(totalCountStr) : 0;
                    Integer bookedCount = snapshot.child("bookedPassengerCount").getValue(Integer.class);
                    bookedPassengerCount = bookedCount != null ? bookedCount : 0;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserDetailsActivity.this, "Failed to load ride details", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void checkBookingStatus() {
        DatabaseReference reqRef = FirebaseDatabase.getInstance().getReference("rideRequests")
                .child(driverUid).child(rideId).child(currentUserUid);

        reqRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.child("status").getValue(String.class);
                    Long requestedPassengers = snapshot.child("requestedPassengers").getValue(Long.class);
                    String phone = snapshot.child("userPhone").getValue(String.class);
                    String userName = snapshot.child("userName").getValue(String.class);

                    etUserName.setText(userName != null ? userName : "");
                    etPassengerCount.setText(String.valueOf(requestedPassengers != null ? requestedPassengers : 1));
                    etUserPhone.setText(phone != null ? phone : "");
                    tvBookingStatus.setText("Booking Status: " + (status != null ? status : "Unknown"));

                    if ("Accepted".equalsIgnoreCase(status)) {
                        btnConfirmBooking.setEnabled(false);
                        btnConfirmBooking.setText("Booking Accepted");
                        btnConfirmBooking.setOnClickListener(null);
                    } else if ("Pending".equalsIgnoreCase(status)) {
                        btnConfirmBooking.setEnabled(true);
                        btnConfirmBooking.setText("Cancel Request");
                        btnConfirmBooking.setOnClickListener(v -> cancelBookingRequest());
                    } else {
                        btnConfirmBooking.setEnabled(true);
                        btnConfirmBooking.setText("Send Booking Request");
                        btnConfirmBooking.setOnClickListener(v -> sendBookingRequest());
                    }
                } else {
                    btnConfirmBooking.setEnabled(true);
                    btnConfirmBooking.setText("Send Booking Request");
                    btnConfirmBooking.setOnClickListener(v -> sendBookingRequest());
                    tvBookingStatus.setText("Booking Status: None");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserDetailsActivity.this, "Failed to check status", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendBookingRequest() {
        String userName = etUserName.getText().toString().trim();
        String countStr = etPassengerCount.getText().toString().trim();
        String userPhone = etUserPhone.getText().toString().trim();

        if (userName.isEmpty()) {
            etUserName.setError("Enter name");
            etUserName.requestFocus();
            return;
        }
        if (userPhone.isEmpty()) {
            etUserPhone.setError("Enter phone number");
            etUserPhone.requestFocus();
            return;
        }

        int count;
        try {
            count = Integer.parseInt(countStr);
            if (count <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            etPassengerCount.setError("Enter valid number");
            etPassengerCount.requestFocus();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("rideRequests")
                .child(driverUid).child(rideId).child(currentUserUid);

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("userName", userName);
        requestData.put("requestedPassengers", count);
        requestData.put("userPhone", userPhone);
        requestData.put("status", "Pending");
        requestData.put("requestTimestamp", System.currentTimeMillis());

        ref.setValue(requestData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Request sent", Toast.LENGTH_SHORT).show();
                tvBookingStatus.setText("Booking Status: Pending");
                btnConfirmBooking.setEnabled(true);
                btnConfirmBooking.setText("Cancel Request");
                btnConfirmBooking.setOnClickListener(v -> cancelBookingRequest());

                checkSmsPermissionAndStartService();

            } else {
                Toast.makeText(this, "Failed to send request", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cancelBookingRequest() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("rideRequests")
                .child(driverUid).child(rideId).child(currentUserUid);

        ref.child("status").setValue("Cancelled").addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Request cancelled", Toast.LENGTH_SHORT).show();
                tvBookingStatus.setText("Booking Status: Cancelled");
                btnConfirmBooking.setEnabled(true);
                btnConfirmBooking.setText("Send Booking Request");
                btnConfirmBooking.setOnClickListener(v2 -> sendBookingRequest());
            } else {
                Toast.makeText(this, "Failed to cancel request", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkSmsPermissionAndStartService() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        } else {
            startRideRequestService();
        }
    }

    private void startRideRequestService() {
        Intent serviceIntent = new Intent(this, RideRequestListenerService.class);
        serviceIntent.putExtra("rideId", rideId);
        serviceIntent.putExtra("driverUid", driverUid);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRideRequestService();
            } else {
                Toast.makeText(this, "SMS permission required to start service", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

