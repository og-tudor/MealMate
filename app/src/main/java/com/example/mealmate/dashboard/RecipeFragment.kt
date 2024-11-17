package com.yourpackage.name

import Ingredient
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.mealmate.R
import com.example.mealmate.SavedRecipesFragment
import com.example.mealmate.dashboard.GeneralFunctions
import com.example.mealmate.dashboard.home.ExploreFragment
import com.example.mealmate.model.Recipe
import com.example.mealmate.repository.CategoriesRepository
import com.example.mealmate.repository.RecipesRepository
import com.example.mealmate.utils.FragmentSource


class RecipeFragment : Fragment() {
    private lateinit var cardContainer: GridLayout
    private lateinit var homeButton: ImageButton
    private lateinit var discoverButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private lateinit var generalFunctions: GeneralFunctions

    private lateinit var recipeName: String
    private var imageUri: Uri? = null
    private var ingredients: Array<Ingredient> = emptyArray()
    private lateinit var instructions: String
    private var imageBitmap: Bitmap? = null
    private lateinit var linstructionEditText: EditText


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.recipe_page, container, false)

        // Initialize UI elements
        homeButton = view.findViewById(R.id.home_button)
        discoverButton = view.findViewById(R.id.discover_button)
        settingsButton = view.findViewById(R.id.settings_button)
        cardContainer = view.findViewById(R.id.card_container)

        generalFunctions = GeneralFunctions(requireActivity(), homeButton, discoverButton, settingsButton)
        homeButton.setOnClickListener { generalFunctions.selectButton(homeButton) }
        discoverButton.setOnClickListener { generalFunctions.selectButton(discoverButton) }
        settingsButton.setOnClickListener { generalFunctions.selectButton(settingsButton) }
        imageBitmap = arguments?.getParcelable("imageBitmap")

        setupButtons(view)
        setupSaveRecipeButton(view)
        setupEditButtonListeners(view)

        // Retrieve the arguments
        recipeName = arguments?.getString("recipeName") ?: "No Title"
        imageUri = arguments?.getString("imageUri")?.let { Uri.parse(it) }
        ingredients = arguments?.getParcelableArray("ingredients")?.filterIsInstance<Ingredient>()?.toTypedArray() ?: emptyArray()
        instructions = arguments?.getString("instructions") ?: "No instructions available"

        displayRecipeDetails(view, inflater)

        return view
    }

    private fun setupEditButtonListeners(view: View) {
        val editButton: ImageView = view.findViewById(R.id.edit_recipe_button)
        val saveEditButton: ImageView = view.findViewById(R.id.save_edit_recipe_button)
        val ingredientAddNewRow: View = view.findViewById(R.id.addNewIngredientRow)

        val editInstructionsButton: ImageView = view.findViewById(R.id.icon_edit_instructions)

        saveEditButton.visibility = View.GONE
        ingredientAddNewRow.visibility = View.GONE
        editInstructionsButton.visibility = View.GONE

        // Edit button click listener
        editButton.setOnClickListener {
            editButton.visibility = View.GONE
            saveEditButton.visibility = View.VISIBLE
            ingredientAddNewRow.visibility = View.VISIBLE
            editInstructionsButton.visibility = View.VISIBLE
        }

        // Save button click listener
        saveEditButton.setOnClickListener {
            saveEditButton.visibility = View.GONE
            ingredientAddNewRow.visibility = View.GONE
            editInstructionsButton.visibility = View.GONE
            editButton.visibility = View.VISIBLE
            saveInstructions()
        }

        // Ingredient add new row click listener
        ingredientAddNewRow.setOnClickListener {
            showAddIngredientDialog()
        }

        // Enable editing instructions on clicking the edit icon
        editInstructionsButton.setOnClickListener {
            val instructionsSection = view.findViewById<LinearLayout>(R.id.instructions_section)
            for (i in 0 until instructionsSection.childCount) {
                val child = instructionsSection.getChildAt(i)
                if (child is EditText) {
                    child.isFocusableInTouchMode = true
                    child.isCursorVisible = true
                    child.isEnabled = true
                    child.requestFocus()
                }
            }
        }
    }

    private fun saveInstructions() {
        val instructionsSection = view?.findViewById<LinearLayout>(R.id.instructions_section)
        val updatedInstructions = mutableListOf<String>()

        instructionsSection?.let {
            for (i in 0 until it.childCount) {
                val child = it.getChildAt(i)
                if (child is EditText) {
                    updatedInstructions.add(child.text.toString())
                    // Disable editing after saving
                    child.isFocusable = false
                    child.isFocusableInTouchMode = false
                    child.isCursorVisible = false
                    child.isEnabled = false
                }
            }
        }

        // Update the instructions variable with the new instructions
        instructions = updatedInstructions.joinToString("\n")

        // Optionally, update the repository if needed
        val categoryId = arguments?.getString("categoryID")
        val recipeId = arguments?.getString("recipeID")
        if (categoryId != null && recipeId != null) {
            RecipesRepository.updateRecipeInstructions(requireContext(), categoryId, recipeId, instructions) { success ->
                if (success) {
                    Toast.makeText(requireContext(), "Instructions updated successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Failed to update instructions", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun showAddIngredientDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_add_ingredient) // Create a layout resource file for this dialog
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val ingredientNameInput = dialog.findViewById<EditText>(R.id.ingredient_name_input)
        val ingredientQuantityInput = dialog.findViewById<EditText>(R.id.ingredient_quantity_input)
        val addButton = dialog.findViewById<Button>(R.id.add_button)

        addButton.setOnClickListener {
            val name = ingredientNameInput.text.toString().trim()
            val quantity = ingredientQuantityInput.text.toString().trim()

            if (name.isNotEmpty() && quantity.isNotEmpty()) {
                val newIngredient = Ingredient(name, quantity)
                val categoryID = arguments?.getString("categoryID")
                val recipeID = arguments?.getString("recipeID")
                // Add the ingredient to the repository (you might need to adjust this to your repository logic)
                RecipesRepository.addIngredient(requireContext(), categoryID!!, recipeID!!, newIngredient) { success ->
                    if (success) {
                        Toast.makeText(requireContext(), "Ingredient added successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Failed to add ingredient", Toast.LENGTH_SHORT).show()
                    }
                }

                // Optionally, add the new ingredient to the UI immediately
                addIngredientToUI(newIngredient)

                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Please enter both name and quantity", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun addIngredientToUI(ingredient: Ingredient) {
        val ingredientsSection = view?.findViewById<LinearLayout>(R.id.ingredient_section)
        val inflater = LayoutInflater.from(context)

        val ingredientRow = inflater.inflate(R.layout.ingredient_row, ingredientsSection, false)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        ingredientRow.layoutParams = params
        val ingredientNameTextView = ingredientRow.findViewById<TextView>(R.id.ingredient_name)
        val ingredientQuantityTextView = ingredientRow.findViewById<TextView>(R.id.ingredient_quantity)

        ingredientNameTextView.text = ingredient.name
        ingredientQuantityTextView.text = ingredient.quantity

        ingredientsSection?.addView(ingredientRow)
    }

    private fun setupButtons(view: View) {
        val returnButton: TextView = view.findViewById(R.id.return_button)
        returnButton.visibility = View.VISIBLE

        val editButton: ImageView = view.findViewById(R.id.edit_recipe_button)
        editButton.visibility = View.GONE

        val sourceString = arguments?.getString("SOURCE")
        val source = sourceString?.let { FragmentSource.valueOf(it) } ?: FragmentSource.UNKNOWN_PAGE
        val categoryID = arguments?.getString("categoryID")
        val returnFragment: Fragment? = when (source) {
            FragmentSource.EXPLORE_PAGE -> {
                discoverButton.isSelected = true
                returnButton.text = "< Discover"
                ExploreFragment()
            }
            FragmentSource.SAVED_RECIPIES_LIBRARY -> {
                homeButton.isSelected = true
                editButton.visibility = View.VISIBLE
                returnButton.text = "< Recipes"
                val savedRecipesFragment = SavedRecipesFragment()
                savedRecipesFragment.arguments = Bundle().apply {
                    putString("categoryId", categoryID)
                }
                savedRecipesFragment
            }
            else -> null
        }

        returnButton.setOnClickListener {
            returnFragment?.let {
                generalFunctions.navigateToFragment(it)
            }
        }
    }

    private fun setupSaveRecipeButton(view: View) {
        val saveRecipeButtonLayout: View = view.findViewById(R.id.saveRecipeButtonLayout)
        val saveRecipeButton: Button = saveRecipeButtonLayout.findViewById(R.id.saveRecipeButton)

        saveRecipeButtonLayout.visibility = if (arguments?.getString("SOURCE") == FragmentSource.EXPLORE_PAGE.name) {
            View.VISIBLE
        } else {
            View.GONE
        }

        saveRecipeButton.setOnClickListener {
            showSaveToCategoryDialog()
        }
    }

    private fun displayRecipeDetails(view: View, inflater: LayoutInflater) {
        val recipeNameTextView = view.findViewById<TextView>(R.id.recipe_name)
        val recipeImageView = view.findViewById<ImageView>(R.id.recipe_image)
        val ingredientsSection = view.findViewById<LinearLayout>(R.id.ingredient_section)
        val instructionsSection = view.findViewById<LinearLayout>(R.id.instructions_section)

        recipeNameTextView.text = recipeName

        // Load image based on source
        val source = arguments?.getString("SOURCE")?.let { FragmentSource.valueOf(it) }
        when (source) {
            FragmentSource.EXPLORE_PAGE -> {
                if (imageUri != null) {
                    Glide.with(this)
                        .load(imageUri)
                        .placeholder(R.drawable.placeholder)
                        .into(recipeImageView)
                } else {
                    recipeImageView.setImageResource(R.drawable.placeholder)
                }
            }
            FragmentSource.SAVED_RECIPIES_LIBRARY -> {
                imageBitmap?.let {
                    recipeImageView.setImageBitmap(it)
                } ?: recipeImageView.setImageResource(R.drawable.placeholder)
            }
            else -> recipeImageView.setImageResource(R.drawable.placeholder)
        }

        // Set up ingredients
        ingredientsSection.removeAllViews()
        ingredients.forEach { ingredient ->
            val ingredientRow = inflater.inflate(R.layout.ingredient_row, ingredientsSection, false)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            ingredientRow.layoutParams = params
            val ingredientNameTextView = ingredientRow.findViewById<TextView>(R.id.ingredient_name)
            val ingredientQuantityTextView = ingredientRow.findViewById<TextView>(R.id.ingredient_quantity)

            ingredientNameTextView.text = ingredient.name
            ingredientQuantityTextView.text = ingredient.quantity
            ingredientsSection.addView(ingredientRow)
        }

        // Set up instructions
        // Initialize linstructionEditText within displayRecipeDetails
        linstructionEditText = EditText(requireContext()).apply {
            setText(instructions)
            setPadding(16, 16, 16, 16)
            isFocusable = false
            isFocusableInTouchMode = false
            isCursorVisible = false
            isEnabled = false
            background = null
        }

        // Add linstructionEditText to instructionsSection
        instructionsSection.removeAllViews()
        instructionsSection.addView(linstructionEditText)
    }



    private fun showSaveToCategoryDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.modal_save_recipe)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.attributes?.gravity = Gravity.BOTTOM

        val listContainer = dialog.findViewById<LinearLayout>(R.id.listContainer)
        val closeButton = dialog.findViewById<ImageButton>(R.id.closeButton)
        closeButton.setOnClickListener { dialog.dismiss() }

        if (CategoriesRepository.cachedCategories.isEmpty()) {
            CategoriesRepository.loadCategories(requireContext()) { success ->
                if (success) {
                    displayCategoriesInDialog(listContainer, dialog)
                } else {
                    Toast.makeText(requireContext(), "Failed to load categories", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            displayCategoriesInDialog(listContainer, dialog)
        }

        dialog.show()
    }

    private fun displayCategoriesInDialog(listContainer: LinearLayout, dialog: Dialog? = null) {
        listContainer.removeAllViews()
        for (category in CategoriesRepository.cachedCategories) {
            val itemView = layoutInflater.inflate(R.layout.category_dialog_item, listContainer, false)
            val categoryNameTextView = itemView.findViewById<TextView>(R.id.categoryName)
            val categoryImageView = itemView.findViewById<ImageView>(R.id.categoryIcon)

            categoryNameTextView.text = category.name
            category.photo?.let {
                categoryImageView.setImageBitmap(it)
            } ?: categoryImageView.setImageResource(R.drawable.placeholder)

            itemView.setOnClickListener {
                saveRecipeToFirestore(category.id)
                dialog?.dismiss()
            }

            listContainer.addView(itemView)
        }
    }

    private fun saveRecipeToFirestore(categoryId: String) {
        val recipe = Recipe(
            title = recipeName,
            imageUrl = imageUri?.toString() ?: "", // Provide a default empty string if null
            instructions = instructions,
            ingredientsWithQuantities = ingredients.map { Ingredient(it.name, it.quantity) }
        )


        // Use Glide to fetch the image from the URL and decode it into a Bitmap
        Glide.with(this)
            .asBitmap()
            .load(imageUri) // This should be the URL of the image
            .into(object : com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?) {
                    // Proceed to save recipe to Firestore with the downloaded Bitmap
                    RecipesRepository.addNewRecipe(requireContext(), categoryId, recipe, resource) { success ->
                        if (success) {
                            Toast.makeText(requireContext(), "Recipe added successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Error adding recipe", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Handle if needed when Glide clears the image (e.g., release resources)
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            })
    }

    companion object {
        fun newInstance(
            recipeName: String,
            imageUri: String?,
            imageBitmap: Bitmap?,
            ingredients: Array<Ingredient>?,
            instructions: String?,
            source: FragmentSource,
            categoryID: String?,
            recipeID: String?
        ): RecipeFragment {
            val fragment = RecipeFragment()
            val args = Bundle().apply {
                putString("recipeName", recipeName)
                putString("imageUri", imageUri)
                putParcelable("imageBitmap", imageBitmap)
                putParcelableArray("ingredients", ingredients)
                putString("instructions", instructions)
                putString("SOURCE", source.name)
                putString("categoryID", categoryID)
                putString("recipeID", recipeID)
            }
            fragment.arguments = args
            return fragment
        }
    }

}
