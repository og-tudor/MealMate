package com.example.mealmate

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.mealmate.dashboard.SavedCategoriesFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SavedRecipesFragment : Fragment() {

    private lateinit var coverPhotoImage: ImageView
    private var selectedImageUri: Uri? = null
    private val db = FirebaseFirestore.getInstance()
    private lateinit var cardContainer: GridLayout
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.recipes_saved, container, false)
        val categoryId = arguments?.getString("categoryId")
        // Find the "Return" button, make it visible, and set up the click listener
        val returnButton: TextView = view.findViewById(R.id.return_button)
        cardContainer = view.findViewById(R.id.card_container)
        returnButton.visibility = View.VISIBLE
        returnButton.text = "< Categories"

        // Set an OnClickListener to handle the button click and navigate back
        returnButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SavedCategoriesFragment())
                .addToBackStack(null)
                .commit()

            returnButton.visibility = View.INVISIBLE
        }

        val newRecipeCard = view.findViewById<View>(R.id.new_recipe_card)
        newRecipeCard.setOnClickListener { showAddRecipeDialog() }

        // Use the categoryId to query Firestore
        if (categoryId != null) {
            loadRecipesFromFirestore(categoryId)
        }

        return view
    }

    // Function to load recipes from Firestore
    private fun loadRecipesFromFirestore(categoryId: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid
        val recipesRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("categories")
            .document(categoryId)
            .collection("recipes")

        // Fetch recipes and handle them as needed
        recipesRef.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val recipeName = document.getString("name") ?: ""
                    val imageUri = document.getString("imageUri") ?: ""
                    // make recipe cards for each recipe
                    createRecipeCard(recipeName, imageUri)

                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error fetching recipes: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // function to create a new recipe card
    private fun createRecipeCard(recipeName: String, imageUri: String) {
        val recipeCard = layoutInflater.inflate(R.layout.recipe_default_card, null)
        val recipeNameTextView = recipeCard.findViewById<TextView>(R.id.item_title)
        val recipeImage = recipeCard.findViewById<ImageView>(R.id.item_image)

        recipeNameTextView.text = recipeName
        if (imageUri.isNotEmpty()) {
            recipeImage.setImageURI(Uri.parse(imageUri))
        }

        // Set an OnClickListener to handle the card click
        recipeCard.setOnClickListener {
            // Handle the click event
        }

        // Add the card to the layout
        cardContainer.addView(recipeCard)


    }

    private fun showAddRecipeDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.modal_add_card)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Change dialog name to "Add Recipe"
        dialog.findViewById<TextView>(R.id.modal_name).text = "Add Recipe"
        dialog.findViewById<EditText>(R.id.new_modal_name).hint = "Enter recipe name"

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
            val recipeName = editTextName.text.toString().trim()
            if (recipeName.isNotEmpty()) {
                // Save the recipe data to Firestore
                saveRecipeToFirestore(recipeName, selectedImageUri)
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Please enter a recipe name", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun saveRecipeToFirestore(recipeName: String, imageUri: Uri?) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid
        val categoryId = arguments?.getString("categoryId") // Retrieve the passed categoryId
        if (categoryId == null) {
            Toast.makeText(requireContext(), "Category ID is missing", Toast.LENGTH_SHORT).show()
            return
        }

        // Reference to the specific category's recipes collection
        val recipesRef = db.collection("users").document(userId)
            .collection("categories").document(categoryId)
            .collection("recipes")

        val recipeData = hashMapOf(
            "name" to recipeName,
            "imageUri" to (imageUri?.toString() ?: ""),
            "ingredients" to listOf<String>(), // Initialize with an empty list of ingredients
            "instructions" to "" // Initialize with an empty string for instructions
        )

        recipesRef.add(recipeData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Recipe added successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error adding recipe: ${e.message}", Toast.LENGTH_SHORT).show()
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

    companion object {
        private const val REQUEST_PERMISSION_READ_STORAGE = 101
    }
}
