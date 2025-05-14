package com.example.colorsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UserActivity extends AppCompatActivity {

    private TextView userEmailTextView, userUidTextView;
    private Button backToMainButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        userEmailTextView = findViewById(R.id.userEmailTextView);
        userUidTextView = findViewById(R.id.userUidTextView);
        backToMainButton = findViewById(R.id.backToMainButton);

        // Get current user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Display email and UID
            userEmailTextView.setText(currentUser.getEmail());
            userUidTextView.setText(currentUser.getUid());
        } else {
            // If no user is logged in, redirect to LoginActivity
            Intent intent = new Intent(UserActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        // Back to Main button click listener
        backToMainButton.setOnClickListener(v -> {
            Intent intent = new Intent(UserActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}