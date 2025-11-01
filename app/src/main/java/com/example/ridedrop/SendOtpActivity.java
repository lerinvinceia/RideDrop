package com.example.ridedrop;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Random;

public class SendOtpActivity extends AppCompatActivity {

    EditText editPhone;
    Button btnSendOtp;
    String generatedOtp;
    String phoneNumber;
    String role;

    // New variables to hold rideId and driverUid
    String rideId, driverUid;

    private static final int SMS_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_otp);

        editPhone = findViewById(R.id.editPhone);
        btnSendOtp = findViewById(R.id.btnSendOtp);

        // Get the role, rideId, driverUid passed from the previous activity
        role = getIntent().getStringExtra("role");
        rideId = getIntent().getStringExtra("rideId");
        driverUid = getIntent().getStringExtra("driverUid");

        btnSendOtp.setOnClickListener(v -> {
            phoneNumber = editPhone.getText().toString().trim();

            if (phoneNumber.isEmpty() || phoneNumber.length() != 10) {
                Toast.makeText(SendOtpActivity.this, "Enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show();
            } else {
                checkAndSendOtp();
            }
        });
    }

    private void checkAndSendOtp() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        } else {
            sendOtp();
        }
    }

    private void sendOtp() {
        generatedOtp = String.format("%06d", new Random().nextInt(999999));
        String message = "Your OTP is: " + generatedOtp;

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);

            Toast.makeText(this, "OTP Sent Successfully", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(SendOtpActivity.this, VerifyOtpActivity.class);
            intent.putExtra("otp", generatedOtp);
            intent.putExtra("phone", phoneNumber);
            intent.putExtra("role", role);

            // Pass rideId and driverUid forward as well
            if (rideId != null) intent.putExtra("rideId", rideId);
            if (driverUid != null) intent.putExtra("driverUid", driverUid);

            startActivity(intent);
            finish();

        } catch (Exception e) {
            Toast.makeText(this, "Failed to send OTP: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendOtp();
            } else {
                Toast.makeText(this, "SMS Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
