package com.example.ridedrop;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DriverDetailsActivity extends AppCompatActivity {

    private EditText nameEditText, phoneEditText, startPointEditText, destinationEditText,
            startTimeEditText, dateEditText, vehicleNoEditText, carBrandEditText,
            priceEditText, routeEditText, durationEditText, passengerCountEditText;

    private Button saveButton, calculateDistanceButton;

    private FirebaseAuth mAuth;
    private DatabaseReference driverRef;

    private double price = 0;
    private int passengerCount = 1;
    private double calculatedDistance = 0;

    // OpenRouteService API key (free tier: 2000 requests/day)
    private static final String ORS_API_KEY = "5b3ce3597851110001cf62481af3f436312648f594a99241d355b4ef";
    private static final String ORS_BASE_URL = "https://api.openrouteservice.org/v2/directions/driving-car";

    // Price per km (you can adjust this)
    private static final double PRICE_PER_KM = 2.0;

    private OkHttpClient httpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_details);

        initializeHttpClient();
        initializeFirebase();
        initializeViews();
        prefillPhoneNumber();
        setupPickersAndSteppers();
        setupTextWatchers();
        setSaveButtonListener();
    }

    private void initializeHttpClient() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            driverRef = FirebaseDatabase.getInstance().getReference("rideDetails").child(uid);
        }
    }

    private void initializeViews() {
        nameEditText = findViewById(R.id.editTextName);
        phoneEditText = findViewById(R.id.editTextPhone);
        startPointEditText = findViewById(R.id.editTextStartPoint);
        destinationEditText = findViewById(R.id.editTextDestination);
        startTimeEditText = findViewById(R.id.editTextStartTime);
        dateEditText = findViewById(R.id.editTextDate);
        vehicleNoEditText = findViewById(R.id.editTextVehicleNo);
        carBrandEditText = findViewById(R.id.editTextCarBrand);
        priceEditText = findViewById(R.id.editTextPrice);
        routeEditText = findViewById(R.id.editTextRoute);
        durationEditText = findViewById(R.id.editTextDuration);
        passengerCountEditText = findViewById(R.id.editTextPassengerCount);
        saveButton = findViewById(R.id.buttonSave);
        calculateDistanceButton = findViewById(R.id.buttonCalculateDistance);
    }

    private void prefillPhoneNumber() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("phone")) {
            String phone = intent.getStringExtra("phone");
            if (phone != null) {
                phoneEditText.setText(phone);
                phoneEditText.setEnabled(false);
            }
        }
    }

    private void setupTextWatchers() {
        TextWatcher routeCalculationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Auto-calculate when all required fields are filled
                if (shouldAutoCalculate()) {
                    calculateDistanceAndPrice();
                }
            }
        };

        startPointEditText.addTextChangedListener(routeCalculationWatcher);
        destinationEditText.addTextChangedListener(routeCalculationWatcher);
        routeEditText.addTextChangedListener(routeCalculationWatcher);
    }

    private boolean shouldAutoCalculate() {
        return !TextUtils.isEmpty(startPointEditText.getText().toString().trim()) &&
                !TextUtils.isEmpty(destinationEditText.getText().toString().trim()) &&
                !TextUtils.isEmpty(routeEditText.getText().toString().trim());
    }

    private void setupPickersAndSteppers() {
        startTimeEditText.setOnClickListener(v -> showTimePicker(startTimeEditText));
        durationEditText.setOnClickListener(v -> showTimePicker(durationEditText));
        dateEditText.setOnClickListener(v -> showDatePicker(dateEditText));

        calculateDistanceButton.setOnClickListener(v -> calculateDistanceAndPrice());

        findViewById(R.id.buttonIncreasePrice).setOnClickListener(v -> {
            price += 10;
            priceEditText.setText(String.valueOf((int)price));
        });
        findViewById(R.id.buttonDecreasePrice).setOnClickListener(v -> {
            if (price > 0) price -= 10;
            priceEditText.setText(String.valueOf((int)price));
        });

        findViewById(R.id.buttonIncreasePassenger).setOnClickListener(v -> {
            passengerCount++;
            passengerCountEditText.setText(String.valueOf(passengerCount));
        });
        findViewById(R.id.buttonDecreasePassenger).setOnClickListener(v -> {
            if (passengerCount > 1) passengerCount--;
            passengerCountEditText.setText(String.valueOf(passengerCount));
        });
    }

    private void calculateDistanceAndPrice() {
        String startPoint = startPointEditText.getText().toString().trim();
        String destination = destinationEditText.getText().toString().trim();
        String route = routeEditText.getText().toString().trim().toLowerCase();

        if (TextUtils.isEmpty(startPoint) || TextUtils.isEmpty(destination) || TextUtils.isEmpty(route)) {
            Toast.makeText(this, "Please enter start point, destination, and route", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        calculateDistanceButton.setEnabled(false);
        calculateDistanceButton.setText("Calculating...");

        // First, get coordinates for start and destination
        getCoordinates(startPoint, new CoordinateCallback() {
            @Override
            public void onSuccess(double[] startCoords) {
                getCoordinates(destination, new CoordinateCallback() {
                    @Override
                    public void onSuccess(double[] destCoords) {
                        calculateRoute(startCoords, destCoords, route);
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(DriverDetailsActivity.this, "Error finding destination: " + error, Toast.LENGTH_SHORT).show();
                            resetCalculateButton();
                        });
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(DriverDetailsActivity.this, "Error finding start location: " + error, Toast.LENGTH_SHORT).show();
                    resetCalculateButton();
                });
            }
        });
    }

    private void getCoordinates(String location, CoordinateCallback callback) {
        // Using OpenRouteService Geocoding API
        String geocodeUrl = "https://api.openrouteservice.org/geocode/search?api_key=" + ORS_API_KEY +
                "&text=" + location.replace(" ", "%20") + "&boundary.country=IN";

        Request request = new Request.Builder()
                .url(geocodeUrl)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    JSONObject json = new JSONObject(responseBody);
                    JSONArray features = json.getJSONArray("features");

                    if (features.length() > 0) {
                        JSONObject geometry = features.getJSONObject(0).getJSONObject("geometry");
                        JSONArray coordinates = geometry.getJSONArray("coordinates");
                        double[] coords = {coordinates.getDouble(0), coordinates.getDouble(1)}; // [lon, lat]
                        callback.onSuccess(coords);
                    } else {
                        callback.onError("Location not found");
                    }
                } catch (JSONException e) {
                    callback.onError("Error parsing location data");
                }
            }
        });
    }

    private void calculateRoute(double[] startCoords, double[] destCoords, String routeType) {
        // Build route URL with waypoints based on route type
        StringBuilder urlBuilder = new StringBuilder(ORS_BASE_URL);
        urlBuilder.append("?api_key=").append(ORS_API_KEY);
        urlBuilder.append("&start=").append(startCoords[0]).append(",").append(startCoords[1]);
        urlBuilder.append("&end=").append(destCoords[0]).append(",").append(destCoords[1]);

        // Add route-specific parameters
        if (routeType.contains("ecr")) {
            // For ECR route, try to avoid highways and prefer coastal roads
            urlBuilder.append("&radiuses=10000,10000"); // Allow some flexibility
            urlBuilder.append("&options={\"avoid_features\":[\"highways\"],\"profile\":\"driving-car\"}");
        } else if (routeType.contains("bypass")) {
            // For bypass, prefer highways and faster routes
            urlBuilder.append("&radiuses=10000,10000");
            urlBuilder.append("&options={\"profile\":\"driving-car\",\"preference\":\"fastest\"}");
        }

        Request request = new Request.Builder()
                .url(urlBuilder.toString())
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(DriverDetailsActivity.this, "Error calculating route: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetCalculateButton();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    JSONObject json = new JSONObject(responseBody);

                    if (json.has("features")) {
                        JSONArray features = json.getJSONArray("features");
                        if (features.length() > 0) {
                            JSONObject feature = features.getJSONObject(0);
                            JSONObject properties = feature.getJSONObject("properties");
                            JSONArray segments = properties.getJSONArray("segments");

                            double distance = segments.getJSONObject(0).getDouble("distance") / 1000.0; // Convert to km
                            double duration = segments.getJSONObject(0).getDouble("duration") / 60.0; // Convert to minutes

                            runOnUiThread(() -> updateDistanceAndPrice(distance, duration));
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(DriverDetailsActivity.this, "No route found", Toast.LENGTH_SHORT).show();
                                resetCalculateButton();
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(DriverDetailsActivity.this, "No route data available", Toast.LENGTH_SHORT).show();
                            resetCalculateButton();
                        });
                    }
                } catch (JSONException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(DriverDetailsActivity.this, "Error parsing route data", Toast.LENGTH_SHORT).show();
                        resetCalculateButton();
                    });
                }
            }
        });
    }

    private void updateDistanceAndPrice(double distance, double duration) {
        calculatedDistance = distance;

        // Calculate price based on distance
        price = Math.round(distance * PRICE_PER_KM);
        priceEditText.setText(String.valueOf((int)price));

        // Update duration (convert minutes to HH:MM format)
        int hours = (int) (duration / 60);
        int minutes = (int) (duration % 60);
        String durationStr = String.format("%02d:%02d", hours, minutes);
        durationEditText.setText(durationStr);

        Toast.makeText(this, String.format("Distance: %.1f km, Price: ₹%.0f", distance, price), Toast.LENGTH_LONG).show();
        resetCalculateButton();
    }

    private void resetCalculateButton() {
        calculateDistanceButton.setEnabled(true);
        calculateDistanceButton.setText("Calculate Distance");
    }

    private void setSaveButtonListener() {
        saveButton.setOnClickListener(v -> saveDriverDetails());
    }

    // Rest of your existing methods remain unchanged
    private void saveDriverDetails() {
        String name = nameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String startPoint = startPointEditText.getText().toString().trim();
        String destination = destinationEditText.getText().toString().trim();
        String startTime = startTimeEditText.getText().toString().trim();
        String date = dateEditText.getText().toString().trim();
        String vehicleNo = vehicleNoEditText.getText().toString().trim();
        String route = routeEditText.getText().toString().trim();
        String duration = durationEditText.getText().toString().trim();
        String priceStr = priceEditText.getText().toString().trim();
        String passengerCountStr = passengerCountEditText.getText().toString().trim();
        String carBrand = carBrandEditText.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(startPoint) || TextUtils.isEmpty(destination)
                || TextUtils.isEmpty(startTime) || TextUtils.isEmpty(date) || TextUtils.isEmpty(vehicleNo)
                || TextUtils.isEmpty(carBrand) || TextUtils.isEmpty(priceStr)
                || TextUtils.isEmpty(route) || TextUtils.isEmpty(duration) || TextUtils.isEmpty(passengerCountStr)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidDate(date)) {
            Toast.makeText(this, "Invalid date format. Use yyyy-MM-dd", Toast.LENGTH_SHORT).show();
            return;
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        String rideId = driverRef.push().getKey();
        String driverUid = mAuth.getCurrentUser().getUid();

        DriverRide driverRide = new DriverRide(
                name,
                phone,
                startPoint,
                destination,
                startTime,
                date,
                vehicleNo,
                carBrand,
                priceStr,
                route,
                duration,
                passengerCountStr,
                timestamp,
                rideId,
                driverUid
        );

        driverRef.child(rideId).setValue(driverRide)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Details Saved", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, DriverDashboardActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showDatePicker(EditText editText) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            month++;
            String date = year + "-" + String.format("%02d", month) + "-" + String.format("%02d", dayOfMonth);
            editText.setText(date);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker(EditText editText) {
        Calendar calendar = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
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

    // Callback interface for coordinate retrieval
    private interface CoordinateCallback {
        void onSuccess(double[] coordinates);
        void onError(String error);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
        }
    }
}


