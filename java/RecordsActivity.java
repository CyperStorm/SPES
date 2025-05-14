package com.example.colorsystem;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecordsActivity extends AppCompatActivity {

    private RecyclerView recordsRecyclerView;
    private RecordAdapter recordAdapter;
    private List<Map<String, Object>> recordsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_records);

        // Initialize RecyclerView
        recordsRecyclerView = findViewById(R.id.recordsRecyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recordsRecyclerView.setLayoutManager(layoutManager);

        // Add DividerItemDecoration
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recordsRecyclerView.getContext(),
                layoutManager.getOrientation());
        dividerItemDecoration.setDrawable(ContextCompat.getDrawable(this, R.drawable.divider));
        recordsRecyclerView.addItemDecoration(dividerItemDecoration);

        // Initialize Adapter and set it to the RecyclerView
        recordAdapter = new RecordAdapter(recordsList);
        recordsRecyclerView.setAdapter(recordAdapter);

        loadRecordsFromFirestore();
    }

    private void loadRecordsFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();

            db.collection("users").document(userId)
                    .collection("records")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            recordsList.clear();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Map<String, Object> recordData = document.getData();
                                recordData.put("isExpanded", false); // Default to collapsed
                                recordsList.add(recordData);
                            }
                            recordAdapter.notifyDataSetChanged();
                        } else {
                            Log.e("Firestore", "Error fetching records", task.getException());
                        }
                    });
        } else {
            Log.e("Firestore", "User not logged in.");
        }
    }
}