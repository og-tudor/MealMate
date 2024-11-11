package com.yourpackage.name

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.mealmate.R
import com.example.mealmate.SavedRecipesFragment
import com.example.mealmate.dashboard.GeneralFunctions
import com.example.mealmate.dashboard.home.CategoriesViewModel
import com.example.mealmate.dashboard.home.Category
import com.example.mealmate.dashboard.home.ExploreFragment
import com.example.mealmate.utils.FragmentSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.Serializable

// Create a data class for ingredients with serialization support
data class Ingredient(val name: String, val quantity: String) : Serializable

class RecipeFragment : Fragment() {
    private lateinit var cardContainer: GridLayout
    private lateinit var homeButton: ImageButton
    private lateinit var discoverButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private lateinit var generalFunctions: GeneralFunctions
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var categoriesViewModel: CategoriesViewModel

    private lateinit var recipeName: String
    private var imageUri: Uri? = null
    private var ingredients: Array<Ingredient> = emptyArray()
    private lateinit var instructions: String


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the correct layout for this fragment
        val view = inflater.inflate(R.layout.recipe_page, container, false)
        categoriesViewModel = ViewModelProvider(requireActivity()).get(CategoriesViewModel::class.java)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize UI elements
        homeButton = view.findViewById(R.id.home_button)
        discoverButton = view.findViewById(R.id.discover_button)
        settingsButton = view.findViewById(R.id.settings_button)
        cardContainer = view.findViewById(R.id.card_container)

        generalFunctions = GeneralFunctions(requireActivity(), homeButton, discoverButton, settingsButton)
        homeButton.setOnClickListener { generalFunctions.selectButton(homeButton) }
        discoverButton.setOnClickListener { generalFunctions.selectButton(discoverButton) }
        settingsButton.setOnClickListener { generalFunctions.selectButton(settingsButton) }
        val sourceString = arguments?.getString("SOURCE")
        val source = sourceString?.let { FragmentSource.valueOf(it) } ?: FragmentSource.UNKNOWN_PAGE // Default if not found

        // Reference the included layout and find the Button inside it
        val saveRecipeButtonLayout: View = view.findViewById(R.id.saveRecipeButtonLayout)
        val saveRecipeButton: Button = saveRecipeButtonLayout.findViewById(R.id.saveRecipeButton)

        // Set the initial visibility of the layout
        saveRecipeButtonLayout.visibility = View.GONE
        saveRecipeButton.setOnClickListener {
            showSaveToCategoryDialog()
        }

        // Set up the return button
        val returnButton: TextView = view.findViewById(R.id.return_button)
        returnButton.visibility = View.VISIBLE

        var ReturnFragment: Fragment? = null

        when (source) {
            FragmentSource.EXPLORE_PAGE -> {
                discoverButton.isSelected = true
                returnButton.text = "< Discover"
                ReturnFragment = ExploreFragment()
                // only show button in explore page
                saveRecipeButtonLayout.visibility = View.VISIBLE
            }
            FragmentSource.SAVED_RECIPIES_LIBRARY -> {
                homeButton.isSelected = true
                returnButton.text = "< Recipies"
                ReturnFragment = SavedRecipesFragment()
            }

            else -> {
                // Toast error
                returnButton.visibility = View.INVISIBLE
            }
        }

        returnButton.setOnClickListener {
            ReturnFragment?.let {
                // Only navigate if 'ReturnFragment' is not null
                generalFunctions.navigateToFragment(it, hideView = returnButton)
            }
        }


        // Retrieve the arguments
        recipeName = arguments?.getString("recipeName") ?: "No Title"
        imageUri = arguments?.getString("imageUri")?.let { Uri.parse(it) }
        ingredients = arguments?.getSerializable("ingredients") as? Array<Ingredient> ?: emptyArray()
        instructions = arguments?.getString("instructions") ?: "No instructions available"

        // Find views in the layout
        val recipeNameTextView = view.findViewById<TextView>(R.id.recipe_name)
        val recipeImageView = view.findViewById<ImageView>(R.id.recipe_image)
        val ingredientsSection = view.findViewById<LinearLayout>(R.id.ingredient_section)
        val instructionsSection = view.findViewById<LinearLayout>(R.id.instructions_section)

        // Set the recipe name
        recipeNameTextView.text = recipeName ?: "No Title"

        // Load the image using Glide for better performance and compatibility
        if (!imageUri.toString().isNullOrEmpty()) {
            Glide.with(this)
                .load(Uri.parse(imageUri.toString()))
                .placeholder(R.drawable.placeholder)
                .into(recipeImageView)
        } else {
            recipeImageView.setImageResource(R.drawable.placeholder)
        }

