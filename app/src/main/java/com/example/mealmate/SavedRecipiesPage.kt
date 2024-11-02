package com.example.mealmate

import InitialsDrawable
import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SavedRecipiesPage : AppCompatActivity() {

    // Firebase instances
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var homeButton: ImageButton
    private lateinit var discoverButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private lateinit var cardContainer: LinearLayout
    private lateinit var coverPhotoImage: ImageView
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.saved_recipies)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Find the "New Category" card
        val newCategoryCard = findViewById<View>(R.id.new_category_card)
        newCategoryCard.setOnClickListener { showAddCategoryDialog() }

        // Load categories from Firestore
        loadCategoriesFromFirestore()
    }

    // Function to load categories from Firestore
    private fun loadCategoriesFromFirestore() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid
        val categoriesRef = db.collection("users").document(userId).collection("categories")

        categoriesRef.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val categoryName = document.getString("name") ?: ""
                    val imageUriString = document.getString("imageUri") ?: ""
                    val imageUri = if (imageUriString.isNotEmpty()) Uri.parse(imageUriString) else null

                    // Add the category to the UI
                    addNewItemCard(categoryName, imageUri)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching categories: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            selectedImageUri = result.data?.data
            if (selectedImageUri != null) {
                coverPhotoImage.setImageURI(selectedImageUri)
                findViewById<TextView>(R.id.upload_text)?.visibility = View.GONE
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
                // Save the category to Firestore
                saveCategoryToFirestore(newCategoryName, selectedImageUri)
                addNewItemCard(newCategoryName, selectedImageUri)
                dialog.dismiss()
            }
        }

        cancelButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun saveCategoryToFirestore(categoryName: String, imageUri: Uri?) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid
        val categoriesRef = db.collection("users").document(userId).collection("categories")

        val categoryData = hashMapOf(
            "name" to categoryName,
            "imageUri" to (imageUri?.toString() ?: "")  // Save image URI as a string
        )

        categoriesRef.add(categoryData)
            .addOnSuccessListener {
                Toast.makeText(this, "Category added successfully to Firestore!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error adding category: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
        val inflater = LayoutInflater.from(this)
        val newItemCard = inflater.inflate(R.layout.item_card, cardContainer, false)

        val itemTitle = newItemCard.findViewById<TextView>(R.id.item_title)
        val itemImage = newItemCard.findViewById<ImageView>(R.id.item_image)

        itemTitle.text = newName

        if (imageUri != null) {
            try {
                // Use ContentResolver to get an InputStream
                val inputStream = contentResolver.openInputStream(imageUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                itemImage.setImageBitmap(bitmap)
                inputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
            selectedImageUri = null
        } else {
            val initialLetter = newName.firstOrNull()?.uppercaseChar().toString()
            val initialsDrawable = InitialsDrawable(this, initialLetter)
            initialsDrawable.color = Color.DKGRAY
            itemImage.setImageDrawable(initialsDrawable)
        }

        val newCategoryCardIndex = cardContainer.indexOfChild(findViewById(R.id.new_category_card))
        cardContainer.addView(newItemCard, newCategoryCardIndex)
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
