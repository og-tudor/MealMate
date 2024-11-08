package com.yourpackage.name

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.mealmate.R
import com.example.mealmate.SavedRecipesFragment
import com.example.mealmate.dashboard.GeneralFunctions
import java.io.Serializable

// Create a data class for ingredients with serialization support
data class Ingredient(val name: String, val quantity: String) : Serializable

class RecipeFragment : Fragment() {
    private lateinit var cardContainer: GridLayout
    private lateinit var homeButton: ImageButton
    private lateinit var discoverButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private lateinit var generalFunctions: GeneralFunctions

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the correct layout for this fragment
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

        // Retrieve the arguments
        val recipeName = arguments?.getString("recipeName")
        val imageUri = arguments?.getString("imageUri")
        val ingredients = arguments?.getSerializable("ingredients") as? Array<Ingredient>
        val instructions = arguments?.getString("instructions")

        // Find views in the layout
        val recipeNameTextView = view.findViewById<TextView>(R.id.recipe_name)
        val recipeImageView = view.findViewById<ImageView>(R.id.recipe_image)
        val ingredientsSection = view.findViewById<LinearLayout>(R.id.ingredient_section)
        val instructionsSection = view.findViewById<LinearLayout>(R.id.instructions_section)

        // Set the recipe name
        recipeNameTextView.text = recipeName ?: "No Title"

        // Load the image using Glide for better performance and compatibility
        if (!imageUri.isNullOrEmpty()) {
            Glide.with(this)
                .load(Uri.parse(imageUri))
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

        // Set up the return button
        val returnButton: TextView = view.findViewById(R.id.return_button)
        returnButton.visibility = View.VISIBLE
        returnButton.text = "< Recipes"
        returnButton.setOnClickListener {
            generalFunctions.navigateToFragment(SavedRecipesFragment(), hideView = returnButton)
        }

        return view
    }

    companion object {
        fun newInstance(
            recipeName: String,
            imageUri: String,
            ingredients: Array<Ingredient>?,
            instructions: String?
        ): RecipeFragment {
            val fragment = RecipeFragment()
            val args = Bundle().apply {
                putString("recipeName", recipeName)
                putString("imageUri", imageUri)
                putSerializable("ingredients", ingredients ?: emptyArray<Ingredient>()) // Use an empty array if ingredients is null
                putString("instructions", instructions)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
