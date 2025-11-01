package com.example.ridedrop;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class DriverHistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private RideAdapter rideAdapter;
    private List<DriverRide> rideList;
    private DatabaseReference databaseReference;
    private String currentDriverId;

    public DriverHistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_driver_history, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewRides);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        rideList = new ArrayList<>();

        // ✅ Updated to match RideAdapter constructor
        rideAdapter = new RideAdapter(getContext(), rideList, false); // Book button hidden
        recyclerView.setAdapter(rideAdapter);

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        currentDriverId = firebaseAuth.getCurrentUser().getUid();

        databaseReference = FirebaseDatabase.getInstance().getReference("rideDetails").child(currentDriverId);

        loadDriverRideDetails();

        return view;
    }

    private void loadDriverRideDetails() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                rideList.clear();
                for (DataSnapshot rideSnapshot : dataSnapshot.getChildren()) {
                    DriverRide driverRide = rideSnapshot.getValue(DriverRide.class);
                    if (driverRide != null) {
                        rideList.add(driverRide);
                    }
                }
                rideAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getContext(), "Error loading ride details: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}