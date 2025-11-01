package com.example.ridedrop;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class DriverRidesFragment extends Fragment {

    private LinearLayout requestContainer;
    private String currentDriverUid;
    private static final int SMS_PERMISSION_CODE = 100;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_driver_rides, container, false);
        requestContainer = view.findViewById(R.id.requestContainer);
        currentDriverUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Request SMS permission if not granted
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        }

        loadRequestsGroupedByDate();
        return view;
    }

    private void loadRequestsGroupedByDate() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("rideRequests").child(currentDriverUid);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                requestContainer.removeAllViews();

                // Map<DateString, List<RequestData>>
                Map<String, List<RequestData>> groupedRequests = new TreeMap<>();

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                // Gather all requests grouped by date string
                for (DataSnapshot rideSnapshot : snapshot.getChildren()) {
                    String rideId = rideSnapshot.getKey();

                    for (DataSnapshot userSnapshot : rideSnapshot.getChildren()) {
                        String userId = userSnapshot.getKey();
                        String userName = userSnapshot.child("userName").getValue(String.class);
                        String status = userSnapshot.child("status").getValue(String.class);
                        Long requestedPassengersValue = userSnapshot.child("requestedPassengers").getValue(Long.class);
                        String userPhone = userSnapshot.child("userPhone").getValue(String.class);
                        Long timestamp = userSnapshot.child("requestTimestamp").getValue(Long.class); // Assuming timestamp stored

                        if (status == null) status = "Pending";
                        if (requestedPassengersValue == null) requestedPassengersValue = 1L;

                        // Show only Pending or Accepted requests (not declined or others)
                        if (!status.equalsIgnoreCase("Pending") && !status.equalsIgnoreCase("Accepted")) {
                            continue;
                        }

                        // Format date string for grouping
                        String dateKey;
                        if (timestamp != null) {
                            Date requestDate = new Date(timestamp);
                            dateKey = dateFormat.format(requestDate);
                        } else {
                            // If no timestamp, group under "Unknown Date"
                            dateKey = "Unknown Date";
                        }

                        RequestData requestData = new RequestData(rideId, userId, userName, status, requestedPassengersValue.intValue(), userPhone);

                        // Add to map
                        if (!groupedRequests.containsKey(dateKey)) {
                            groupedRequests.put(dateKey, new ArrayList<>());
                        }
                        groupedRequests.get(dateKey).add(requestData);
                    }
                }

                if (groupedRequests.isEmpty()) {
                    TextView emptyView = new TextView(getContext());
                    emptyView.setText("No ride requests found.");
                    requestContainer.addView(emptyView);
                    return;
                }

                // Display requests grouped by date with header
                for (Map.Entry<String, List<RequestData>> entry : groupedRequests.entrySet()) {
                    String date = entry.getKey();
                    List<RequestData> requests = entry.getValue();

                    // Create date header
                    TextView dateHeader = new TextView(getContext());
                    dateHeader.setText(formatDateHeader(date));
                    dateHeader.setTextSize(18);
                    dateHeader.setPadding(0, 20, 0, 10);
                    requestContainer.addView(dateHeader);

                    // Add each request under this date
                    for (RequestData req : requests) {
                        addRequestView(req);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load requests", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addRequestView(RequestData req) {
        View requestView = LayoutInflater.from(getContext()).inflate(R.layout.item_request, requestContainer, false);

        TextView tvRequestDetails = requestView.findViewById(R.id.tvRequestDetails);
        TextView tvRequestedPassengers = requestView.findViewById(R.id.tvRequestedPassengers);
        Button btnAccept = requestView.findViewById(R.id.btnAccept);
        Button btnDecline = requestView.findViewById(R.id.btnDecline);

        tvRequestDetails.setText(req.userName);
        tvRequestedPassengers.setText("Requested Passengers: " + req.requestedPassengers);

        if (req.status.equalsIgnoreCase("Pending")) {
            btnAccept.setEnabled(true);
            btnAccept.setText("Accept");
            btnAccept.setOnClickListener(v -> acceptRequest(req.rideId, req.userId, req.requestedPassengers, req.userPhone));
        } else if (req.status.equalsIgnoreCase("Accepted")) {
            btnAccept.setEnabled(false);
            btnAccept.setText("Accepted");
        }

        // Decline button always enabled so driver can decline even after accepting
        btnDecline.setEnabled(true);
        btnDecline.setOnClickListener(v -> declineRequest(req.rideId, req.userId, req.userPhone));

        requestContainer.addView(requestView);
    }

    private String formatDateHeader(String dateString) {
        if ("Unknown Date".equals(dateString)) {
            return dateString;
        }
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = inputFormat.parse(dateString);
            SimpleDateFormat outputFormat = new SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateString;
        }
    }

    private void acceptRequest(String rideId, String userId, int requestedPassengers, String userPhone) {
        DatabaseReference rideRef = FirebaseDatabase.getInstance().getReference("rideDetails")
                .child(currentDriverUid).child(rideId);

        rideRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Integer bookedCount = currentData.child("bookedPassengerCount").getValue(Integer.class);
                Integer totalCount = null;

                Object totalCountObj = currentData.child("passengerCount").getValue();
                if (totalCountObj instanceof String) {
                    totalCount = Integer.parseInt((String) totalCountObj);
                } else if (totalCountObj instanceof Long) {
                    totalCount = ((Long) totalCountObj).intValue();
                } else if (totalCountObj instanceof Integer) {
                    totalCount = (Integer) totalCountObj;
                }

                if (bookedCount == null) bookedCount = 0;
                if (totalCount == null) return Transaction.success(currentData);

                int newBookedCount = bookedCount + requestedPassengers;
                if (newBookedCount > totalCount) {
                    return Transaction.abort();
                }

                currentData.child("bookedPassengerCount").setValue(newBookedCount);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                if (committed) {
                    DatabaseReference reqRef = FirebaseDatabase.getInstance().getReference("rideRequests")
                            .child(currentDriverUid).child(rideId).child(userId);
                    reqRef.child("status").setValue("Accepted").addOnSuccessListener(aVoid -> {
                        sendSms(userPhone, "Your booking request has been accepted by the driver.");
                        Toast.makeText(getContext(), "Request accepted and SMS sent.", Toast.LENGTH_SHORT).show();
                        loadRequestsGroupedByDate();
                    });
                } else {
                    Toast.makeText(getContext(), "Cannot accept request: Over capacity or error.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void declineRequest(String rideId, String userId, String userPhone) {
        DatabaseReference reqRef = FirebaseDatabase.getInstance().getReference("rideRequests")
                .child(currentDriverUid).child(rideId).child(userId);

        reqRef.child("status").setValue("Declined").addOnSuccessListener(aVoid -> {
            sendSms(userPhone, "Your booking request has been declined by the driver.");
            Toast.makeText(getContext(), "Request declined and SMS sent.", Toast.LENGTH_SHORT).show();
            loadRequestsGroupedByDate();
        });
    }

    private void sendSms(String phoneNumber, String message) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(getContext(), "User phone number not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
        } else {
            Toast.makeText(getContext(), "SMS permission not granted.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "SMS permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Helper class to hold request info
    private static class RequestData {
        String rideId, userId, userName, status, userPhone;
        int requestedPassengers;

        RequestData(String rideId, String userId, String userName, String status, int requestedPassengers, String userPhone) {
            this.rideId = rideId;
            this.userId = userId;
            this.userName = userName;
            this.status = status;
            this.requestedPassengers = requestedPassengers;
            this.userPhone = userPhone;
        }
    }
}

