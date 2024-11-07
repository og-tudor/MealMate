package com.yourpackage.name

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mealmate.R
import com.example.mealmate.SavedRecipesFragment
import com.example.mealmate.dashboard.GeneralFunctions

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
        homeButton.isSelected = true

        generalFunctions = GeneralFunctions(requireActivity(), homeButton, discoverButton, settingsButton)
        // Set up click listeners for each button
        homeButton.setOnClickListener { generalFunctions.selectButton(homeButton) }
        discoverButton.setOnClickListener { generalFunctions.selectButton(discoverButton) }
        settingsButton.setOnClickListener { generalFunctions.selectButton(settingsButton) }


        // Retrieve the arguments
        val recipeName = arguments?.getString("recipeName")
        val imageUri = arguments?.getString("imageUri")

        // Find views in the layout
        val recipeNameTextView = view.findViewById<TextView>(R.id.recipe_name)
        val recipeImageView = view.findViewById<ImageView>(R.id.recipe_image)

        // Check if views are not null before using them
        if (recipeNameTextView != null && recipeImageView != null) {
            // Set the recipe name
            recipeNameTextView.text = recipeName

            // Load the image if the URI is provided
            if (!imageUri.isNullOrEmpty()) {
                recipeImageView.setImageURI(Uri.parse(imageUri))
            } else {
                // Optionally, set a placeholder image if imageUri is empty or null
                recipeImageView.setImageResource(R.drawable.placeholder)
            }
        }
        val returnButton: TextView = view.findViewById(R.id.return_button)
        cardContainer = view.findViewById(R.id.card_container)
        returnButton.visibility = View.VISIBLE
        returnButton.text = "< Recipies"

        val generalFunctions = GeneralFunctions(requireActivity(), homeButton, discoverButton, settingsButton)

        returnButton.setOnClickListener {
            generalFunctions.navigateToFragment(SavedRecipesFragment(), hideView = returnButton)
        }


        return view
    }

    companion object {
        fun newInstance(recipeName: String, imageUri: String): RecipeFragment {
            val fragment = RecipeFragment()
            val args = Bundle()
            args.putString("recipeName", recipeName)
            args.putString("imageUri", imageUri)
            fragment.arguments = args
            return fragment
        }
    }
}
