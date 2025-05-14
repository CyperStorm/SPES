package com.example.colorsystem;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminActivity extends AppCompatActivity {

    private RecyclerView userRecyclerView;
    private Button backToMainButton;
    private LinearLayout userListContainer;
    private TextView emptyUserListTextView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private List<Map<String, Object>> userList;
    private UserAdapter userAdapter;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_admin);
            Log.d("AdminActivity", "onCreate: Layout set successfully");

            // Initialize Firebase
            mAuth = FirebaseAuth.getInstance();
            if (mAuth.getCurrentUser() == null) {
                Log.e("AdminActivity", "No user is currently signed in.");
                Toast.makeText(this, "Error: No user signed in.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(AdminActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return;
            }

            db = FirebaseFirestore.getInstance();
            currentUserId = mAuth.getCurrentUser().getUid();
            Log.d("AdminActivity", "Current user ID: " + currentUserId);

            // Initialize views
            userRecyclerView = findViewById(R.id.userRecyclerView);
            backToMainButton = findViewById(R.id.backToMainButton);
            userListContainer = findViewById(R.id.user_list_container);
            emptyUserListTextView = findViewById(R.id.emptyUserListTextView);

            // Verify views are not null
            if (userRecyclerView == null || backToMainButton == null || userListContainer == null || emptyUserListTextView == null) {
                Log.e("AdminActivity", "One or more views are null: " +
                        "userRecyclerView=" + (userRecyclerView == null) +
                        ", backToMainButton=" + (backToMainButton == null) +
                        ", userListContainer=" + (userListContainer == null) +
                        ", emptyUserListTextView=" + (emptyUserListTextView == null));
                Toast.makeText(this, "Error: Failed to initialize UI components.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            // Initially hide the user list container
            userListContainer.setVisibility(View.GONE);
            Log.d("AdminActivity", "onCreate: user_list_container set to GONE");

            // Initialize user list
            userList = new ArrayList<>();
            userAdapter = new UserAdapter(userList, currentUserId, this::onUserClicked, this);
            userRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            userRecyclerView.setAdapter(userAdapter);
            Log.d("AdminActivity", "onCreate: RecyclerView adapter set");

            // Check admin status
            checkAdminStatus();

            // Back to Main button click listener
            backToMainButton.setOnClickListener(v -> {
                Log.d("AdminActivity", "Back to Main button clicked");
                Intent intent = new Intent(AdminActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            });
        } catch (Exception e) {
            Log.e("AdminActivity", "Crash in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void checkAdminStatus() {
        try {
            String userId = mAuth.getCurrentUser().getUid();
            Log.d("AdminActivity", "Checking admin status for user: " + userId);
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        try {
                            if (documentSnapshot.exists()) {
                                Boolean isAdmin = documentSnapshot.getBoolean("isAdmin");
                                Log.d("AdminActivity", "isAdmin: " + isAdmin);
                                if (isAdmin == null || !isAdmin) {
                                    Log.w("AdminActivity", "User is not an admin: " + userId);
                                    Toast.makeText(this, "Access denied: Admin role required.", Toast.LENGTH_LONG).show();
                                    Intent intent = new Intent(AdminActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    // Admin check passed, proceed to fetch users
                                    fetchUsers();
                                }
                            } else {
                                // If user document doesn't exist, create it with isAdmin=false
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("isAdmin", false);
                                userData.put("email", mAuth.getCurrentUser().getEmail());
                                db.collection("users").document(userId).set(userData)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.w("AdminActivity", "Created user document with isAdmin=false for: " + userId);
                                            Toast.makeText(this, "Access denied: Admin role required.", Toast.LENGTH_LONG).show();
                                            Intent intent = new Intent(AdminActivity.this, MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("AdminActivity", "Error creating user document: " + e.getMessage(), e);
                                            Toast.makeText(this, "Error initializing user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            Intent intent = new Intent(AdminActivity.this, MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        });
                            }
                        } catch (Exception e) {
                            Log.e("AdminActivity", "Crash in checkAdminStatus success: " + e.getMessage(), e);
                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("AdminActivity", "Error checking admin status: " + e.getMessage(), e);
                        Toast.makeText(this, "Error checking admin status: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(AdminActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    });
        } catch (Exception e) {
            Log.e("AdminActivity", "Crash in checkAdminStatus: " + e.getMessage(), e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void fetchUsers() {
        try {
            db.collection("users").get()
                    .addOnCompleteListener(task -> {
                        try {
                            if (task.isSuccessful()) {
                                userList.clear();
                                int totalUsers = 0;
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    totalUsers++;
                                    String uid = document.getId();
                                    Log.d("AdminActivity", "Found user: " + uid);
                                    if (!uid.equals(currentUserId)) {
                                        Map<String, Object> userData = document.getData();
                                        userData.put("uid", uid);
                                        if (!userData.containsKey("email") || userData.get("email") == null) {
                                            Log.w("AdminActivity", "Email missing in Firestore for user: " + uid + ". Please ensure the 'email' field is stored in Firestore during user registration or update the document manually in the Firebase Console.");
                                            userData.put("email", "Not available");
                                        }
                                        userList.add(userData);
                                        Log.d("AdminActivity", "Added user to list: " + uid + ", isAdmin: " + userData.get("isAdmin") + ", email: " + userData.get("email"));
                                    } else {
                                        Log.d("AdminActivity", "Excluded current admin user: " + uid);
                                    }
                                }
                                Log.d("AdminActivity", "Total users fetched: " + totalUsers);
                                Log.d("AdminActivity", "Users added to list (excluding admin): " + userList.size());

                                // Check if the activity is finishing before updating the UI
                                if (isFinishing()) {
                                    Log.w("AdminActivity", "Activity is finishing, skipping UI update");
                                    return;
                                }

                                // Show the user list container
                                userListContainer.setVisibility(View.VISIBLE);
                                Log.d("AdminActivity", "fetchUsers: user_list_container set to VISIBLE");

                                // Show or hide the empty list placeholder
                                if (userList.isEmpty()) {
                                    emptyUserListTextView.setVisibility(View.VISIBLE);
                                    userRecyclerView.setVisibility(View.GONE);
                                    Log.d("AdminActivity", "fetchUsers: Showing empty user list placeholder");
                                } else {
                                    emptyUserListTextView.setVisibility(View.GONE);
                                    userRecyclerView.setVisibility(View.VISIBLE);
                                    Log.d("AdminActivity", "fetchUsers: Showing user list");
                                }

                                userAdapter.notifyDataSetChanged();
                                Log.d("AdminActivity", "fetchUsers: Adapter notified of data change");
                            } else {
                                Log.e("AdminActivity", "Error fetching users: " + task.getException().getMessage(), task.getException());
                                if (!isFinishing()) {
                                    Toast.makeText(this, "Error fetching users: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        } catch (Exception e) {
                            Log.e("AdminActivity", "Crash in fetchUsers success: " + e.getMessage(), e);
                            if (!isFinishing()) {
                                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                finish();
                            }
                        }
                    });
        } catch (Exception e) {
            Log.e("AdminActivity", "Crash in fetchUsers: " + e.getMessage(), e);
            if (!isFinishing()) {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void onUserClicked(String userId) {
        try {
            Intent intent = new Intent(AdminActivity.this, UserRecordsActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        } catch (Exception e) {
            Log.e("AdminActivity", "Crash in onUserClicked: " + e.getMessage(), e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}