package com.example.mealmate.dashboard

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
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.example.mealmate.R
import com.example.mealmate.SavedRecipesFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream

class SavedCategoriesFragment : Fragment() {

    // Firebase instances
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var homeButton: ImageButton
    private lateinit var discoverButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private lateinit var cardContainer: LinearLayout
    private lateinit var coverPhotoImage: ImageView
    private var selectedImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.categories_saved, container, false)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize buttons and containers
        homeButton = view.findViewById(R.id.home_button)
        discoverButton = view.findViewById(R.id.discover_button)
        settingsButton = view.findViewById(R.id.settings_button)
        cardContainer = view.findViewById(R.id.card_container)
        homeButton.isSelected = true

        // Set up click listeners for each button
        homeButton.setOnClickListener { selectButton(homeButton) }
        discoverButton.setOnClickListener { selectButton(discoverButton) }
        settingsButton.setOnClickListener { selectButton(settingsButton) }

        // Apply window insets to padding for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Find the "New Category" card
        val newCategoryCard = view.findViewById<View>(R.id.new_category_card)
        newCategoryCard.setOnClickListener { showAddCategoryDialog() }

        // Load categories from Firestore
        loadCategoriesFromFirestore()

        return view
    }

    // Function to load categories from Firestore in alphabetical order
    private fun loadCategoriesFromFirestore() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid
        val categoriesRef = db.collection("users").document(userId).collection("categories")

        // Use orderBy to sort the documents by the "name" field in alphabetical order
        categoriesRef.orderBy("name").get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val categoryName = document.getString("name") ?: ""
                    val filePath = document.getString("imageUri") ?: ""
                    val imageUri = if (filePath.isNotEmpty()) Uri.fromFile(File(filePath)) else null

                    // Add the category to the UI
                    addNewItemCard(categoryName, imageUri)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error fetching categories: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK && result.data != null) {
            selectedImageUri = result.data?.data
            if (selectedImageUri != null) {
                coverPhotoImage.setImageURI(selectedImageUri)
                view?.findViewById<TextView>(R.id.upload_text)?.visibility = View.GONE
                coverPhotoImage.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
                coverPhotoImage.layoutParams.height = LinearLayout.LayoutParams.MATCH_PARENT
                coverPhotoImage.scaleType = ImageView.ScaleType.CENTER_CROP
                coverPhotoImage.requestLayout()
            }
        }
    }

    // Function to select a button and handle navigation
    private fun selectButton(selectedButton: ImageButton) {
        homeButton.isSelected = false
        discoverButton.isSelected = false
        settingsButton.isSelected = false

        selectedButton.isSelected = true

        // Handle navigation based on selected button
        when (selectedButton) {
            homeButton -> navigateToFragment(SavedCategoriesFragment())
            discoverButton -> navigateToFragment(SavedCategoriesFragment()) // Replace with DiscoverFragment
            settingsButton -> navigateToFragment(SavedCategoriesFragment()) // Replace with SettingsFragment
        }
    }

    // Function to navigate to a fragment
    private fun navigateToFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showAddCategoryDialog() {
        val dialog = Dialog(requireContext())
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
                if (selectedImageUri != null) {
                    val filePath = copyImageToLocalStorage(selectedImageUri!!)
                    if (filePath != null) {
                        saveCategoryToFirestore(newCategoryName, filePath)
                    } else {
                        Toast.makeText(requireContext(), "Failed to save image locally", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    saveCategoryToFirestore(newCategoryName, null)
                }
                dialog.dismiss()
                // Refresh the categories
                refreshCategories()
            }
        }

        cancelButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun refreshCategories() {
        // Remove all views except the "New Category" card
        for (i in cardContainer.childCount - 1 downTo 0) {
            val childView = cardContainer.getChildAt(i)
            if (childView.findViewById<TextView>(R.id.item_title)?.text != "New Category") {
                cardContainer.removeViewAt(i)
            }
        }
        loadCategoriesFromFirestore()
    }

    private fun copyImageToLocalStorage(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val file = File(requireContext().filesDir, "${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveCategoryToFirestore(categoryName: String, filePath: String?) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid
        val categoriesRef = db.collection("users").document(userId).collection("categories")

        val categoryData = hashMapOf(
            "name" to categoryName,
            "imageUri" to (filePath ?: "")
        )

        categoriesRef.add(categoryData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Category added successfully to Firestore!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error adding category: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addNewItemCard(newName: String, imageUri: Uri?) {
        val inflater = LayoutInflater.from(requireContext())
        val newItemCard = inflater.inflate(R.layout.categories_default_card, cardContainer, false)

        val itemTitle = newItemCard.findViewById<TextView>(R.id.item_title)
        val itemImage = newItemCard.findViewById<ImageView>(R.id.item_image)

        itemTitle.text = newName

        if (imageUri != null) {
            try {
                val bitmap = BitmapFactory.decodeFile(imageUri.path)
                itemImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        } else {
            val initialLetter = newName.firstOrNull()?.uppercaseChar().toString()
            val initialsDrawable = InitialsDrawable(requireContext(), initialLetter)
            initialsDrawable.color = Color.DKGRAY
            itemImage.setImageDrawable(initialsDrawable)
        }

        // Add click listener to navigate to SavedRecipesFragment
        newItemCard.setOnClickListener {
            navigateToRecipesFragment()
        }

        val newCategoryCardIndex = cardContainer.indexOfChild(view?.findViewById(R.id.new_category_card))
        cardContainer.addView(newItemCard, newCategoryCardIndex)
    }

    // Function to navigate to SavedRecipesFragment
    private fun navigateToRecipesFragment() {
        val fragment = SavedRecipesFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    REQUEST_PERMISSION_READ_STORAGE
                )
            } else {
                openGallery()
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
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

    companion object {
        private const val REQUEST_PERMISSION_READ_STORAGE = 101
    }
}
