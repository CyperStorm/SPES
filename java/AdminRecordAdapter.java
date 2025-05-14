package com.example.colorsystem;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminRecordAdapter extends RecyclerView.Adapter<AdminRecordAdapter.RecordViewHolder> {
    private final List<Map<String, Object>> records;
    private final OnDeleteClickListener deleteClickListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(String userId, String recordId);
    }

    public AdminRecordAdapter(List<Map<String, Object>> records, OnDeleteClickListener deleteClickListener) {
        this.records = records;
        this.deleteClickListener = deleteClickListener;
        // Initialize isExpanded for each record
        for (Map<String, Object> record : records) {
            if (!record.containsKey("isExpanded")) {
                record.put("isExpanded", false);
            }
        }
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_record, parent, false);
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        Map<String, Object> record = records.get(position);
        String userId = (String) record.get("userId");
        String recordId = (String) record.get("recordId");

        // Build the record string for copy/share
        StringBuilder recordText = new StringBuilder();

        // Set summary (timestamp)
        if (record.containsKey("timestamp")) {
            Object timestampObj = record.get("timestamp");
            if (timestampObj instanceof Timestamp) {
                Timestamp firebaseTimestamp = (Timestamp) timestampObj;
                Date date = firebaseTimestamp.toDate();
                SimpleDateFormat formatter = new SimpleDateFormat("MMMM d, yyyy 'at' h:mm:ss a", Locale.ENGLISH);
                String formattedDate = formatter.format(date);
                holder.timestampTextView.setText("Time: " + formattedDate);
                recordText.append("Time: ").append(formattedDate).append("\n");
            }
        }

        // Set details
        if (record.containsKey("rgb")) {
            holder.colorTextView.setText("RGB: " + record.get("rgb"));
            recordText.append("RGB: ").append(record.get("rgb")).append("\n");
        }
        if (record.containsKey("description")) {
            holder.descriptionTextView.setText("Description: " + record.get("description"));
            recordText.append("Description: ").append(record.get("description")).append("\n");
        }
        if (record.containsKey("hex")) {
            holder.hexTextView.setText("Hex: " + record.get("hex"));
            recordText.append("Hex: ").append(record.get("hex")).append("\n");
        }
        if (record.containsKey("munsell")) {
            holder.munsellTextView.setText("Munsell Notation: " + record.get("munsell"));
            recordText.append("Munsell Notation: ").append(record.get("munsell")).append("\n");
        }
        if (record.containsKey("position")) {
            Map<String, Object> positionMap = (Map<String, Object>) record.get("position");
            holder.positionTextView.setText("Position: x=" + positionMap.get("x") + ", y=" + positionMap.get("y"));
            recordText.append("Position: x=").append(positionMap.get("x")).append(", y=").append(positionMap.get("y")).append("\n");
        }
        if (record.containsKey("imageUrl")) {
            holder.urlTextView.setText("Image URL: " + record.get("imageUrl"));
            recordText.append("Image URL: ").append(record.get("imageUrl"));
        }

        // Handle expand/collapse
        boolean isExpanded = record.get("isExpanded") != null && (Boolean) record.get("isExpanded");
        holder.detailsLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.expandArrow.setRotation(isExpanded ? 180 : 0); // Rotate arrow: down (expanded), right (collapsed)

        // Set click listener for expand/collapse
        holder.summaryLayout.setOnClickListener(v -> {
            Log.d("AdminRecordAdapter", "Summary clicked for position: " + position + ", isExpanded: " + isExpanded);
            boolean newExpandedState = !isExpanded;
            record.put("isExpanded", newExpandedState);
            notifyItemChanged(position); // Notify the adapter to rebind this item
        });

        // Copy Button Click Listener
        holder.copyButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) holder.itemView.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Record", recordText.toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(holder.itemView.getContext(), "Record copied to clipboard", Toast.LENGTH_SHORT).show();
        });

        // Share Button Click Listener
        holder.shareButton.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, recordText.toString());
            holder.itemView.getContext().startActivity(Intent.createChooser(shareIntent, "Share Record"));
        });

        // Delete Button Click Listener
        holder.deleteButton.setOnClickListener(v -> {
            if (deleteClickListener != null) {
                deleteClickListener.onDeleteClick(userId, recordId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    public static class RecordViewHolder extends RecyclerView.ViewHolder {
        TextView timestampTextView, colorTextView, descriptionTextView, hexTextView, munsellTextView, positionTextView, urlTextView;
        ImageView copyButton, shareButton, expandArrow;
        Button deleteButton;
        LinearLayout summaryLayout, detailsLayout;

        public RecordViewHolder(@NonNull View itemView) {
            super(itemView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            colorTextView = itemView.findViewById(R.id.colorInfoTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            hexTextView = itemView.findViewById(R.id.hexTextView);
            munsellTextView = itemView.findViewById(R.id.munsellTextView);
            positionTextView = itemView.findViewById(R.id.positionTextView);
            urlTextView = itemView.findViewById(R.id.urlTextView);
            copyButton = itemView.findViewById(R.id.copyButton);
            shareButton = itemView.findViewById(R.id.shareButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            summaryLayout = itemView.findViewById(R.id.summaryLayout);
            detailsLayout = itemView.findViewById(R.id.detailsLayout);
            expandArrow = itemView.findViewById(R.id.expandArrow);
        }
    }
}