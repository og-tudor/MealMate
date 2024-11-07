package com.example.mealmate.dashboard.home

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
import androidx.lifecycle.ViewModelProvider
import com.example.mealmate.R
import com.example.mealmate.SavedRecipesFragment
import com.example.mealmate.dashboard.GeneralFunctions
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

    private var categoriesLoaded = false
    private val cachedCategories = mutableListOf<Category>()


    // Use ViewModel to retain data
    private lateinit var categoriesViewModel: CategoriesViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.categories_saved, container, false)
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize UI elements
        homeButton = view.findViewById(R.id.home_button)
        discoverButton = view.findViewById(R.id.discover_button)
        settingsButton = view.findViewById(R.id.settings_button)
        cardContainer = view.findViewById(R.id.card_container)
        homeButton.isSelected = true

        // Initialize ViewModel using ViewModelProvider
        categoriesViewModel = ViewModelProvider(requireActivity()).get(CategoriesViewModel::class.java)

        // Clear existing views in cardContainer to avoid duplicates
        cardContainer.removeAllViews()

        // Check if layout is already initialized
        if (categoriesViewModel.layoutInitialized) {
            // Use cached data
            categoriesViewModel.cachedCategories.forEach { category ->
                // Add each category to the UI
                addNewItemCard(category.name, category.imageUri, category.id)
            }
            addNewCategoryCard()
        } else {
            // Load categories from Firestore and cache them
            loadCategoriesFromFirestore()
            categoriesViewModel.layoutInitialized = true
        }

        val generalFunctions = GeneralFunctions(requireActivity(), homeButton, discoverButton, settingsButton)
        // Set up click listeners for each button
        homeButton.setOnClickListener { generalFunctions.selectButton(homeButton) }
        discoverButton.setOnClickListener { generalFunctions.selectButton(discoverButton) }
        settingsButton.setOnClickListener { generalFunctions.selectButton(settingsButton) }

        // Apply window insets to padding for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        return view
    }

    // Method to add the "Add New Category" card at the end of cardContainer
    private fun addNewCategoryCard() {
        val inflater = LayoutInflater.from(requireContext())
        val newCategoryCard = inflater.inflate(R.layout.categories_new_card, cardContainer, false)

        // Set the click listener for the "Add New Category" card
        newCategoryCard.setOnClickListener {
            showAddCategoryDialog()
        }

        // Add the "Add New Category" card to the cardContainer
        cardContainer.addView(newCategoryCard)
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

        categoriesRef.orderBy("name").get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val categoryName = document.getString("name") ?: ""
                    val filePath = document.getString("imageUri") ?: ""
                    val imageUri = if (filePath.isNotEmpty()) Uri.fromFile(File(filePath)) else null
                    val categoryId = document.id

                    // Cache the category data in ViewModel
                    val category = Category(categoryName, imageUri, categoryId)
                    // Only add to cache if not already present
                    if (!categoriesViewModel.cachedCategories.contains(category)) {
                        categoriesViewModel.cachedCategories.add(category)
                    }

                    // Add the category to the UI
                    addNewItemCard(categoryName, imageUri, categoryId)
                }

                // Add the "Add New Category" card at the end, after all categories have been added
                addNewCategoryCard()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error fetching categories: ${e.message}", Toast.LENGTH_SHORT).show()

                // Still add the "Add New Category" card even if there's an error fetching categories
                addNewCategoryCard()
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


    private fun showAddCategoryDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.modal_add_card)
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
        val editTextName = dialog.findViewById<EditText>(R.id.new_modal_name)

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
        cardContainer.removeAllViews()
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

    private fun addNewItemCard(newName: String, imageUri: Uri?, categoryId: String) {
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

        // Add click listener to navigate to SavedRecipesFragment with categoryId
        newItemCard.setOnClickListener {
            navigateToRecipesFragment(categoryId)
        }

        newItemCard.setOnLongClickListener {
            // Inflate the popup view
            val inflater = LayoutInflater.from(requireContext())
            val popupView = inflater.inflate(R.layout.popup_window_categories, null)

            // Create a PopupWindow
            val popupWindow = PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )

            // Set up the Edit Button
            val editButton = popupView.findViewById<LinearLayout>(R.id.edit_button)
            val editTextView = editButton.findViewById<TextView>(R.id.menu_option_text)
            val editIconView = editButton.findViewById<ImageView>(R.id.menu_option_icon)
            editTextView.text = "Edit"
            editTextView.setTextColor(Color.GRAY)
            editIconView.setImageResource(R.drawable.icon_edit) // Set your edit icon

            // Set up the Delete Button
            val deleteButton = popupView.findViewById<LinearLayout>(R.id.delete_button)
            val deleteTextView = deleteButton.findViewById<TextView>(R.id.menu_option_text)
            val deleteIconView = deleteButton.findViewById<ImageView>(R.id.menu_option_icon)
            deleteTextView.text = "Delete"
            deleteIconView.setImageResource(R.drawable.icon_delete) // Set your delete icon

            // Set up the click listener for the "Delete" option
            deleteButton.setOnClickListener {
                deleteCategory(categoryId)
                popupWindow.dismiss() // Close the popup
            }

            // You can add a click listener for the "Edit" button as well
            editButton.setOnClickListener {
                // Handle the edit action
                showEditCategoryDialog(categoryId, newName, imageUri)
                popupWindow.dismiss() // Close the popup
            }

            // Show the PopupWindow
            popupWindow.showAsDropDown(newItemCard)
            true
        }

        val newCategoryCardIndex = cardContainer.indexOfChild(view?.findViewById(R.id.new_category_card))
        cardContainer.addView(newItemCard, newCategoryCardIndex)
    }

    private fun showEditCategoryDialog(categoryId: String, existingCategoryName: String, existingImageUri: Uri?) {
        // Create a dialog using the existing layout
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.modal_add_card)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Set dialog size
        val dialogWidth = (resources.displayMetrics.widthPixels * 0.85).toInt()
        val dialogHeight = (resources.displayMetrics.heightPixels * 0.6).toInt()
        dialog.window?.setLayout(dialogWidth, dialogHeight)

        // Access UI components
        val modalTitle = dialog.findViewById<TextView>(R.id.modal_name)
        val nameEditText = dialog.findViewById<EditText>(R.id.new_modal_name)
        val coverPhotoSection = dialog.findViewById<LinearLayout>(R.id.cover_photo_section)
        val coverPhotoImage = dialog.findViewById<ImageView>(R.id.cover_photo_image)
        val uploadText = dialog.findViewById<TextView>(R.id.upload_text)
        val saveButton = dialog.findViewById<Button>(R.id.save_button)
        val cancelButton = dialog.findViewById<Button>(R.id.cancel_button)

        // Customize the title and button text
        modalTitle.text = "Edit Category" // Change the title text
        saveButton.text = "Update" // Change the button text

        // Set the existing category name and image if available
        nameEditText.setText(existingCategoryName)
        if (existingImageUri != null) {
            coverPhotoImage.setImageURI(existingImageUri)
            uploadText.visibility = View.GONE
        }

        // Set up the click listeners
        coverPhotoSection.setOnClickListener { checkAndRequestPermission() }

        saveButton.setOnClickListener {
            val updatedCategoryName = nameEditText.text.toString().trim()
            if (updatedCategoryName.isNotEmpty()) {
                // Handle updating the category in Firestore or your database
                updateCategoryInFirestore(categoryId, updatedCategoryName)
                dialog.dismiss()
            }
        }

        cancelButton.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }


    private fun deleteCategory(categoryId: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid
        val categoryRef = db.collection("users").document(userId).collection("categories").document(categoryId)

        // Delete the category from Firestore
        categoryRef.delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Category deleted successfully", Toast.LENGTH_SHORT).show()
                // Remove the category from the UI and ViewModel cache
                categoriesViewModel.cachedCategories.removeAll { it.id == categoryId }
                refreshCategories() // Refresh the categories in the UI
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error deleting category: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    // Function to navigate to SavedRecipesFragment and pass categoryId
    private fun navigateToRecipesFragment(categoryId: String) {
        val fragment = SavedRecipesFragment()

        // Use a Bundle to pass the categoryId
        val bundle = Bundle()
        bundle.putString("categoryId", categoryId)
        fragment.arguments = bundle

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun updateCategoryInFirestore(categoryId: String, updatedCategoryName: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid
        val categoryRef = db.collection("users").document(userId).collection("categories").document(categoryId)

        // Create a map to update the fields
        val updates = hashMapOf<String, Any>(
            "name" to updatedCategoryName
        )

        // Update the category in Firestore
        categoryRef.update(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Category updated successfully", Toast.LENGTH_SHORT).show()
                // Update the cached category in ViewModel
                val category = categoriesViewModel.cachedCategories.find { it.id == categoryId }
                category?.name = updatedCategoryName

                // Refresh the categories to reflect the changes
                refreshCategories()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error updating category: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
