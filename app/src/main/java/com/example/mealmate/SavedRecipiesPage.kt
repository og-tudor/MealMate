package com.example.mealmate

import InitialsDrawable
import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SavedRecipiesPage : AppCompatActivity() {

    private lateinit var homeButton: ImageButton
    private lateinit var discoverButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private lateinit var cardContainer: LinearLayout
    private lateinit var coverPhotoImage: ImageView
    private var selectedImageUri: Uri? = null // Store selected image URI here

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.saved_recipies)

        // Initialize buttons and containers
        homeButton = findViewById(R.id.home_button)
        discoverButton = findViewById(R.id.discover_button)
        settingsButton = findViewById(R.id.settings_button)
        cardContainer = findViewById(R.id.card_container)
        homeButton.isSelected = true

        // Set up click listeners for each button
        homeButton.setOnClickListener { selectButton(homeButton) }
        discoverButton.setOnClickListener { selectButton(discoverButton) }
        settingsButton.setOnClickListener { selectButton(settingsButton) }

        // Apply window insets to padding for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Find the "New Category" card
        val newCategoryCard = findViewById<View>(R.id.new_category_card)

        // Set a click listener on the "New Category" card
        newCategoryCard.setOnClickListener { showAddCategoryDialog() }
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            selectedImageUri = result.data?.data
            if (selectedImageUri != null) {
                coverPhotoImage.setImageURI(selectedImageUri)

                // Hide the "Upload a photo" text once an image is loaded
                findViewById<TextView>(R.id.upload_text)?.visibility = View.GONE

                // Expand the ImageView to fill the LinearLayout
                coverPhotoImage.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
                coverPhotoImage.layoutParams.height = LinearLayout.LayoutParams.MATCH_PARENT
                coverPhotoImage.scaleType = ImageView.ScaleType.CENTER_CROP
                coverPhotoImage.requestLayout()
            }
        }
    }

    private fun showAddCategoryDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_category)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Set dialog size
        val dialogWidth = (resources.displayMetrics.widthPixels * 0.85).toInt()
        val dialogHeight = (resources.displayMetrics.heightPixels * 0.6).toInt()
        dialog.window?.setLayout(dialogWidth, dialogHeight)

        // Initialize views in dialog
        coverPhotoImage = dialog.findViewById(R.id.cover_photo_image)
        val coverPhotoSection = dialog.findViewById<LinearLayout>(R.id.cover_photo_section)
        coverPhotoSection.setOnClickListener { checkAndRequestPermission() }

        val saveButton = dialog.findViewById<Button>(R.id.save_button)
        val cancelButton = dialog.findViewById<Button>(R.id.cancel_button)
        val editTextName = dialog.findViewById<EditText>(R.id.new_category_name)

        saveButton.setOnClickListener {
            val newCategoryName = editTextName.text.toString().trim()
            if (newCategoryName.isNotEmpty()) {
                addNewItemCard(newCategoryName, selectedImageUri)
                dialog.dismiss()
            }
        }

        cancelButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    REQUEST_PERMISSION_READ_STORAGE
                )
            } else {
                openGallery()
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_PERMISSION_READ_STORAGE
                )
            } else {
                openGallery()
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun addNewItemCard(newName: String, imageUri: Uri?) {
        // Inflate a new item_card layout
        val inflater = LayoutInflater.from(this)
        val newItemCard = inflater.inflate(R.layout.item_card, cardContainer, false)

        // Find the TextView and ImageView within the inflated item_card layout
        val itemTitle = newItemCard.findViewById<TextView>(R.id.item_title)
        val itemImage = newItemCard.findViewById<ImageView>(R.id.item_image)

        // Set the category name
        itemTitle.text = newName

        if (imageUri != null) {
            // Display the selected image if available
            itemImage.setImageURI(imageUri)
        } else {
            // Use InitialsDrawable to display the first letter in a circular background
            val initialLetter = newName.firstOrNull()?.uppercaseChar().toString()
            val initialsDrawable = InitialsDrawable(this, initialLetter)
            initialsDrawable.color = Color.DKGRAY  // Background color
            itemImage.setImageDrawable(initialsDrawable)
        }

        // Add the new item card before the "New Category" card
        val newCategoryCardIndex = cardContainer.indexOfChild(findViewById(R.id.new_category_card))
        cardContainer.addView(newItemCard, newCategoryCardIndex)
    }


    // Helper function to create a TextDrawable with the first letter of the category name
    private fun createTextDrawable(letter: String): Drawable {
        val drawable = ContextCompat.getDrawable(this, R.drawable.default_circle_background)!!.mutate()
        val tintedDrawable = DrawableCompat.wrap(drawable)
        DrawableCompat.setTint(tintedDrawable, Color.DKGRAY) // Adjust color as needed

        // Add the letter as text over the drawable
        val canvas = Canvas()
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 32f // Adjust text size as needed
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val bounds = Rect()
        paint.getTextBounds(letter, 0, letter.length, bounds)
        val x = drawable.intrinsicWidth / 2
        val y = (drawable.intrinsicHeight / 2) - ((paint.descent() + paint.ascent()) / 2)
        canvas.drawText(letter, x.toFloat(), y, paint)

        return tintedDrawable
    }


    private fun selectButton(selectedButton: ImageButton) {
        homeButton.isSelected = false
        discoverButton.isSelected = false
        settingsButton.isSelected = false

        selectedButton.isSelected = true

        when (selectedButton) {
            homeButton -> startActivity(Intent(this, SavedRecipiesPage::class.java))
            discoverButton -> startActivity(Intent(this, SavedRecipiesPage::class.java))
            settingsButton -> startActivity(Intent(this, SavedRecipiesPage::class.java))
        }
    }

    companion object {
        private const val REQUEST_PERMISSION_READ_STORAGE = 101
    }
}
