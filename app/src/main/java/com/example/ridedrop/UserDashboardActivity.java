package com.example.ridedrop;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserDashboardActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RideAdapter rideAdapter;

    private List<DriverRide> allFutureRides = new ArrayList<>();
    private List<DriverRide> filteredRides = new ArrayList<>();

    private EditText filterStart, filterDestination, filterDate;
    private Button btnFilter, btnClearFilter, btnPublishRide;
    private ImageButton btnSelectDate;

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        // Initialize Views
        recyclerView = findViewById(R.id.recyclerView);
        filterStart = findViewById(R.id.editStartFilter);
        filterDestination = findViewById(R.id.editDestinationFilter);
        filterDate = findViewById(R.id.editDateFilter);
        btnFilter = findViewById(R.id.btnFilter);
        btnClearFilter = findViewById(R.id.btnClearFilter);
        btnPublishRide = findViewById(R.id.btnPublishRide);
        btnSelectDate = findViewById(R.id.btnSelectDate);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        rideAdapter = new RideAdapter(this, filteredRides, true); // showBookButton = true
        recyclerView.setAdapter(rideAdapter);

        // Open Login Activity for publishing ride
        btnPublishRide.setOnClickListener(v -> {
            Intent intent = new Intent(UserDashboardActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        // Apply Filter
        btnFilter.setOnClickListener(v -> applyFilters());

        // Clear Filter
        btnClearFilter.setOnClickListener(v -> {
            filterStart.setText("");
            filterDestination.setText("");
            filterDate.setText("");
            filteredRides.clear();
            filteredRides.addAll(allFutureRides);
            rideAdapter.notifyDataSetChanged();
        });

        // Select Date using Calendar
        btnSelectDate.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    UserDashboardActivity.this,
                    (view, year, month, dayOfMonth) -> {
                        String selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                        filterDate.setText(selectedDate);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            datePickerDialog.show();
        });

        // Load rides from Firebase
        loadAllFutureRides();
    }

    private void loadAllFutureRides() {
        DatabaseReference ridesRef = FirebaseDatabase.getInstance().getReference("rideDetails");
        String todayStr = sdf.format(new Date());

        ridesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allFutureRides.clear();

                for (DataSnapshot driverSnap : snapshot.getChildren()) {
                    String driverUid = driverSnap.getKey();

                    for (DataSnapshot rideSnap : driverSnap.getChildren()) {
                        String rideId = rideSnap.getKey();

                        String name = rideSnap.child("name").getValue(String.class);
                        String phone = rideSnap.child("phone").getValue(String.class);
                        String startPoint = rideSnap.child("startPoint").getValue(String.class);
                        String destination = rideSnap.child("destination").getValue(String.class);
                        String startTime = rideSnap.child("startTime").getValue(String.class);
                        String date = rideSnap.child("date").getValue(String.class);
                        String vehicleNo = rideSnap.child("vehicleNo").getValue(String.class);
                        String carBrand = rideSnap.child("carBrand").getValue(String.class);
                        String price = rideSnap.child("price").getValue(String.class);
                        String route = rideSnap.child("route").getValue(String.class);
                        String duration = rideSnap.child("duration").getValue(String.class);

                        String passengerCount = rideSnap.child("passengerCount").exists() ?
                                String.valueOf(rideSnap.child("passengerCount").getValue()) : "0";

                        String timestamp = rideSnap.child("timestamp").getValue(String.class);

                        if (date != null) {
                            try {
                                Date rideDate = sdf.parse(date);
                                Date todayDate = sdf.parse(todayStr);

                                if (rideDate != null && todayDate != null && !rideDate.before(todayDate)) {
                                    DriverRide ride = new DriverRide(
                                            name, phone, startPoint, destination,
                                            startTime, date, vehicleNo, carBrand,
                                            price, route, duration, passengerCount,
                                            timestamp, rideId, driverUid
                                    );
                                    allFutureRides.add(ride);
                                }
                            } catch (ParseException e) {
                                System.err.println("Date parse error for ride date: " + date);
                            }
                        }
                    }
                }

                filteredRides.clear();
                filteredRides.addAll(allFutureRides);
                rideAdapter.notifyDataSetChanged();

                if (filteredRides.isEmpty()) {
                    Toast.makeText(UserDashboardActivity.this, "No future rides available.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserDashboardActivity.this, "Failed to load rides", Toast.LENGTH_SHORT).show();
                System.err.println("Firebase error: " + error.getMessage());
            }
        });
    }

    private void applyFilters() {
        String start = filterStart.getText().toString().trim().toLowerCase();
        String destination = filterDestination.getText().toString().trim().toLowerCase();
        String date = filterDate.getText().toString().trim();

        List<DriverRide> filtered = new ArrayList<>();

        for (DriverRide ride : allFutureRides) {
            boolean matches = true;

            if (!TextUtils.isEmpty(start) && (ride.getStartPoint() == null || !ride.getStartPoint().toLowerCase().contains(start))) {
                matches = false;
            }

            if (!TextUtils.isEmpty(destination) && (ride.getDestination() == null || !ride.getDestination().toLowerCase().contains(destination))) {
                matches = false;
            }

            if (!TextUtils.isEmpty(date) && (ride.getDate() == null || !ride.getDate().equalsIgnoreCase(date))) {
                matches = false;
            }

            if (matches) {
                filtered.add(ride);
            }
        }

        filteredRides.clear();
        filteredRides.addAll(filtered);
        rideAdapter.notifyDataSetChanged();

        if (filtered.isEmpty()) {
            Toast.makeText(this, "No rides match your filter criteria.", Toast.LENGTH_SHORT).show();
        }
    }
}
