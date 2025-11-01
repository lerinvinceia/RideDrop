package com.example.ridedrop;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RideAdapter extends RecyclerView.Adapter<RideAdapter.RideViewHolder> {

    private Context context;
    private List<DriverRide> rideList;
    private boolean showBookButton;

    public RideAdapter(Context context, List<DriverRide> rideList, boolean showBookButton) {
        this.context = context;
        this.rideList = rideList;
        this.showBookButton = showBookButton;
    }

    @Override
    public RideViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_ride, parent, false);
        return new RideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RideViewHolder holder, int position) {
        DriverRide ride = rideList.get(position);

        holder.nameTextView.setText(ride.getName());
        holder.phoneTextView.setText(ride.getPhone());
        holder.startPointTextView.setText(ride.getStartPoint());
        holder.destinationTextView.setText(ride.getDestination());
        holder.dateTextView.setText(ride.getDate());
        holder.priceTextView.setText(ride.getPrice());

        String passengerCount = ride.getPassengerCount();
        if (passengerCount == null || passengerCount.isEmpty()) {
            passengerCount = "0";
        }
        holder.passengerCountTextView.setText("Passengers: " + passengerCount);

        if (showBookButton) {
            holder.bookButton.setVisibility(View.VISIBLE);
            holder.bookButton.setOnClickListener(v -> {
                Intent intent = new Intent(context, LoginActivity.class);
                intent.putExtra("rideId", ride.getRideId());
                intent.putExtra("driverUid", ride.getDriverUid());
                context.startActivity(intent);
                Log.d("RideAdapter", "rideId = " + ride.getRideId() + ", driverUid = " + ride.getDriverUid());
            });
        } else {
            holder.bookButton.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return rideList.size();
    }

    public class RideViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, phoneTextView, startPointTextView, destinationTextView, dateTextView, priceTextView;
        TextView passengerCountTextView;
        Button bookButton;

        public RideViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            phoneTextView = itemView.findViewById(R.id.phoneTextView);
            startPointTextView = itemView.findViewById(R.id.startPointTextView);
            destinationTextView = itemView.findViewById(R.id.destinationTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            priceTextView = itemView.findViewById(R.id.priceTextView);
            passengerCountTextView = itemView.findViewById(R.id.passengerCountTextView);
            bookButton = itemView.findViewById(R.id.bookButton);
        }
    }
}