        // Display ingredients
        if (ingredients != null && ingredients.isNotEmpty()) {
            ingredients.forEach { ingredient ->
                val ingredientRow = inflater.inflate(R.layout.ingredient_row, ingredientsSection, false)
                val ingredientNameTextView = ingredientRow.findViewById<TextView>(R.id.ingredient_name)
                val ingredientQuantityTextView = ingredientRow.findViewById<TextView>(R.id.ingredient_quantity)

                // Set the text for the ingredient row
                ingredientNameTextView.text = ingredient.name
                ingredientQuantityTextView.text = ingredient.quantity

                // Add the inflated ingredient row to the ingredient section
                ingredientsSection.addView(ingredientRow)
            }
        } else {
            // Display a message if no ingredients are available
            val noIngredientsTextView = TextView(context).apply {
                text = "No ingredients available"
                setPadding(16, 16, 16, 16)
            }
            ingredientsSection.addView(noIngredientsTextView)
        }

        // Display instructions
        instructionsSection.removeAllViews()
        if (!instructions.isNullOrEmpty()) {
            val instructionTextView = TextView(context).apply {
                text = instructions
                setPadding(16, 16, 16, 16)
            }
            instructionsSection.addView(instructionTextView)
        } else {
            val noInstructionsTextView = TextView(context).apply {
                text = "No instructions available"
                setPadding(16, 16, 16, 16)
            }
            instructionsSection.addView(noInstructionsTextView)
        }

        return view
    }

    private fun showSaveToCategoryDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.modal_save_recipe)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val windowAttributes = dialog.window?.attributes
        windowAttributes?.gravity = Gravity.BOTTOM
        dialog.window?.attributes = windowAttributes

        val listContainer = dialog.findViewById<LinearLayout>(R.id.listContainer)
        val closeButton = dialog.findViewById<ImageButton>(R.id.closeButton)
        closeButton.setOnClickListener { dialog.dismiss() }

        // Load and display cached categories
        if (categoriesViewModel.cachedCategories.isEmpty()) {
            loadCategoriesFromFirestore(dialog) // Pass the dialog to the method
        } else {
            displayCategoriesInDialog(listContainer, dialog)
        }

        dialog.show()
    }

    private fun saveRecipeToFirestore(recipeName: String, imageUri: Uri?, ingredients: Array<Ingredient>, instructions: String, categoryId: String? = null) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid
        val categoryId = categoryId
        if (categoryId == null) {
            Toast.makeText(requireContext(), "Category ID is missing", Toast.LENGTH_SHORT).show()
            return
        }

        val recipesRef = db.collection("users").document(userId)
            .collection("categories").document(categoryId)
            .collection("recipes")

        // hashmap, key = ingredient name, value = ingredient quantity
        var ingredientsList = ingredients.map { it.name to it.quantity }.toMap()



        val recipeData = hashMapOf(
            "name" to recipeName,
            "imageUri" to (imageUri?.toString() ?: ""),
            "ingredients" to ingredientsList,
            "instructions" to instructions
        )

        recipesRef.add(recipeData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Recipe added successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error adding recipe: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }



    private fun displayCategoriesInDialog(listContainer: LinearLayout, dialog: Dialog? = null) {
        listContainer.removeAllViews()
        for (category in categoriesViewModel.cachedCategories) {
            val itemView = layoutInflater.inflate(R.layout.category_dialog_item, listContainer, false)
            val categoryNameTextView = itemView.findViewById<TextView>(R.id.categoryName)
            val categoryImageView = itemView.findViewById<ImageView>(R.id.categoryIcon)

            categoryNameTextView.text = category.name
            if (category.imageUri != null) {
                categoryImageView.setImageURI(category.imageUri)
            } else {
                categoryImageView.setImageResource(R.drawable.placeholder)
            }

            // Set click listener for the category item
            // Set click listener for the category item
            itemView.setOnClickListener {
                saveRecipeToFirestore(
                    recipeName = recipeName,
                    imageUri = imageUri,
                    ingredients = ingredients,
                    instructions = instructions,
                    categoryId = category.id
                )
                dialog?.dismiss()
            }
            // Add the item view to the container
            listContainer.addView(itemView)
        }
    }


    // Load categories from Firestore
    private fun loadCategoriesFromFirestore(dialog: Dialog) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid
        val categoriesRef = db.collection("users").document(userId).collection("categories")

        categoriesRef.orderBy("name").get()
            .addOnSuccessListener { documents ->
                categoriesViewModel.cachedCategories.clear()
                for (document in documents) {
                    val categoryName = document.getString("name") ?: ""
                    val filePath = document.getString("imageUri") ?: ""
                    val imageUri = if (filePath.isNotEmpty()) Uri.fromFile(File(filePath)) else null
                    val categoryId = document.id

                    val category = Category(categoryName, imageUri, categoryId)
                    categoriesViewModel.cachedCategories.add(category)
                }
                displayCategoriesInDialog(dialog.findViewById(R.id.listContainer))
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error fetching categories: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    companion object {
        fun newInstance(
            recipeName: String,
            imageUri: String,
            ingredients: Array<Ingredient>?,
            instructions: String?,
            source: FragmentSource
        ): RecipeFragment {
            val fragment = RecipeFragment()
            val args = Bundle().apply {
                putString("recipeName", recipeName)
                putString("imageUri", imageUri)
                putSerializable("ingredients", ingredients ?: emptyArray<Ingredient>()) // Use an empty array if ingredients is null
                putString("instructions", instructions)
                putString("SOURCE", source.name)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
