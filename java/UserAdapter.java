package com.example.colorsystem;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<Map<String, Object>> userList;
    private String currentUserId;
    private Context context;
    private OnUserClickListener onUserClickListener;

    public interface OnUserClickListener {
        void onUserClicked(String userId);
    }

    public UserAdapter(List<Map<String, Object>> userList, String currentUserId, OnUserClickListener onUserClickListener, Context context) {
        this.userList = userList;
        this.currentUserId = currentUserId;
        this.onUserClickListener = onUserClickListener;
        this.context = context;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        Map<String, Object> user = userList.get(position);
        String uid = (String) user.get("uid");
        Boolean isAdmin = (Boolean) user.get("isAdmin");
        Object emailObj = user.get("email");

        // Set email
        String email;
        if (emailObj instanceof String) {
            email = (String) emailObj;
        } else {
            email = "Not available";
        }
        holder.emailTextView.setText("Email: " + email);

        // Set UID
        holder.uidTextView.setText("UID: " + uid);

        // Set the button text to the current role
        String role = (isAdmin != null && isAdmin) ? "Admin" : "User";
        holder.roleButton.setText(role);

        // Set up the PopupMenu for role selection
        holder.roleButton.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(context, holder.roleButton);
            popupMenu.getMenu().add("User");
            popupMenu.getMenu().add("Admin");
            popupMenu.setOnMenuItemClickListener(item -> {
                String selectedRole = item.getTitle().toString();
                boolean newIsAdmin = selectedRole.equals("Admin");

                // Only update if the role has changed
                if ((isAdmin == null && newIsAdmin) || (isAdmin != null && isAdmin != newIsAdmin)) {
                    updateUserRole(uid, newIsAdmin);
                    holder.roleButton.setText(selectedRole);
                }
                return true;
            });

            // Apply the custom style to the PopupMenu
            try {
                popupMenu.show();
            } catch (Exception e) {
                Log.e("UserAdapter", "Error showing PopupMenu: " + e.getMessage(), e);
                Toast.makeText(context, "Error showing role options", Toast.LENGTH_SHORT).show();
            }
        });

        // Set click listener for the entire item (e.g., to view user details)
        holder.itemView.setOnClickListener(v -> {
            if (onUserClickListener != null) {
                onUserClickListener.onUserClicked(uid);
            }
        });

        Log.d("UserAdapter", "Binding user at position " + position + ": UID=" + uid + ", isAdmin=" + isAdmin + ", email=" + email);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    private void updateUserRole(String userId, boolean isAdmin) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> updates = new HashMap<>();
        updates.put("isAdmin", isAdmin);

        db.collection("users").document(userId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "User role updated to " + (isAdmin ? "Admin" : "User"), Toast.LENGTH_SHORT).show();
                    // Update the local userList to reflect the change
                    for (Map<String, Object> user : userList) {
                        if (user.get("uid").equals(userId)) {
                            user.put("isAdmin", isAdmin);
                            break;
                        }
                    }
                    notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("UserAdapter", "Error updating user role: " + e.getMessage(), e);
                    Toast.makeText(context, "Error updating user role: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView emailTextView, uidTextView;
        Button roleButton;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            emailTextView = itemView.findViewById(R.id.emailTextView);
            uidTextView = itemView.findViewById(R.id.uidTextView);
            roleButton = itemView.findViewById(R.id.roleButton);
        }
    }
}