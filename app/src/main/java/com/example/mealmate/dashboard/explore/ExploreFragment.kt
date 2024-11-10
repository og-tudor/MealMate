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
import com.example.mealmate.utils.AnimationHandler
import com.example.mealmate.utils.FragmentSource
import com.yourpackage.name.Ingredient
import com.yourpackage.name.RecipeFragment
import retrofit2.HttpException

class ExploreFragment : Fragment() {
    // Number of recipes to fetch for the random
    private var MAXIMUM_CARDS = 20

    private var CUP_TO_GRAMS = 250
    private var TSP_TO_GRAMS = 6 // Round up value
    private var TBSP_TO_GRAMS = 14 // Round down value
    private var OUNCES_TO_GRAMS = 28

    private lateinit var cardContainer: LinearLayout
    private lateinit var lottieAnimationView: LottieAnimationView
    private lateinit var animationHandler: AnimationHandler
    private lateinit var homeButton: ImageButton
    private lateinit var discoverButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private lateinit var searchBarContainer: LinearLayout
    private lateinit var searchInput: EditText
    private lateinit var searchIcon: ImageView
    private lateinit var generalFunctions: GeneralFunctions
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
        animationHandler = AnimationHandler(lottieAnimationView)
        searchBarContainer = view.findViewById(R.id.search_bar_container)
        searchInput = view.findViewById(R.id.search_input)
        searchIcon = view.findViewById(R.id.search_icon_button)
        discoverButton.isSelected = true

        // Set up search bar with click listener for the search icon
        generalFunctions = GeneralFunctions(requireActivity(), homeButton, discoverButton, settingsButton)
        generalFunctions.setupSearchBar(searchBarContainer, searchInput, searchIcon) { query ->
            makeSearchApiCall(query)
        }

        // Set up button listeners using GeneralFunctions
        val buttonFunctions = GeneralFunctions(requireActivity(), homeButton, discoverButton, settingsButton)
        homeButton.setOnClickListener { buttonFunctions.selectButton(homeButton) }
        discoverButton.setOnClickListener { buttonFunctions.selectButton(discoverButton) }
        settingsButton.setOnClickListener { buttonFunctions.selectButton(settingsButton) }

        // Show the animation and make initial API calls
        animationHandler.showAnimation()
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

        animationHandler.showAnimation()

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
                            // Get the ingredient field and set it accessible
                            val ingredientField = meal::class.java.getDeclaredField("strIngredient$i")
                            ingredientField.isAccessible = true
                            val ingredientValue = ingredientField.get(meal) as? String

                            // Get the measure field and set it accessible
                            val measureField = meal::class.java.getDeclaredField("strMeasure$i")
                            measureField.isAccessible = true
                            val measureValue = measureField.get(meal) as? String

