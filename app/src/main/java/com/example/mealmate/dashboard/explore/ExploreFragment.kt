package com.example.mealmate.dashboard.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
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
import com.example.mealmate.dashboard.GeneralFunctions
import com.example.mealmate.model.Meal
import retrofit2.HttpException

class ExploreFragment : Fragment() {
    private var MAXIMUM_CARDS = 1
    private lateinit var cardContainer: LinearLayout
    private lateinit var lottieAnimationView: LottieAnimationView
    private lateinit var homeButton: ImageButton
    private lateinit var discoverButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private lateinit var searchBarContainer: LinearLayout
    private lateinit var searchInput: EditText
    private lateinit var searchIcon: ImageView
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
        searchBarContainer = view.findViewById(R.id.search_bar_container)
        searchInput = view.findViewById(R.id.search_input)
        searchIcon = view.findViewById(R.id.search_icon_button)
        discoverButton.isSelected = true

        // Set up search bar with click listener for the search icon
        val generalFunctions = GeneralFunctions(requireActivity(), homeButton, discoverButton, settingsButton)
        generalFunctions.setupSearchBar(searchBarContainer, searchInput, searchIcon) { query ->
            makeSearchApiCall(query)
        }

        // Set up button listeners using GeneralFunctions
        val buttonFunctions = GeneralFunctions(requireActivity(), homeButton, discoverButton, settingsButton)
        homeButton.setOnClickListener { buttonFunctions.selectButton(homeButton) }
        discoverButton.setOnClickListener { buttonFunctions.selectButton(discoverButton) }
        settingsButton.setOnClickListener { buttonFunctions.selectButton(settingsButton) }

        // Show the Lottie animation and make initial API calls
        lottieAnimationView.visibility = View.VISIBLE
        lottieAnimationView.playAnimation()

        for (i in 0 until MAXIMUM_CARDS) {
            fetchRandomMeal()
        }

        return view
    }

    private fun makeSearchApiCall(query: String) {
        // Capitalize the first letter of the search query
        val formattedQuery = query.trim().replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }

        lottieAnimationView.visibility = View.VISIBLE
        lottieAnimationView.playAnimation()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.searchMeals(formattedQuery)
                response.meals?.let { meals ->
                    cardContainer.removeAllViews() // Clear existing cards before showing new results
                    for (meal in meals) {
                        // Extract ingredients and measures
                        val ingredients = mutableListOf<String>()
                        val measures = mutableListOf<String>()

                        for (i in 1..20) {
                            val ingredientField = meal::class.java.getDeclaredField("strIngredient$i").get(meal) as? String
                            val measureField = meal::class.java.getDeclaredField("strMeasure$i").get(meal) as? String

                            if (!ingredientField.isNullOrBlank()) {
                                ingredients.add(ingredientField)
                            }
                            if (!measureField.isNullOrBlank()) {
                                measures.add(measureField)
                            }
                        }

                        // Create a combined list of pairs of measures and ingredients
                        val ingredientsWithQuantities = ingredients.zip(measures)

                        val recipe = Recipe(
                            id = meal.idMeal,
                            title = meal.strMeal,
                            imageUrl = meal.strMealThumb,
                            instructions = meal.strInstructions,
                            ingredientsWithQuantities = ingredientsWithQuantities
                        )

                        addCardToContainer(recipe)
                    }
                } ?: run {
                    Toast.makeText(requireContext(), "No results found for \"$formattedQuery\"", Toast.LENGTH_SHORT).show()
                }
                lottieAnimationView.visibility = View.GONE
                lottieAnimationView.cancelAnimation()
            } catch (e: HttpException) {
                Log.e("ExploreFragment", "API call failed: ${e.message}")
                Toast.makeText(requireContext(), "Error fetching search results", Toast.LENGTH_SHORT).show()
                lottieAnimationView.visibility = View.GONE
                lottieAnimationView.cancelAnimation()
            } catch (e: Exception) {
                Log.e("ExploreFragment", "Error: ${e.message}")
                Toast.makeText(requireContext(), "An unexpected error occurred", Toast.LENGTH_SHORT).show()
                lottieAnimationView.visibility = View.GONE
                lottieAnimationView.cancelAnimation()
            }
        }
    }


    private fun fetchRandomMeal() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getRandomMeal()
                val mealData = response.meals.firstOrNull()
                mealData?.let { meal ->
                    val recipe = Recipe(
                        id = meal.idMeal,
                        title = meal.strMeal,
                        imageUrl = meal.strMealThumb,
                        instructions = meal.strInstructions,
                        ingredientsWithQuantities = meal.getIngredientsWithMeasures()
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
