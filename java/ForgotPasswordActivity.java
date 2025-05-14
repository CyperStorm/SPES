package com.example.colorsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText emailEditText;
    private Button sendResetEmailButton;
    private TextView backToLoginLink;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        emailEditText = findViewById(R.id.emailEditText);
        sendResetEmailButton = findViewById(R.id.sendResetEmailButton);
        backToLoginLink = findViewById(R.id.backToLoginLink);

        // Send Reset Email button click listener
        sendResetEmailButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(ForgotPasswordActivity.this, "Please enter your email to reset password", Toast.LENGTH_SHORT).show();
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(ForgotPasswordActivity.this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            } else {
                sendResetEmailButton.setEnabled(false);
                Toast.makeText(ForgotPasswordActivity.this, "Sending reset email...", Toast.LENGTH_SHORT).show();
                mAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            sendResetEmailButton.setEnabled(true);
                            if (task.isSuccessful()) {
                                Toast.makeText(ForgotPasswordActivity.this, "Password reset email sent! Check your inbox or spam folder.", Toast.LENGTH_LONG).show();
                            } else {
                                String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                                Toast.makeText(ForgotPasswordActivity.this, "Failed to send reset email: " + errorMessage, Toast.LENGTH_LONG).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            sendResetEmailButton.setEnabled(true);
                            Toast.makeText(ForgotPasswordActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            }
        });

        // Back to Login link click listener
        backToLoginLink.setOnClickListener(v -> {
            Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
}