package com.example.ridedrop;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class HistoryAdapter extends BaseAdapter {

    public static class HistoryItem {
        public String status;
        public String userName;
        public String userPhone;
        public long timestamp;
        public String rideId;
        public String driverId;

        public HistoryItem(String status, String userName, String userPhone, long timestamp, String rideId, String driverId) {
            this.status = status;
            this.userName = userName;
            this.userPhone = userPhone;
            this.timestamp = timestamp;
            this.rideId = rideId;
            this.driverId = driverId;
        }
    }

    private Context context;
    private ArrayList<HistoryItem> historyItems;
    private LayoutInflater inflater;

    public HistoryAdapter(Context context, ArrayList<HistoryItem> historyItems) {
        this.context = context;
        this.historyItems = historyItems;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return historyItems.size();
    }

    @Override
    public Object getItem(int position) {
        return historyItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.history_item_layout, parent, false);
            holder = new ViewHolder();
            holder.historyItemText = convertView.findViewById(R.id.historyItemText);
            holder.timeBadge = convertView.findViewById(R.id.timeBadge);
            holder.statusIndicator = convertView.findViewById(R.id.statusIndicator);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        HistoryItem item = historyItems.get(position);

        // Format the main text
        String mainText = String.format(
                "Status: %s\nRequested by: %s (%s)",
                getStatusText(item.status),
                item.userName != null ? item.userName : "Unknown",
                item.userPhone != null ? item.userPhone : "Unknown"
        );

        holder.historyItemText.setText(mainText);

        // Format time badge
        String timeAgo = getTimeAgo(item.timestamp);
        holder.timeBadge.setText(timeAgo);

        // Set status indicator color
        int statusColor = getStatusColor(item.status);
        holder.statusIndicator.setBackgroundColor(statusColor);

        return convertView;
    }

    private String getStatusText(String status) {
        if (status == null) return "Unknown";

        switch (status.toLowerCase()) {
            case "pending":
                return "⏳ Pending";
            case "approved":
            case "accepted":
                return "✅ Approved";
            case "rejected":
            case "declined":
                return "❌ Rejected";
            case "completed":
                return "🎉 Completed";
            case "cancelled":
                return "🚫 Cancelled";
            default:
                return "📋 " + status;
        }
    }

    private int getStatusColor(String status) {
        if (status == null) return Color.parseColor("#6B7280");

        switch (status.toLowerCase()) {
            case "pending":
                return Color.parseColor("#F59E0B"); // Orange
            case "approved":
            case "accepted":
            case "completed":
                return Color.parseColor("#10B981"); // Green
            case "rejected":
            case "declined":
            case "cancelled":
                return Color.parseColor("#EF4444"); // Red
            default:
                return Color.parseColor("#6B7280"); // Gray
        }
    }

    private String getTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 60 * 1000) {
            return "Just now";
        } else if (diff < 60 * 60 * 1000) {
            long minutes = diff / (60 * 1000);
            return minutes + "m ago";
        } else if (diff < 24 * 60 * 60 * 1000) {
            long hours = diff / (60 * 60 * 1000);
            return hours + "h ago";
        } else if (diff < 7 * 24 * 60 * 60 * 1000) {
            long days = diff / (24 * 60 * 60 * 1000);
            return days + "d ago";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }

    static class ViewHolder {
        TextView historyItemText;
        TextView timeBadge;
        View statusIndicator;
    }
}