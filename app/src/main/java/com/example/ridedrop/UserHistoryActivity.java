package com.example.ridedrop;

import android.content.ContentValues;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class UserHistoryActivity extends AppCompatActivity {

    private ListView historyListView;
    private HistoryAdapter historyAdapter;
    private ArrayList<HistoryAdapter.HistoryItem> historyList;
    private DatabaseReference rideRequestsRef;
    private TextView tvHistoryCount;
    private LinearLayout emptyStateLayout;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_history);

        initViews();
        setupToolbar();
        loadHistoryData();
    }

    private void initViews() {
        historyListView = findViewById(R.id.historyListView);
        tvHistoryCount = findViewById(R.id.tvHistoryCount);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        toolbar = findViewById(R.id.toolbar);

        historyList = new ArrayList<>();
        historyAdapter = new HistoryAdapter(this, historyList);
        historyListView.setAdapter(historyAdapter);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadHistoryData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String currentUserId = currentUser.getUid();
        rideRequestsRef = FirebaseDatabase.getInstance().getReference("rideRequests");

        // Show loading state
        showLoadingState();

        rideRequestsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                historyList.clear();

                for (DataSnapshot driverSnapshot : snapshot.getChildren()) {
                    String driverId = driverSnapshot.getKey();

                    for (DataSnapshot rideSnapshot : driverSnapshot.getChildren()) {
                        String rideId = rideSnapshot.getKey();

                        if (rideSnapshot.hasChild(currentUserId)) {
                            DataSnapshot userSnapshot = rideSnapshot.child(currentUserId);

                            String status = userSnapshot.child("status").getValue(String.class);
                            String userName = userSnapshot.child("userName").getValue(String.class);
                            String userPhone = userSnapshot.child("userPhone").getValue(String.class);
                            Long timestamp = userSnapshot.child("requestTimestamp").getValue(Long.class);

                            if (timestamp == null) {
                                timestamp = System.currentTimeMillis(); // Default to current time
                            }

                            // Insert data into ContentProvider
                            ContentValues values = new ContentValues();
                            values.put("status", status != null ? status : "Unknown");
                            values.put("userName", userName);
                            values.put("userPhone", userPhone);
                            values.put("timestamp", timestamp);
                            values.put("rideId", rideId);
                            values.put("driverId", driverId);
                            getContentResolver().insert(RideHistoryProvider.CONTENT_URI, values);

                            // Add to local list for display
                            HistoryAdapter.HistoryItem item = new HistoryAdapter.HistoryItem(
                                    status != null ? status : "Unknown",
                                    userName,
                                    userPhone,
                                    timestamp,
                                    rideId,
                                    driverId
                            );

                            historyList.add(item);
                        }
                    }
                }

                // Sort by timestamp (newest first)
                historyList.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));

                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("UserHistoryActivity", "Database error: " + error.getMessage());
                Toast.makeText(UserHistoryActivity.this, "Error loading ride history.", Toast.LENGTH_SHORT).show();
                showEmptyState();
            }
        });
    }

    private void updateUI() {
        if (historyList.isEmpty()) {
            showEmptyState();
        } else {
            showHistoryList();
            updateHistoryCount();
        }
        historyAdapter.notifyDataSetChanged();
    }

    private void showLoadingState() {
        historyListView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);
        tvHistoryCount.setText("Loading...");
    }

    private void showEmptyState() {
        historyListView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);
        tvHistoryCount.setText("0 items");
    }

    private void showHistoryList() {
        historyListView.setVisibility(View.VISIBLE);
        emptyStateLayout.setVisibility(View.GONE);
    }

    private void updateHistoryCount() {
        int count = historyList.size();
        tvHistoryCount.setText(count + (count == 1 ? " item" : " items"));
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
