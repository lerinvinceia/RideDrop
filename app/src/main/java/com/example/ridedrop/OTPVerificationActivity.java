package com.example.ridedrop;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class OTPVerificationActivity extends AppCompatActivity {

    private EditText nameEditText, phoneEditText, otpEditText;
    private Button sendOtpButton, verifyOtpButton;

    private String phoneNumber;
    private String verificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otpverification_activity);

        nameEditText = findViewById(R.id.editTextName);
        phoneEditText = findViewById(R.id.editTextPhone);
        otpEditText = findViewById(R.id.editTextOtp);

        sendOtpButton = findViewById(R.id.buttonSendOtp);
        verifyOtpButton = findViewById(R.id.buttonVerifyOtp);

        sendOtpButton.setOnClickListener(v -> sendOtp());
        verifyOtpButton.setOnClickListener(v -> verifyOtp());
    }

    private void sendOtp() {
        phoneNumber = phoneEditText.getText().toString().trim();

        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                "+91" + phoneNumber,  // Adjust the country code as needed
                60,
                TimeUnit.SECONDS,
                this,
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                        // Optional: autofill the OTP if available
                        String code = credential.getSmsCode();

                        Log.d("OTP", "Auto‐retrieved OTP: " + code);
                        if (code != null) {
                            otpEditText.setText(code);
                        }
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        Toast.makeText(OTPVerificationActivity.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(String vId, PhoneAuthProvider.ForceResendingToken token) {
                        verificationId = vId;
                        Toast.makeText(OTPVerificationActivity.this, "OTP sent successfully", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void verifyOtp() {
        String otp = otpEditText.getText().toString().trim();

        if (otp.isEmpty()) {
            Toast.makeText(this, "Please enter the OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        if (verificationId == null) {
            Toast.makeText(this, "Please send OTP first", Toast.LENGTH_SHORT).show();
            return;
        }

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);

        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "OTP Verified Successfully", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(OTPVerificationActivity.this, DriverDetailsActivity.class));
                finish();
            } else {
                Toast.makeText(this, "OTP Verification Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