                            if (!ingredientValue.isNullOrBlank()) {
                                ingredients.add(ingredientValue)
                            }
                            if (!measureValue.isNullOrBlank()) {
                                // Use the conversion function
                                val convertedMeasure = convertMeasureToGrams(measureValue)
                                measures.add(convertedMeasure)
                            }
                        }

                        // Create a combined list of pairs of ingredients and measures
                        val ingredientsWithQuantities = measures.zip(ingredients)

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
                animationHandler.hideAnimation()
            } catch (e: HttpException) {
                Log.e("ExploreFragment", "API call failed: ${e.message}")
                Toast.makeText(requireContext(), "Error fetching search results", Toast.LENGTH_SHORT).show()
                animationHandler.hideAnimation()
            } catch (e: Exception) {
                Log.e("ExploreFragment", "Error: ${e.message}")
                Toast.makeText(requireContext(), "An unexpected error occurred", Toast.LENGTH_SHORT).show()
                animationHandler.hideAnimation()
            }
        }
    }

    private fun fetchRandomMeal() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getRandomMeal()
                val mealData = response.meals.firstOrNull()
                mealData?.let { meal ->
                    // Use the conversion function for each measure in the ingredients list
                    val convertedIngredientsWithMeasures = meal.getIngredientsWithMeasures().map { (measure, ingredient) ->
                        convertMeasureToGrams(measure) to ingredient
                    }

                    val recipe = Recipe(
                        id = meal.idMeal,
                        title = meal.strMeal,
                        imageUrl = meal.strMealThumb,
                        instructions = meal.strInstructions,
                        ingredientsWithQuantities = convertedIngredientsWithMeasures
                    )

                    addCardToContainer(recipe)
                }
                cardsLoaded++
                if (cardsLoaded == MAXIMUM_CARDS) {
                    animationHandler.hideAnimation()
                }
            } catch (e: Exception) {
                Log.e("ExploreFragment", "API call failed: ${e.message}")
                Toast.makeText(requireContext(), "Error fetching data", Toast.LENGTH_SHORT).show()
                cardsLoaded++
                if (cardsLoaded == MAXIMUM_CARDS) {
                    animationHandler.hideAnimation()
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

        // Set a click listener to open RecipeFragment with the recipe data
        cardView.setOnClickListener {
            // Prepare the ingredients array using the Ingredient data class
            val ingredientsArray = recipe.ingredientsWithQuantities.map {
                Ingredient(
                    name = it.second,  // Ingredient name
                    quantity = it.first // Measure
                )
            }.toTypedArray()

            // Create an instance of RecipeFragment with the recipe data
            val fragment = RecipeFragment.newInstance(
                recipeName = recipe.title.ifEmpty { "No Title" },
                imageUri = recipe.imageUrl.ifEmpty { "" },
                ingredients = ingredientsArray,
                instructions = recipe.instructions.ifEmpty { "No instructions available" },
                source = FragmentSource.EXPLORE_PAGE
            )

            // Use GeneralFunctions to handle the navigation
            generalFunctions.navigateToFragment(fragment)
        }

        // Add the card to the LinearLayout
        cardContainer.addView(cardView)
    }

    private fun convertMeasureToGrams(measure: String): String {
        var convertedMeasure = measure
        val numericValue = extractNumericValue(measure)

        when {
            measure.contains(Regex("tsp|teaspoon", RegexOption.IGNORE_CASE)) -> {
                if (numericValue != null) {
                    val gramsValue = numericValue * TSP_TO_GRAMS
                    convertedMeasure = "${gramsValue.toInt()} grams"
                } else {
                    convertedMeasure = measure.replace(Regex("tsp|teaspoon", RegexOption.IGNORE_CASE), "grams")
                }
            }
            measure.contains(Regex("tbsp|tbs|tbls|tblsp|tablespoons", RegexOption.IGNORE_CASE)) -> {
                if (numericValue != null) {
                    val gramsValue = numericValue * TBSP_TO_GRAMS
                    convertedMeasure = "${gramsValue.toInt()} grams"
                } else {
                    convertedMeasure = measure.replace(Regex("tbsp|tbs|tbls|tblsp|tablespoons", RegexOption.IGNORE_CASE), "grams")
                }
            }
            measure.contains(Regex("oz|ounce|ounces", RegexOption.IGNORE_CASE)) -> {
                if (numericValue != null) {
                    val gramsValue = numericValue * OUNCES_TO_GRAMS
                    convertedMeasure = "${gramsValue.toInt()} grams"
                } else {
                    convertedMeasure = measure.replace(Regex("oz|ounce|ounces", RegexOption.IGNORE_CASE), "grams")
                }
            }
            measure.contains(Regex("cup|cups", RegexOption.IGNORE_CASE)) -> {
                if (numericValue != null) {
                    val gramsValue = numericValue * CUP_TO_GRAMS
                    convertedMeasure = "${gramsValue.toInt()} grams"
                } else {
                    convertedMeasure = measure.replace(Regex("cup|cups", RegexOption.IGNORE_CASE), "grams")
                }
            }
            else -> {
                // Leave the measure unchanged for other cases
            }
        }
        return convertedMeasure
    }

    // Helper function to extract numeric value, including fractions (e.g., "1/2")
    private fun extractNumericValue(measure: String): Double? {
        return if (measure.contains('/')) {
            // Handle fractional values
            measure.split(' ').mapNotNull { part ->
                if (part.contains('/')) {
                    val fractionParts = part.split('/')
                    if (fractionParts.size == 2) {
                        val numerator = fractionParts[0].toDoubleOrNull()
                        val denominator = fractionParts[1].toDoubleOrNull()
                        if (numerator != null && denominator != null) {
                            numerator / denominator
                        } else null
                    } else null
                } else {
                    part.toDoubleOrNull()
                }
            }.sum()
        } else {
            // Handle normal numeric values
            measure.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
        }
    }
}
