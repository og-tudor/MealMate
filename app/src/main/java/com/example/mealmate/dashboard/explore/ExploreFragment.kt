package com.example.mealmate.dashboard.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.example.mealmate.R
import com.example.mealmate.model.Recipe
import com.example.mealmate.network.RetrofitInstance
import kotlinx.coroutines.launch
import android.widget.LinearLayout
import com.example.mealmate.dashboard.GeneralFunctions

class ExploreFragment : Fragment() {
    private var MAXIMUM_CARDS = 20
    private lateinit var cardContainer: LinearLayout
    private lateinit var lottieAnimationView: LottieAnimationView
    private lateinit var homeButton: ImageButton
    private lateinit var discoverButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private var cardsLoaded = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.explore_page, container, false)

        // Initialize UI elements
        homeButton = view.findViewById(R.id.home_button)
        discoverButton = view.findViewById(R.id.discover_button)
        settingsButton = view.findViewById(R.id.settings_button)
        cardContainer = view.findViewById(R.id.card_container)
        lottieAnimationView = view.findViewById(R.id.lottie_animation_view)
        discoverButton.isSelected = true

        val generalFunctions = GeneralFunctions(requireActivity(), homeButton, discoverButton, settingsButton)
        homeButton.setOnClickListener { generalFunctions.selectButton(homeButton) }
        discoverButton.setOnClickListener { generalFunctions.selectButton(discoverButton) }
        settingsButton.setOnClickListener { generalFunctions.selectButton(settingsButton) }

        // Show the Lottie animation and make API calls
        lottieAnimationView.visibility = View.VISIBLE
        lottieAnimationView.playAnimation()

        for (i in 0 until MAXIMUM_CARDS) {
            fetchRandomMeal()
        }

        return view
    }

    private fun fetchRandomMeal() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getRandomMeal()
                val meal = response.meals.firstOrNull()
                meal?.let {
                    val recipe = Recipe(
                        id = it.idMeal,
                        title = it.strMeal,
                        imageUrl = it.strMealThumb
                    )
                    addCardToContainer(recipe)
                }
                cardsLoaded++
                if (cardsLoaded == MAXIMUM_CARDS) {
                    lottieAnimationView.visibility = View.GONE
                    lottieAnimationView.cancelAnimation()
                }
            } catch (e: Exception) {
                Log.e("ExploreFragment", "API call failed: ${e.message}")
                Toast.makeText(requireContext(), "Error fetching data", Toast.LENGTH_SHORT).show()
                cardsLoaded++
                if (cardsLoaded == MAXIMUM_CARDS) {
                    lottieAnimationView.visibility = View.GONE
                    lottieAnimationView.cancelAnimation()
                }
            }
        }
    }

    private fun addCardToContainer(recipe: Recipe) {
        // Inflate the card layout
        val cardView = layoutInflater.inflate(R.layout.categories_default_card, cardContainer, false)

        // Set the recipe title
        val titleTextView = cardView.findViewById<TextView>(R.id.item_title)
        titleTextView.text = recipe.title

        // Set the recipe image using Glide
        val imageView = cardView.findViewById<ImageView>(R.id.item_image)
        Glide.with(this)
            .load(recipe.imageUrl)
            .centerCrop()
            .into(imageView)

        // Add the card to the LinearLayout
        cardContainer.addView(cardView)
    }
}
