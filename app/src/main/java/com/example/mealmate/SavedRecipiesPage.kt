package com.example.mealmate;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SavedRecipiesPage extends AppCompatActivity {

    private ImageButton homeButton, discoverButton, settingsButton;
    private LinearLayout cardContainer;

    private ImageView coverPhotoImage;
    private static final int REQUEST_IMAGE_PICK = 1;
    private static final int REQUEST_PERMISSION_READ_STORAGE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.saved_recipies);

        // Apply window insets to padding for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        homeButton = findViewById(R.id.home_button);
        homeButton.setSelected(true);
        discoverButton = findViewById(R.id.discover_button);
        settingsButton = findViewById(R.id.settings_button);

        // Set up click listeners for each button
        homeButton.setOnClickListener(v -> selectButton(homeButton));
        discoverButton.setOnClickListener(v -> selectButton(discoverButton));
        settingsButton.setOnClickListener(v -> selectButton(settingsButton));

        // Find the container for cards
        cardContainer = findViewById(R.id.card_container);

        // Find the "New Category" card
        View newCategoryCard = findViewById(R.id.new_category_card);

        // Set a click listener on the "New Category" card
        newCategoryCard.setOnClickListener(v -> showAddCategoryDialog());
    }

    private void showAddCategoryDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_category);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // Set dialog size
        int dialogWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
        int dialogHeight = (int) (getResources().getDisplayMetrics().heightPixels * 0.6);
        dialog.getWindow().setLayout(dialogWidth, dialogHeight);

        // Initialize views in dialog
        coverPhotoImage = dialog.findViewById(R.id.cover_photo_image);
        TextView uploadText = dialog.findViewById(R.id.upload_text);
        LinearLayout coverPhotoSection = dialog.findViewById(R.id.cover_photo_section);

        // Set OnClickListener for the cover photo section
        coverPhotoSection.setOnClickListener(v -> checkAndRequestPermission());

        Button saveButton = dialog.findViewById(R.id.save_button);
        Button cancelButton = dialog.findViewById(R.id.cancel_button);
        EditText editTextName = dialog.findViewById(R.id.new_category_name);

        saveButton.setOnClickListener(v -> {
            String newCategoryName = editTextName.getText().toString().trim();
            // TODO get the photo uploaded and send it as parameter
            if (!newCategoryName.isEmpty()) {
                addNewItemCard(newCategoryName);
                dialog.dismiss();
            }
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void checkAndRequestPermission() {
        // Check for storage permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 and above
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        REQUEST_PERMISSION_READ_STORAGE);
            } else {
                openGallery();
            }
        } else { // Android 12 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_PERMISSION_READ_STORAGE);
            } else {
                openGallery();
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_READ_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Permission denied to read your storage", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null && coverPhotoImage != null) {
                coverPhotoImage.setImageURI(selectedImageUri); // Display the selected image

                // Hide the "Upload a photo" text once an image is loaded
                TextView uploadText = findViewById(R.id.upload_text);
                if (uploadText != null) {
                    uploadText.setVisibility(View.GONE);
                }

                // Expand the ImageView to fill the LinearLayout
                coverPhotoImage.getLayoutParams().width = LinearLayout.LayoutParams.MATCH_PARENT;
                coverPhotoImage.getLayoutParams().height = LinearLayout.LayoutParams.MATCH_PARENT;
                coverPhotoImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                coverPhotoImage.requestLayout(); // Apply changes
            }
        }
    }

    private void addNewItemCard(String newName) {
        // Inflate a new item_card layout
        LayoutInflater inflater = LayoutInflater.from(this);
        View newItemCard = inflater.inflate(R.layout.item_card, cardContainer, false);

        // Find the TextView within the inflated item_card layout and set the category name
        TextView itemTitle = newItemCard.findViewById(R.id.item_title);
        itemTitle.setText(newName);

        // Add the new item card before the "New Category" card
        int newCategoryCardIndex = cardContainer.indexOfChild(findViewById(R.id.new_category_card));
        cardContainer.addView(newItemCard, newCategoryCardIndex);
    }

    private void selectButton(ImageButton selectedButton) {
        // Deselect all buttons
        homeButton.setSelected(false);
        discoverButton.setSelected(false);
        settingsButton.setSelected(false);

        // Select the clicked button
        selectedButton.setSelected(true);

        // Perform navigation or other actions based on the selected button
        if (selectedButton == homeButton) {
            startActivity(new Intent(this, SavedRecipiesPage.class));
        } else if (selectedButton == discoverButton) {
            startActivity(new Intent(this, SavedRecipiesPage.class));
        } else if (selectedButton == settingsButton) {
            startActivity(new Intent(this, SavedRecipiesPage.class));
        }
    }
}
