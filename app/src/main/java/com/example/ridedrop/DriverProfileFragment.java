package com.example.ridedrop;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;

public class DriverProfileFragment extends Fragment {

    private EditText nameEditText, phoneEditText, startPointEditText, destinationEditText,
            startTimeEditText, dateEditText, vehicleNoEditText, carBrandEditText,
            priceEditText, routeEditText, durationEditText, passengerCountEditText;

    private String driverId;
    private DatabaseReference driverRideRef;
    private String latestRideKey = null;

    private double price = 0;
    private int passengerCount = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_driver_profile, container, false);

        // Initialize UI
        nameEditText = view.findViewById(R.id.nameEditText);
        phoneEditText = view.findViewById(R.id.phoneEditText);
        startPointEditText = view.findViewById(R.id.startPointEditText);
        destinationEditText = view.findViewById(R.id.destinationEditText);
        startTimeEditText = view.findViewById(R.id.startTimeEditText);
        dateEditText = view.findViewById(R.id.dateEditText);
        vehicleNoEditText = view.findViewById(R.id.vehicleNoEditText);
        carBrandEditText = view.findViewById(R.id.carBrandEditText);
        priceEditText = view.findViewById(R.id.editTextPrice);
        routeEditText = view.findViewById(R.id.routeEditText);
        durationEditText = view.findViewById(R.id.durationEditText);
        passengerCountEditText = view.findViewById(R.id.editTextPassengerCount);

        phoneEditText.setEnabled(false);  // Make phone field non-editable

        driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        driverRideRef = FirebaseDatabase.getInstance().getReference("rideDetails").child(driverId);

        loadLatestRideDetails();  // Load latest ride when fragment opens

        // Set up pickers
        startTimeEditText.setOnClickListener(v -> showTimePicker(startTimeEditText));
        durationEditText.setOnClickListener(v -> showTimePicker(durationEditText));
        dateEditText.setOnClickListener(v -> showDatePicker(dateEditText));

        // Button actions
        view.findViewById(R.id.buttonEdit).setOnClickListener(v -> updateRideDetails());
        view.findViewById(R.id.buttonNewRide).setOnClickListener(v -> createNewRide());

        // Stepper controls
        view.findViewById(R.id.buttonIncreasePrice).setOnClickListener(v -> {
            price += 10;
            priceEditText.setText(String.valueOf(price));
        });

        view.findViewById(R.id.buttonDecreasePrice).setOnClickListener(v -> {
            if (price > 0) price -= 10;
            priceEditText.setText(String.valueOf(price));
        });

        view.findViewById(R.id.buttonIncreasePassenger).setOnClickListener(v -> {
            passengerCount++;
            passengerCountEditText.setText(String.valueOf(passengerCount));
        });

        view.findViewById(R.id.buttonDecreasePassenger).setOnClickListener(v -> {
            if (passengerCount > 1) passengerCount--;
            passengerCountEditText.setText(String.valueOf(passengerCount));
        });

        return view;
    }

    private void loadLatestRideDetails() {
        Query latestRideQuery = driverRideRef.orderByChild("timestamp").limitToLast(1);
        latestRideQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot rideSnapshot : snapshot.getChildren()) {
                        DriverRide driverRide = rideSnapshot.getValue(DriverRide.class);
                        if (driverRide != null) {
                            nameEditText.setText(driverRide.getName());
                            phoneEditText.setText(driverRide.getPhone());
                            startPointEditText.setText(driverRide.getStartPoint());
                            destinationEditText.setText(driverRide.getDestination());
                            startTimeEditText.setText(driverRide.getStartTime());
                            dateEditText.setText(driverRide.getDate());
                            vehicleNoEditText.setText(driverRide.getVehicleNo());
                            carBrandEditText.setText(driverRide.getCarBrand());
                            priceEditText.setText(driverRide.getPrice());
                            routeEditText.setText(driverRide.getRoute());
                            durationEditText.setText(driverRide.getDuration());
                            passengerCountEditText.setText(driverRide.getPassengerCount());

                            latestRideKey = rideSnapshot.getKey();

                            try {
                                price = Double.parseDouble(driverRide.getPrice());
                                passengerCount = Integer.parseInt(driverRide.getPassengerCount());
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRideDetails() {
        if (latestRideKey == null) {
            Toast.makeText(getContext(), "No ride selected to update.", Toast.LENGTH_SHORT).show();
            return;
        }

        DriverRide updatedRide = collectRideFromForm(false);
        if (updatedRide == null) return;

        driverRideRef.child(latestRideKey).setValue(updatedRide)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Ride updated successfully.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Update failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createNewRide() {
        DriverRide newRide = collectRideFromForm(true);
        if (newRide == null) return;

        driverRideRef.push().setValue(newRide)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "New ride added.", Toast.LENGTH_SHORT).show();
                        loadLatestRideDetails();
                    } else {
                        Toast.makeText(getContext(), "Failed to add new ride.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private DriverRide collectRideFromForm(boolean includeTimestamp) {
        String name = nameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String startPoint = startPointEditText.getText().toString().trim();
        String destination = destinationEditText.getText().toString().trim();
        String startTime = startTimeEditText.getText().toString().trim();
        String date = dateEditText.getText().toString().trim();
        String vehicleNo = vehicleNoEditText.getText().toString().trim();
        String carBrand = carBrandEditText.getText().toString().trim();
        String priceStr = priceEditText.getText().toString().trim();
        String route = routeEditText.getText().toString().trim();
        String duration = durationEditText.getText().toString().trim();
        String passengerCountStr = passengerCountEditText.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
            Toast.makeText(getContext(), "Name and phone are required", Toast.LENGTH_SHORT).show();
            return null;
        }

        if (!isValidDate(date)) {
            Toast.makeText(getContext(), "Invalid date format (yyyy-MM-dd)", Toast.LENGTH_SHORT).show();
            return null;
        }

        String timestamp = includeTimestamp ? String.valueOf(System.currentTimeMillis()) : null;
        String rideId = includeTimestamp ? driverRideRef.push().getKey() : latestRideKey;
        String driverUid = driverId;

        return new DriverRide(
                name, phone, startPoint, destination, startTime, date,
                vehicleNo, carBrand, priceStr, route, duration, passengerCountStr,
                timestamp, rideId, driverUid
        );
    }

    private void showDatePicker(EditText editText) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            month++;
            String date = year + "-" + String.format("%02d", month) + "-" + String.format("%02d", dayOfMonth);
            editText.setText(date);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker(EditText editText) {
        Calendar calendar = Calendar.getInstance();
        new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
            String time = String.format("%02d:%02d", hourOfDay, minute);
            editText.setText(time);
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    private boolean isValidDate(String date) {
        try {
            String[] parts = date.split("-");
            if (parts.length == 3) {
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int day = Integer.parseInt(parts[2]);
                return (month >= 1 && month <= 12) && (day >= 1 && day <= 31);
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
}
