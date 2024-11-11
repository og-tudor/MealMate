package com.example.mealmate

import Ingredient
import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.mealmate.dashboard.GeneralFunctions
import com.example.mealmate.dashboard.home.SavedCategoriesFragment
import com.example.mealmate.model.Recipe
import com.example.mealmate.repository.CategoriesRepository
import com.example.mealmate.repository.RecipesRepository
import com.example.mealmate.utils.FragmentSource
import com.google.firebase.auth.FirebaseAuth
import com.yourpackage.name.RecipeFragment

class SavedRecipesFragment : Fragment() {

    private lateinit var coverPhotoImage: ImageView
    private lateinit var selectedPhotoBitmap: Bitmap
    private lateinit var cardContainer: GridLayout
    private lateinit var generalFunctions: GeneralFunctions
    private lateinit var homeButton: ImageButton
    private lateinit var discoverButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private var selectedPhoto: Bitmap? = null
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var categoryId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize gallery launcher early in the lifecycle
        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK && result.data != null) {
                val selectedImageUri = result.data?.data
                if (selectedImageUri != null) {
                    selectedPhoto = getBitmapFromUri(selectedImageUri)
                    if (selectedPhoto != null) {
                        coverPhotoImage.setImageBitmap(selectedPhoto)
                        view?.findViewById<TextView>(R.id.upload_text)?.visibility = View.GONE
                        coverPhotoImage.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
                        coverPhotoImage.layoutParams.height = LinearLayout.LayoutParams.MATCH_PARENT
                        coverPhotoImage.scaleType = ImageView.ScaleType.CENTER_CROP
                        coverPhotoImage.requestLayout()
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.recipes_library, container, false)
        categoryId = arguments?.getString("categoryId") ?: return view

        // Initialize UI elements
        homeButton = view.findViewById(R.id.home_button)
        discoverButton = view.findViewById(R.id.discover_button)
        settingsButton = view.findViewById(R.id.settings_button)
        cardContainer = view.findViewById(R.id.card_container)
        homeButton.isSelected = true

        generalFunctions = GeneralFunctions(requireActivity(), homeButton, discoverButton, settingsButton)
        // Set up click listeners for each button
        homeButton.setOnClickListener { generalFunctions.selectButton(homeButton) }
        discoverButton.setOnClickListener { generalFunctions.selectButton(discoverButton) }
        settingsButton.setOnClickListener { generalFunctions.selectButton(settingsButton) }

        val returnButton: TextView = view.findViewById(R.id.return_button)
        returnButton.visibility = View.VISIBLE
        returnButton.text = "< Categories"
        returnButton.setOnClickListener {
            generalFunctions.navigateToFragment(SavedCategoriesFragment(), hideView = returnButton)
        }

        val recipeAddNewCardButton = view.findViewById<View>(R.id.new_recipe_card)
        recipeAddNewCardButton.setOnClickListener { showAddRecipeDialog(categoryId) }

        // Load recipes from RecipeRepository
        loadRecipes(categoryId)

        return view
    }

//    private fun addNewRecipeCard() {
//        val newRecipeCard = view?.findViewById<View>(R.id.new_recipe_card)
//        newRecipeCard?.setOnClickListener { showAddRecipeDialog(categoryId) }
//
//        cardContainer.addView(newRecipeCard)
//    }

    private fun loadRecipes(categoryId: String) {
        RecipesRepository.loadRecipes(requireContext(), categoryId) { success ->
            if (success) {
                val recipes = RecipesRepository.getCachedRecipesForCategory(categoryId)
                recipes.forEach { recipe -> createRecipeCard(recipe) }
            } else {
                Toast.makeText(requireContext(), "Error loading recipes", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createRecipeCard(recipe: Recipe) {
        val recipeCard = layoutInflater.inflate(R.layout.recipes_library_default_card, null)
        val recipeNameTextView = recipeCard.findViewById<TextView>(R.id.item_title)
        val recipeImage = recipeCard.findViewById<ImageView>(R.id.recipe_library_item_image)

        recipeNameTextView.text = recipe.title
        recipe.imageBitmap?.let { recipeImage.setImageBitmap(it) }

        recipeCard.setOnClickListener {
            val ingredientsArray = recipe.ingredientsWithQuantities.map { (name, quantity) ->
                Ingredient(name, quantity)
            }.toTypedArray()

            val fragment = RecipeFragment.newInstance(
                recipeName = recipe.title,
                imageUri = recipe.imageUrl,
                ingredients = ingredientsArray,
                instructions = recipe.instructions,
                source = FragmentSource.SAVED_RECIPIES_LIBRARY
            )

            generalFunctions.navigateToFragment(fragment)
        }

        cardContainer.addView(recipeCard)
    }

    private fun setupRecipies() {
        // for every category in the database, add a card to the view
        val recipies = RecipesRepository.getCachedRecipesForCategory(categoryId)
        if (recipies.isNotEmpty()) {
            recipies.forEach { recipe ->
                addRecipeCard(recipe)
            }
        } else {
            Log.d("SavedCategoriesFragment", "No categories found.")
            Toast.makeText(requireContext(), "No categories available", Toast.LENGTH_SHORT).show()
        }
//        addNewRecipeCard()
    }

    private fun addRecipeCard(recipe: Recipe) {
        val inflater = LayoutInflater.from(requireContext())
        val recipeCard = inflater.inflate(R.layout.recipes_library_default_card, cardContainer, false)

        val itemTitle = recipeCard.findViewById<TextView>(R.id.item_title)
        val itemImage = recipeCard.findViewById<ImageView>(R.id.recipe_library_item_image)

        itemTitle.text = recipe.title
        itemImage.setImageBitmap(recipe.imageBitmap)

        recipeCard.setOnClickListener {
            val bundle = Bundle().apply {
                putString("recipeId", recipe.id)
            }
            generalFunctions.navigateToFragment(SavedRecipesFragment(), args = bundle)
        }

        cardContainer.addView(recipeCard)
    }


    private fun showAddRecipeDialog(categoryId: String) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.modal_add_card)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.findViewById<TextView>(R.id.modal_name).text = "Add Recipe"
        dialog.findViewById<EditText>(R.id.new_modal_name).hint = "Enter recipe name"
        val dialogWidth = (resources.displayMetrics.widthPixels * 0.85).toInt()
        val dialogHeight = (resources.displayMetrics.heightPixels * 0.6).toInt()
        dialog.window?.setLayout(dialogWidth, dialogHeight)

        coverPhotoImage = dialog.findViewById(R.id.cover_photo_image)
        val coverPhotoSection = dialog.findViewById<LinearLayout>(R.id.cover_photo_section)

        coverPhotoSection.setOnClickListener {
            Log.d("SavedCategoriesFragment", "Cover photo section clicked.")
            generalFunctions.openGallery { bitmap: Bitmap ->
                selectedPhoto = bitmap
                coverPhotoImage.setImageBitmap(bitmap)
            }
        }

        val saveButton = dialog.findViewById<Button>(R.id.save_button)
        val cancelButton = dialog.findViewById<Button>(R.id.cancel_button)
        val editTextName = dialog.findViewById<EditText>(R.id.new_modal_name)

        saveButton.setOnClickListener {
            val recipeName = editTextName.text.toString().trim()
            if (recipeName.isNotEmpty()) {
                val recipeItem = Recipe(
                    id = "",
                    title = recipeName,
                    instructions = "",
                    ingredientsWithQuantities = emptyList(),
                    imageBitmap = selectedPhoto
                )
                Log.d("SavedCategoriesFragment", "Save button clicked. Saving category: $recipeName")
                RecipesRepository.addNewRecipe(requireContext(), categoryId, recipeItem, selectedPhoto) { success ->
                    if (success) {
                        Toast.makeText(requireContext(), "Category added successfully", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        // Refresh the UI to display the new category
                        refreshRecipiesDisplay()
                    } else {
                        Toast.makeText(requireContext(), "Failed to add category. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Log.d("SavedCategoriesFragment", "Save button clicked but category name is empty.")
                Toast.makeText(requireContext(), "Category name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun refreshRecipiesDisplay() {
        cardContainer.removeAllViews()
        setupRecipies()
    }

    private fun saveRecipeToRepository(recipeName: String, categoryId: String) {
        val recipe = Recipe(
            id = "", // Let the repository generate an ID if needed
            title = recipeName,
            instructions = "",
            ingredientsWithQuantities = emptyList(),
            imageBitmap = selectedPhotoBitmap
        )

        RecipesRepository.addNewRecipe(requireContext(), categoryId, recipe, selectedPhotoBitmap) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Recipe added successfully!", Toast.LENGTH_SHORT).show()
                createRecipeCard(recipe)
            } else {
                Toast.makeText(requireContext(), "Error adding recipe", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(requireContext().contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}
