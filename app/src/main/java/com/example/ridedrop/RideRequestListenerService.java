package com.example.ridedrop;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.telephony.SmsManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.*;

public class RideRequestListenerService extends Service {

    private DatabaseReference requestRef;
    private ValueEventListener requestListener;

    private String rideId;
    private String driverUid;

    private static final String CHANNEL_ID = "RideRequestChannel";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        rideId = intent.getStringExtra("rideId");
        driverUid = intent.getStringExtra("driverUid");

        if (rideId == null || driverUid == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        showForegroundNotification("Listening for new ride requests...");

        requestRef = FirebaseDatabase.getInstance().getReference("rideRequests")
                .child(driverUid).child(rideId);

        requestListener = requestRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String status = userSnapshot.child("status").getValue(String.class);
                    String userName = userSnapshot.child("userName").getValue(String.class);

                    if ("Pending".equalsIgnoreCase(status)) {
                        sendRequestNotification(userName);
                        fetchDriverPhoneAndSendSMS(userName);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });

        return START_STICKY;
    }

    private void sendRequestNotification(String userName) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("New Ride Request")
                .setContentText(userName + " has sent a booking request")
                .setSmallIcon(R.drawable.ic_ride)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void fetchDriverPhoneAndSendSMS(String userName) {
        DatabaseReference rideRef = FirebaseDatabase.getInstance()
                .getReference("rideDetails").child(driverUid).child(rideId);

        rideRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String phone = snapshot.child("phone").getValue(String.class);
                if (phone != null && !phone.isEmpty()) {
                    String message = "You have a new booking request from " + userName;
                    sendSMS(phone, message);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
    }

    private void sendSMS(String phone, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phone, null, message, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showForegroundNotification(String contentText) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("RideDrop Service Active")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_ride)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Ride Request Listener",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        if (requestRef != null && requestListener != null) {
            requestRef.removeEventListener(requestListener);
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

