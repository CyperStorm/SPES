package com.example.colorsystem;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserRecordsActivity extends AppCompatActivity {

    private TextView userIdTextView;
    private RecyclerView recordsRecyclerView;
    private Button backButton;
    private FirebaseFirestore db;
    private List<Map<String, Object>> recordList;
    private AdminRecordAdapter recordAdapter;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_records);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Get the user ID from the intent
        userId = getIntent().getStringExtra("USER_ID");
        if (userId == null) {
            Toast.makeText(this, "Error: User ID not provided.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialize views
        userIdTextView = findViewById(R.id.userIdTextView);
        recordsRecyclerView = findViewById(R.id.recordsRecyclerView);
        backButton = findViewById(R.id.backButton);

        // Set the user ID in the UI
        userIdTextView.setText("Records for User: " + userId);

        // Initialize record list
        recordList = new ArrayList<>();
        recordAdapter = new AdminRecordAdapter(recordList, this::deleteRecord);
        recordsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recordsRecyclerView.setAdapter(recordAdapter);

        // Fetch records for the user
        fetchRecords();

        // Back button click listener
        backButton.setOnClickListener(v -> finish());
    }

    private void fetchRecords() {
        // Preserve the isExpanded state of existing records
        Map<String, Boolean> expandedStates = new HashMap<>();
        for (Map<String, Object> record : recordList) {
            String recordId = (String) record.get("recordId");
            Boolean isExpanded = (Boolean) record.get("isExpanded");
            if (recordId != null && isExpanded != null) {
                expandedStates.put(recordId, isExpanded);
            }
        }

        db.collection("users").document(userId).collection("records")
                .orderBy("timestamp", Query.Direction.DESCENDING) // Sort by timestamp in descending order
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        recordList.clear();
                        int recordCount = 0;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, Object> recordData = document.getData();
                            String recordId = document.getId();
                            recordData.put("recordId", recordId);
                            recordData.put("userId", userId);
                            // Restore the isExpanded state if it exists, otherwise default to false
                            recordData.put("isExpanded", expandedStates.getOrDefault(recordId, false));
                            recordList.add(recordData);
                            recordCount++;
                        }
                        Log.d("UserRecordsActivity", "Fetched " + recordList.size() + " records for user " + userId);
                        Log.d("UserRecordsActivity", "Total records processed: " + recordCount);
                        recordAdapter.notifyDataSetChanged();
                        if (recordList.isEmpty()) {
                            Toast.makeText(this, "No records found for this user.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Displaying " + recordList.size() + " records.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e("UserRecordsActivity", "Error fetching records: " + task.getException().getMessage(), task.getException());
                        Toast.makeText(this, "Error fetching records: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void deleteRecord(String userId, String recordId) {
        db.collection("users").document(userId).collection("records").document(recordId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Record deleted.", Toast.LENGTH_SHORT).show();
                    fetchRecords(); // Refresh record list
                })
                .addOnFailureListener(e -> {
                    Log.e("UserRecordsActivity", "Error deleting record: " + e.getMessage(), e);
                    Toast.makeText(this, "Error deleting record: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}