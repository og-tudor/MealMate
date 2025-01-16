package com.example.mealmate

import Ingredient
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieAnimationView
import com.example.mealmate.dashboard.GeneralFunctions
import com.example.mealmate.dashboard.home.SavedCategoriesFragment
import com.example.mealmate.model.Recipe
import com.example.mealmate.repository.RecipesRepository
import com.example.mealmate.utils.AnimationHandler
import com.example.mealmate.utils.FragmentSource
import com.yourpackage.name.RecipeFragment
import java.util.Locale

class SavedRecipesFragment : Fragment() {

    private lateinit var coverPhotoImage: ImageView
    private lateinit var cardContainer: GridLayout
    private lateinit var generalFunctions: GeneralFunctions
    private lateinit var homeButton: ImageButton
    private lateinit var discoverButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private var selectedPhoto: Bitmap? = null
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var categoryId: String
    private lateinit var animationHandler: AnimationHandler
    private lateinit var lottieAnimationView: LottieAnimationView
    private val allRecipeCards = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize gallery launcher early in the lifecycle
        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK && result.data != null) {
                val selectedImageUri = result.data?.data
                selectedImageUri?.let {
                    selectedPhoto = getBitmapFromUri(it)
                    selectedPhoto?.let { bitmap ->
                        coverPhotoImage.setImageBitmap(bitmap)
                        coverPhotoImage.scaleType = ImageView.ScaleType.CENTER_CROP
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
        recipeAddNewCardButton.tag = "new_recipe_card"
        // add it also to the list of all recipe cards
        allRecipeCards.add(recipeAddNewCardButton)

        recipeAddNewCardButton.setOnClickListener { showAddRecipeDialog(categoryId) }

        val searchBar = view.findViewById<EditText>(R.id.search_input)
        val searchIcon = view.findViewById<ImageView>(R.id.search_icon_button)
        searchBar.addTextChangedListener(object : android.text.TextWatcher {
            //

            override fun afterTextChanged(s: android.text.Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim().lowercase(Locale.ROOT)

                // change the color of the search icon based on the query
                searchIcon.setColorFilter(
                    if (query.isEmpty()) {
                        ContextCompat.getColor(requireContext(), R.color.black)
                    } else {
                        ContextCompat.getColor(requireContext(), R.color.orange)
                    }
                )

                // get the "new recipe" card
                val newRecipeCard = allRecipeCards.find { it.tag == "new_recipe_card" }

                // filter the cards based on the query excluding the "new recipe" card
                val matchingCards = allRecipeCards.filter { card ->
                    if (card.tag == "new_recipe_card") {
                        false
                    } else {
                        // get the title of the card
                        val itemTitle = card.findViewById<TextView>(R.id.item_title)
                        val title = itemTitle?.text?.toString()?.lowercase(Locale.ROOT) ?: ""
                        title.startsWith(query)
                    }
                }

                // sort the matching cards by title
                val sortedMatchingCards = matchingCards.sortedBy { card ->
                    val itemTitle = card.findViewById<TextView>(R.id.item_title)
                    (itemTitle?.text?.toString()?.lowercase(Locale.ROOT) ?: "")
                }

                // recreate the card container
                cardContainer.removeAllViews()
                newRecipeCard?.let { cardContainer.addView(it) }
                sortedMatchingCards.forEach { cardContainer.addView(it) }
            }


        })

        lottieAnimationView = view.findViewById(R.id.lottie_animation_view)
        animationHandler = AnimationHandler(lottieAnimationView)
        // Load recipes from RecipeRepository
        animationHandler.showAnimation(Color.parseColor("#E8602E"))
        loadRecipes(categoryId)

        return view
    }

    private fun loadRecipes(categoryId: String) {
        RecipesRepository.loadRecipes(requireContext(), categoryId) { success ->
            if (success) {
                val recipes = RecipesRepository.getCachedRecipesForCategory(categoryId)
                // sort them by title
                val sortedRecipes = recipes.sortedBy { it.title.lowercase(Locale.ROOT) }
                sortedRecipes.forEach { recipe -> createRecipeCard(recipe) }
                animationHandler.hideAnimation()
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

            // Create an instance of RecipeFragment with the recipe data
            val fragment = RecipeFragment.newInstance(
                recipeName = recipe.title,
                imageUri = null,                     // Set imageUri to null since we're using imageBitmap
                imageBitmap = recipe.imageBitmap,     // Pass the Bitmap retrieved from RecipesRepository
                ingredients = ingredientsArray,
                instructions = recipe.instructions,
                source = FragmentSource.SAVED_RECIPIES_LIBRARY,
                categoryID = categoryId,
                recipeID = recipe.id,
                recipeCategory = recipe.recipeCategory
            )

            // Use GeneralFunctions to handle the navigation
            generalFunctions.navigateToFragment(fragment)

        }

        // Long click: afișăm popup-ul cu opțiunile Edit și Delete
        recipeCard.setOnLongClickListener {
            val inflater = LayoutInflater.from(requireContext())
            // Folosim același layout de popup, sau poți crea unul nou, de ex. popup_window_recipes.xml
            val popupView = inflater.inflate(R.layout.popup_window_categories, null)

            val popupWindow = PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )

            // Configurăm butonul EDIT
            val editButton = popupView.findViewById<LinearLayout>(R.id.edit_button)
            val editTextView = editButton.findViewById<TextView>(R.id.menu_option_text)
            val editIcon = editButton.findViewById<ImageView>(R.id.menu_option_icon)
            editTextView.text = "Edit"
            editTextView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            editIcon.setImageResource(R.drawable.icon_edit)  // Asigură-te că ai un icon corespunzător

            editButton.setOnClickListener {
                popupWindow.dismiss()
                showEditRecipeDialog(recipe)
            }

            // Configurăm butonul DELETE
            val deleteButton = popupView.findViewById<LinearLayout>(R.id.delete_button)
            val deleteTextView = deleteButton.findViewById<TextView>(R.id.menu_option_text)
            val deleteIcon = deleteButton.findViewById<ImageView>(R.id.menu_option_icon)
            deleteTextView.text = "Delete"
            deleteTextView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
            deleteIcon.setImageResource(R.drawable.icon_delete)  // Asigură-te că ai un icon corespunzător

            deleteButton.setOnClickListener {
                // Apelăm funcția din RecipesRepository pentru a șterge rețeta
                RecipesRepository.deleteRecipe(requireContext(), categoryId, recipe.id) { success ->
                    if (success) {
                        Toast.makeText(requireContext(), "Recipe deleted successfully", Toast.LENGTH_SHORT).show()
                        refreshRecipiesDisplay()
                    } else {
                        Toast.makeText(requireContext(), "Failed to delete recipe. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
                popupWindow.dismiss()
            }

            // Afișăm popup-ul sub card
            popupWindow.showAsDropDown(recipeCard)
            true
        }

        cardContainer.addView(recipeCard)
        allRecipeCards.add(recipeCard)
    }

    private fun showEditRecipeDialog(recipe: Recipe) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.modal_add_card)  // Poți crea un layout specific pentru editarea rețetei
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val dialogWidth = (resources.displayMetrics.widthPixels * 0.85).toInt()
        val dialogHeight = (resources.displayMetrics.heightPixels * 0.6).toInt()
        dialog.window?.setLayout(dialogWidth, dialogHeight)

        // Referințe la view-uri din dialog
        val coverPhotoSection = dialog.findViewById<LinearLayout>(R.id.cover_photo_section)
        val coverPhotoImage = dialog.findViewById<ImageView>(R.id.cover_photo_image)
        val uploadText = dialog.findViewById<TextView>(R.id.upload_text)
        val editTextName = dialog.findViewById<EditText>(R.id.new_modal_name)
        val saveButton = dialog.findViewById<Button>(R.id.save_button)
        val cancelButton = dialog.findViewById<Button>(R.id.cancel_button)

        // schimbam numele modalului
        dialog.findViewById<TextView>(R.id.modal_name).text = "Edit Recipe"


        // Pre-umple câmpurile cu datele rețetei
        editTextName.setText(recipe.title)
        if (recipe.imageBitmap != null) {
            coverPhotoImage.setImageBitmap(recipe.imageBitmap)
            uploadText.visibility = View.GONE
            coverPhotoImage.scaleType = ImageView.ScaleType.CENTER_CROP
        } else {
            uploadText.visibility = View.VISIBLE
        }

        // Permite utilizatorului să selecteze o nouă poză
        coverPhotoSection.setOnClickListener {
            // Exemplu: poți reutiliza un launcher din gallery sau o funcție similară
            generalFunctions.openGallery { bitmap: Bitmap ->
                coverPhotoImage.setImageBitmap(bitmap)
                uploadText.visibility = View.GONE
                // Poți salva bitmap-ul ales într-o variabilă temporară
                selectedPhoto = bitmap
            }
        }

        // Salvare: actualizează rețeta existentă
        saveButton.setOnClickListener {
            val updatedName = editTextName.text.toString().trim()
            if (updatedName.isNotEmpty()) {
                val photoToUse = selectedPhoto ?: recipe.imageBitmap
                // Aici ai nevoie de o funcție în repository pentru update
                RecipesRepository.updateRecipe(requireContext(), categoryId, recipe.id, updatedName, photoToUse) { success ->
                    if (success) {
                        Toast.makeText(requireContext(), "Recipe updated successfully", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        refreshRecipiesDisplay()
                    } else {
                        Toast.makeText(requireContext(), "Failed to update recipe. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Recipe name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
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
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryLauncher.launch(galleryIntent)
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
                Log.d("SavedRecipiesFragment", "Save button clicked. Saving recipe: $recipeName")
                dialog.dismiss()
                RecipesRepository.addNewRecipe(requireContext(), categoryId, recipeItem, selectedPhoto) { success ->
                    if (success) {
                        Toast.makeText(requireContext(), "Recipe added successfully", Toast.LENGTH_SHORT).show()
                        // Refresh the UI to display the new recipe
                        refreshRecipiesDisplay()
                    } else {
                        Toast.makeText(requireContext(), "Failed to add recipe. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Log.d("SavedRecipiesFragment", "Save button clicked but recipe name is empty.")
                Toast.makeText(requireContext(), "Recipe name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun refreshRecipiesDisplay() {
        // Use a temporary list to avoid modifying the collection while iterating
        val viewsToRemove = mutableListOf<View>()

        // Iterate through child views of cardContainer
        for (i in 0 until cardContainer.childCount) {
            val view = cardContainer.getChildAt(i)
            if (view.id != R.id.new_recipe_card) {
                viewsToRemove.add(view)
            }
        }

        // Remove views after iteration
        viewsToRemove.forEach { cardContainer.removeView(it) }

        // Reload the recipes
        loadRecipes(categoryId)
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
