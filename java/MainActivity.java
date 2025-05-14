package com.example.colorsystem;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.chrisbanes.photoview.PhotoView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.Manifest;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import android.view.MenuItem;


public class MainActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_CODE = 10;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private PhotoView photoView;
    private TextView colorInfo, descInfo, statusInfo;
    private Bitmap bitmap;
    private Uri photoURI;

    private StorageReference storageReference;

    private FirebaseFirestore firestore;

    private FirebaseAuth mAuth;
    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private ActionBarDrawerToggle drawerToggle;

    // SharedPreferences for "Remember Me" data
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "LoginPrefs";
    private static final String KEY_REMEMBER = "remember";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize DrawerLayout and NavigationView
        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);

        // Set up the ActionBarDrawerToggle for the hamburger icon
        drawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference("images");

        // Check if user is logged in
        if (mAuth.getCurrentUser() == null) {
            // User is not logged in, redirect to LoginActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Check admin status and update menu visibility
        String userId = mAuth.getCurrentUser().getUid();
        checkAdminStatusAndUpdateMenu(userId);

        // Handle NavigationView item clicks
        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_user_page) {
                Intent intent = new Intent(MainActivity.this, UserActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_admin_page) {
                // Navigate to AdminActivity (already checked for admin status in menu visibility)
                Intent intent = new Intent(MainActivity.this, AdminActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_view_records) {
                Intent intent = new Intent(MainActivity.this, RecordsActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_logout) {
                // Sign out from Firebase
                mAuth.signOut();

                // Clear "Remember Me" data
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(KEY_REMEMBER, false);
                editor.remove(KEY_EMAIL);
                editor.remove(KEY_PASSWORD);
                editor.apply();

                // Navigate to LoginActivity
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear back stack
                startActivity(intent);
                finish();
            }
            drawerLayout.closeDrawers();
            return true;
        });

        // Initialize views
        photoView = findViewById(R.id.photoView);
        Button uploadButton = findViewById(R.id.uploadButton);
        Button cameraButton = findViewById(R.id.cameraButton);
        ImageButton resetButton = findViewById(R.id.resetButton); // Initialize Reset button
        colorInfo = findViewById(R.id.colorInfo);
        descInfo = findViewById(R.id.descInfo);
        statusInfo = findViewById(R.id.statusInfo);

        List<Map<String, Object>> recordList = new ArrayList<>();
        RecordAdapter adapter = new RecordAdapter(recordList);

        // Add RippleEffectView to the root view
        ViewGroup rootView = findViewById(android.R.id.content); // Root view of the activity
        RippleEffectView rippleEffectView = new RippleEffectView(this);
        rootView.addView(rippleEffectView);

        // Set up button to upload an image
        uploadButton.setOnClickListener(view -> openImageSelector());

        // Set up button to take a photo
        cameraButton.setOnClickListener(view -> checkPermissionsAndDispatchTakePictureIntent());

        // Set up reset button to clear CardView results
        resetButton.setOnClickListener(view -> resetResults());

        // Set up touch listener for ripple effect
        rootView.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                rippleEffectView.startRipple(event.getRawX(), event.getRawY(), rootView);
            }
            return true;
        });

        photoView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                rippleEffectView.startRipple(event.getRawX(), event.getRawY(), rootView);

                // Ensure bitmap is not null
                if (bitmap == null) {
                    Log.e("Bitmap", "Bitmap is null. Cannot extract pixel data.");
                    Toast.makeText(this, "Image not loaded. Please select or capture an image.", Toast.LENGTH_SHORT).show();
                    return true;
                }

                // Ensure touch coordinates are within the bitmap's bounds
                int x = (int) event.getX();
                int y = (int) event.getY();
                if (x < bitmap.getWidth() && y < bitmap.getHeight()) {
                    int pixel = bitmap.getPixel(x, y);
                    int r = Color.red(pixel);
                    int g = Color.green(pixel);
                    int b = Color.blue(pixel);

                    // Ensure photoURI is valid
                    if (photoURI == null) {
                        Log.e("PhotoURI", "PhotoURI is null. Cannot upload image.");
                        Toast.makeText(this, "No image selected. Please select or capture an image.", Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    // Update information and initiate upload
                    updateInfo(r, g, b, x, y);
                } else {
                    Log.e("TouchEvent", "Touch coordinates out of bounds.");
                    Toast.makeText(this, "Touch outside image bounds.", Toast.LENGTH_SHORT).show();
                }
            }
            return true;
        });

        // Fetch records from Firestore
        firestore.collection("images")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        recordList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            recordList.add(document.getData());
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Log.e("FirestoreFetch", "Error fetching records", task.getException());
                    }
                });
    }

    private void checkAdminStatusAndUpdateMenu(String userId) {
        firestore.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean isAdmin = documentSnapshot.getBoolean("isAdmin");
                        Log.d("MainActivity", "User " + userId + " isAdmin: " + isAdmin);
                        // Update menu visibility based on admin status
                        MenuItem adminPageItem = navView.getMenu().findItem(R.id.nav_admin_page);
                        if (isAdmin != null && isAdmin) {
                            adminPageItem.setVisible(true);
                        } else {
                            adminPageItem.setVisible(false);
                            // If user document exists but isAdmin is false or null, ensure it's set to false
                            if (isAdmin == null) {
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("isAdmin", false);
                                firestore.collection("users").document(userId).update(userData);
                            }
                        }
                    } else {
                        // If user document doesn't exist, create it with isAdmin=false
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("isAdmin", false);
                        firestore.collection("users").document(userId).set(userData);
                        Log.d("MainActivity", "Created user document for " + userId + " with isAdmin: false");
                        // Hide admin page
                        navView.getMenu().findItem(R.id.nav_admin_page).setVisible(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MainActivity", "Error checking admin status: " + e.getMessage(), e);
                    Toast.makeText(this, "Error checking admin status.", Toast.LENGTH_SHORT).show();
                    // Hide admin page by default on error
                    navView.getMenu().findItem(R.id.nav_admin_page).setVisible(false);
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // Method to open the image selector
    private void openImageSelector() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, 1);
    }

    private void checkPermissionsAndDispatchTakePictureIntent() {
        if (hasAllPermissions()) {
            dispatchTakePictureIntent();
        } else {
            if (shouldShowRequestPermissionRationale()) {
                showPermissionExplanationDialog();
            } else {
                requestPermissions();
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
        } else {
            dispatchTakePictureIntent();
        }
    }

    private Bitmap rotateImageIfRequired(Bitmap img, Uri selectedImage) throws IOException {
        ExifInterface exif = new ExifInterface(getContentResolver().openInputStream(selectedImage));
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }

    private Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 1 && data != null) {
                Uri imageUri = data.getData();
                photoURI = imageUri; // Update photoURI here
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    bitmap = rotateImageIfRequired(bitmap, imageUri);
                    photoView.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoURI);
                    bitmap = rotateImageIfRequired(bitmap, photoURI);
                    photoView.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean shouldShowRequestPermissionRationale() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                return true;
            }
        }
        return false;
    }

    private void showPermissionExplanationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("This app needs camera and storage permissions to take and save photos. Please grant these permissions to continue.")
                .setPositiveButton("Grant Permissions", (dialog, which) -> requestPermissions())
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Toast.makeText(this, "Permissions are required to use this feature", Toast.LENGTH_LONG).show();
                })
                .show();
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
    }

    private boolean hasAllPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                Log.d("PermissionCheck", "Permission not granted: " + permission);
                return false;
            }
        }
        Log.d("PermissionCheck", "All permissions granted");
        return true;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this, "com.example.colorsystem.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        } else {
            Toast.makeText(this, "No Camera App Found", Toast.LENGTH_LONG).show();
        }
    }

    private File createImageFile() throws IOException {
        String imageFileName = "JPEG_" + System.currentTimeMillis() + "_";
        File storageDir = getExternalFilesDir(null);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return image;
    }

    private String rgbToMunsell(int r, int g, int b) {
        double[] rgb = {r / 255.0, g / 255.0, b / 255.0};
        double[] lab = srgbToLab(rgb);

        // Munsell conversion
        double hue = (Math.atan2(lab[2], lab[1]) * 180 / Math.PI + 360) % 360;
        double value = lab[0] / 10;
        double chroma = Math.sqrt(lab[1] * lab[1] + lab[2] * lab[2]) / 5;

        List<String> hueNames = Arrays.asList("R", "YR", "Y", "GY", "G", "BG", "B", "PB", "P", "RP");
        int hueIndex = (int) (hue / 36);

        // Calculate the nearest 2.5, 5, 7.5, or 10 hue division
        double[] steps = {2.5, 5.0, 7.5, 10.0};
        double rawHueValue = (hue % 36) / 3.6;
        double nearestStep = steps[0];
        double minDifference = Math.abs(rawHueValue - steps[0]);
        for (double step : steps) {
            double difference = Math.abs(rawHueValue - step);
            if (difference < minDifference) {
                minDifference = difference;
                nearestStep = step;
            }
        }

        String munsellHue = String.format("%.1f%s", nearestStep, hueNames.get(hueIndex));
        String munsellValue = String.format("%d", (int) Math.round(value));
        String munsellChroma = String.format("%d", (int) Math.round(chroma));

        return String.format("%s %s/%s", munsellHue, munsellValue, munsellChroma);
    }

    private double[] srgbToLab(double[] rgb) {
        double[] xyz = new double[3];
        for (int i = 0; i < 3; i++) {
            rgb[i] = (rgb[i] > 0.04045) ? Math.pow((rgb[i] + 0.055) / 1.055, 2.4) : rgb[i] / 12.92;
        }
        xyz[0] = rgb[0] * 0.4124564 + rgb[1] * 0.3575761 + rgb[2] * 0.1804375;
        xyz[1] = rgb[0] * 0.2126729 + rgb[1] * 0.7151522 + rgb[2] * 0.0721750;
        xyz[2] = rgb[0] * 0.0193339 + rgb[1] * 0.1191920 + rgb[2] * 0.9503041;

        double[] lab = new double[3];
        double[] white = {0.95047, 1.00000, 1.08883};
        for (int i = 0; i < 3; i++) {
            xyz[i] /= white[i];
            xyz[i] = (xyz[i] > 0.008856) ? Math.pow(xyz[i], 1.0 / 3.0) : (7.787 * xyz[i]) + (16.0 / 116.0);
        }
        lab[0] = (116.0 * xyz[1]) - 16.0;
        lab[1] = 500.0 * (xyz[0] - xyz[1]);
        lab[2] = 200.0 * (xyz[1] - xyz[2]);
        return lab;
    }

    private String getSedimentDescriptionFromMunsell(String munsell) {
        // split into ["2.5YR","6/4"]
        String[] parts = munsell.split(" ");
        if (parts.length < 2) {
            return "Unclassified sediment – characteristics do not match defined categories. May indicate mixed or unusual composition.";
        }

        String hueCode = parts[0];            // e.g. "2.5YR"
        String[] vc     = parts[1].split("/"); // ["6","4"]
        double value   = Double.parseDouble(vc[0]);
        double chroma  = Double.parseDouble(vc[1]);

        // 1) Bright & colorful → quartz sand / carbonate ooze
        if (value >= 8.0 && chroma > 2.0) {
            return "Light-colored sediment – likely quartz-rich sand or carbonate ooze.";
        }
        // 2) Bright & neutral → calcareous silt / biogenic deposits
        if (value >= 8.0 && chroma <= 2.0) {
            return "Light gray or beige sediment – could be calcareous silt or biogenic deposits.";
        }
        // 3) Very dark → organic-rich mud / clay
        if (value <= 3.0) {
            return "Dark-colored sediment – possibly organic-rich mud or clay under anoxic conditions.";
        }
        // 4) Mid-tone but colorful → map by hue
        if (chroma > 2.0) {
            // red‐brown (iron oxide)
            if (hueCode.endsWith("R") || hueCode.contains("YR")) {
                return "Reddish to brown sediment – indicative of iron oxide presence (e.g., ferruginous clay).";
            }
            // yellow/buff
            if (hueCode.endsWith("Y")) {
                return "Yellow to buff-colored sediment – possibly oxidized silty clay or volcanic ash.";
            }
            // green
            if (hueCode.endsWith("G") || hueCode.contains("GY")) {
                return "Greenish sediment – possibly glauconite-rich clay or reduced iron-bearing mud.";
            }
            // blue/gray
            if (hueCode.endsWith("B") || hueCode.contains("BG")) {
                return "Bluish-gray sediment – may suggest sulfate-reducing conditions or fine-grained anoxic clay.";
            }
        }
        // 5) Everything else → mixed silts/clays/fine sand
        return "Moderate-tone sediment – likely mixed composition of silts, clays, and fine sand.";
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                // Handle permission denial
            }
        }
    }

    private Bitmap getScaledBitmap(Uri uri) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, options);

        // Calculate the sample size
        options.inSampleSize = calculateInSampleSize(options, 1024, 1024);
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private class RippleEffectView extends View {
        private Paint ripplePaint;
        private float touchX = -1;
        private float touchY = -1;
        private float rippleRadius = 0;
        private boolean isRippleActive = false;

        public RippleEffectView(Context context) {
            super(context);
            init();
        }

        public RippleEffectView(Context context, AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        private void init() {
            ripplePaint = new Paint();
            ripplePaint.setColor(Color.BLUE); // Change color as needed
            ripplePaint.setStyle(Paint.Style.FILL);
            ripplePaint.setAlpha(150); // Adjust transparency
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (isRippleActive && touchX >= 0 && touchY >= 0) {
                canvas.drawCircle(touchX, touchY, rippleRadius, ripplePaint);
            }
        }

        public void startRipple(float x, float y, View targetView) {
            int[] viewLocation = new int[2];
            targetView.getLocationOnScreen(viewLocation);

            // Check if the touch is within the bounds of the target view
            if (x >= viewLocation[0] && x <= viewLocation[0] + targetView.getWidth() &&
                    y >= viewLocation[1] && y <= viewLocation[1] + targetView.getHeight()) {

                int[] rippleLocation = new int[2];
                this.getLocationOnScreen(rippleLocation);

                touchX = x - rippleLocation[0];
                touchY = y - rippleLocation[1];
                rippleRadius = 0;
                isRippleActive = true;
                invalidate();
                postDelayed(this::expandRipple, 16);
            }
        }

        private void expandRipple() {
            if (isRippleActive) {
                rippleRadius += 10; // Increment the radius
                if (rippleRadius > 50) { // Stop the ripple after it reaches a certain size
                    isRippleActive = false;
                    touchX = -1;
                    touchY = -1;
                } else {
                    postDelayed(this::expandRipple, 16); // Repeat animation
                }
                postInvalidate();
            }
        }
    }

    private void uploadImageToImgur(Uri imageUri, String rgb, String hex, String munsell, String description, int x, int y) {
        try {
            // Open an InputStream for the image URI
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Log.e("ImgurUpload", "InputStream is null for the provided URI.");
                Toast.makeText(this, "Unable to access the image. Please try again.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create a RequestBody for the image
            byte[] imageBytes = getBytesFromInputStream(inputStream);
            RequestBody requestBody = RequestBody.create(imageBytes, okhttp3.MediaType.parse("image/*"));
            MultipartBody.Part body = MultipartBody.Part.createFormData("image", "image.jpg", requestBody);

            // Get the Imgur API service
            ImgurService imgurService = RetrofitClient.getClient().create(ImgurService.class);
            Call<ResponseBody> call = imgurService.uploadImage(body);

            call.enqueue(new retrofit2.Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        try {
                            // Parse the response to get the image URL
                            String jsonResponse = response.body().string();
                            JSONObject jsonObject = new JSONObject(jsonResponse);
                            String imageUrl = jsonObject.getJSONObject("data").getString("link");
                            Log.d("ImgurUpload", "Image URL: " + imageUrl);

                            // Save metadata and the Imgur image URL to Firestore
                            saveMetadataToFirestore(rgb, hex, munsell, description, x, y, imageUrl);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e("ImgurUpload", "Failed to parse response.");
                        }
                    } else {
                        Log.e("ImgurUpload", "Upload failed: " + response.message());
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.e("ImgurUpload", "Error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ImgurUpload", "Failed to upload image: " + e.getMessage());
            Toast.makeText(this, "Image upload failed. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    // Helper method to convert InputStream to byte array
    private byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    private void saveMetadataToFirestore(String rgb, String hex, String munsell, String description, int x, int y, String imageUrl) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        String userId = mAuth.getCurrentUser().getUid(); // Get logged-in user's UID

        Map<String, Object> imageData = new HashMap<>();
        imageData.put("rgb", rgb);
        imageData.put("hex", hex);
        imageData.put("munsell", munsell);
        imageData.put("description", description);
        imageData.put("position", new HashMap<String, Object>() {{
            put("x", x);
            put("y", y);
        }});
        imageData.put("imageUrl", imageUrl);

        // Add timestamp
        imageData.put("timestamp", FieldValue.serverTimestamp());

        db.collection("users").document(userId)
                .collection("records")
                .add(imageData)
                .addOnSuccessListener(documentReference -> Log.d("FirestoreSave", "Document added with ID: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.e("FirestoreSave", "Error adding document", e));
    }

    private String getRealPathFromURI(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);

        if (cursor != null) {
            try {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                return cursor.getString(columnIndex);
            } catch (IllegalArgumentException e) {
                Log.e("FilePath", "Column not found: " + e.getMessage());
            } finally {
                cursor.close();
            }
        }

        Log.e("FilePath", "Cursor is null or could not resolve the file path.");
        return null;
    }

    private void updateInfo(int r, int g, int b, int x, int y) {
        String rgbHex = String.format("#%02x%02x%02x", r, g, b);
        String munsellColor = rgbToMunsell(r, g, b);
        String sedimentDesc = getSedimentDescriptionFromMunsell(munsellColor);

        colorInfo.setText(String.format("RGB: %d, %d, %d | Hex: %s | Munsell: %s", r, g, b, rgbHex, munsellColor));
        descInfo.setText("Sediment Description: " + sedimentDesc);
        statusInfo.setText(String.format("Position: (x: %d, y: %d)", x, y));

        if (photoURI == null) {
            Log.e("PhotoURI", "PhotoURI is null. Image cannot be uploaded.");
            return;
        }

        if (photoURI != null) {
            // Upload image to Imgur and save metadata
            uploadImageToImgur(photoURI,
                    String.format("%d, %d, %d", r, g, b),
                    rgbHex,
                    munsellColor,
                    sedimentDesc,
                    x,
                    y
            );
        }
    }

    // New method to reset results in the CardView
    private void resetResults() {
        // Reset TextViews to their initial state
        colorInfo.setText("Color Information");
        descInfo.setText("Sediment Description");
        statusInfo.setText("Position");

        // Notify user
        Toast.makeText(this, "Results reset", Toast.LENGTH_SHORT).show();
    }
    @Override
    protected void onStart() {
        super.onStart();

        if (mAuth.getCurrentUser() == null) {
            // User is not logged in, redirect to LoginActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
}
