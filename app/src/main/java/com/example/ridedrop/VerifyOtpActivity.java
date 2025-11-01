package com.example.ridedrop;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class VerifyOtpActivity extends AppCompatActivity {

    EditText editOtp;
    Button btnVerifyOtp;
    String receivedOtp;
    String phoneNumber;
    String role;

    String rideId, driverUid;

    BroadcastReceiver otpReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        editOtp = findViewById(R.id.editOtp);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);

        receivedOtp = getIntent().getStringExtra("otp");
        phoneNumber = getIntent().getStringExtra("phone");
        role = getIntent().getStringExtra("role");

        rideId = getIntent().getStringExtra("rideId");
        driverUid = getIntent().getStringExtra("driverUid");

        btnVerifyOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyOtp();
            }
        });

        otpReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String message = intent.getStringExtra("message");

                if (message != null) {
                    String otpFromSms = message.replaceAll("[^0-9]", "");
                    editOtp.setText(otpFromSms);
                    Toast.makeText(context, "OTP Auto-Filled!", Toast.LENGTH_SHORT).show();
                }
            }
        };

        registerReceiver(otpReceiver, new IntentFilter("otp_received"));
    }

    private void verifyOtp() {
        String enteredOtp = editOtp.getText().toString().trim();
        if (enteredOtp.equals(receivedOtp)) {
            Toast.makeText(this, "OTP Verified Successfully!", Toast.LENGTH_SHORT).show();

            Intent intent;
            if ("Driver".equals(role)) {
                intent = new Intent(VerifyOtpActivity.this, DriverDetailsActivity.class);
                intent.putExtra("phone", phoneNumber);
                // Do NOT pass rideId and driverUid to DriverDetailsActivity
            } else {
                intent = new Intent(VerifyOtpActivity.this, UserDetailsActivity.class);
                intent.putExtra("phone", phoneNumber);
                // Pass rideId and driverUid ONLY to UserDetailsActivity
                if (rideId != null) intent.putExtra("rideId", rideId);
                if (driverUid != null) intent.putExtra("driverUid", driverUid);
            }

            startActivity(intent);
            finish();

        } else {
            Toast.makeText(this, "Invalid OTP. Try Again.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (otpReceiver != null) {
            unregisterReceiver(otpReceiver);
        }
    }
}
