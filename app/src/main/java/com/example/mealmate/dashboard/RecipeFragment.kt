package com.yourpackage.name

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mealmate.R
import com.example.mealmate.SavedRecipesFragment

class RecipeFragment : Fragment() {
    private lateinit var cardContainer: GridLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the correct layout for this fragment
        val view = inflater.inflate(R.layout.recipe_page, container, false)

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

        // Set an OnClickListener to handle the button click and navigate back
        returnButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SavedRecipesFragment())
                .addToBackStack(null)
                .commit()

            returnButton.visibility = View.INVISIBLE
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
