package com.example.mealmate.dashboard.home

import Category2
import InitialsDrawable
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
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.example.mealmate.R
import com.example.mealmate.SavedRecipesFragment
import com.example.mealmate.dashboard.GeneralFunctions
import com.example.mealmate.repository.CategoriesRepository

class SavedCategoriesFragment : Fragment() {

    private lateinit var cardContainer: LinearLayout
    private lateinit var generalFunctions: GeneralFunctions
    private lateinit var coverPhotoImage: ImageView
    private var selectedImageUri: Uri? = null
    private lateinit var homeButton: ImageButton
    private lateinit var discoverButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private var selectedPhoto: Bitmap? = null


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
        val view = inflater.inflate(R.layout.categories_saved, container, false)
        initializeComponents(view)
        setupCategories()
        return view
    }

    private fun setupCategories() {
        // for every category in the database, add a card to the view
        val categories = CategoriesRepository.cachedCategories
        if (categories.isNotEmpty()) {
            categories.forEach { category ->
                addCategoryCard(category)
            }
        } else {
            Log.d("SavedCategoriesFragment", "No categories found.")
            Toast.makeText(requireContext(), "No categories available", Toast.LENGTH_SHORT).show()
        }
        addNewCategoryCard()
    }

    private fun initializeComponents(view: View) {
        cardContainer = view.findViewById(R.id.card_container_categories)
        homeButton = view.findViewById(R.id.home_button)
        discoverButton = view.findViewById(R.id.discover_button)
        settingsButton = view.findViewById(R.id.settings_button)
        homeButton.isSelected = true

        generalFunctions = GeneralFunctions(requireActivity(), homeButton, discoverButton, settingsButton)
        homeButton.setOnClickListener { generalFunctions.selectButton(homeButton) }
        discoverButton.setOnClickListener { generalFunctions.selectButton(discoverButton) }
        settingsButton.setOnClickListener { generalFunctions.selectButton(settingsButton) }

        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun addCategoryCard(category: Category2) {
        val inflater = LayoutInflater.from(requireContext())
        val categoryCard = inflater.inflate(R.layout.categories_default_card, cardContainer, false)

        val itemTitle = categoryCard.findViewById<TextView>(R.id.item_title)
        val itemImage = categoryCard.findViewById<ImageView>(R.id.item_image)

        itemTitle.text = category.name

        if (category.photo != null) {
            itemImage.setImageBitmap(category.photo)
        } else {
            val initialLetter = category.name.firstOrNull()?.uppercaseChar().toString()
            val initialsDrawable = InitialsDrawable(requireContext(), initialLetter)
            initialsDrawable.color = Color.DKGRAY
            itemImage.setImageDrawable(initialsDrawable)
        }

        categoryCard.setOnClickListener {
            val bundle = Bundle().apply {
                putString("categoryId", category.id)
            }
            generalFunctions.navigateToFragment(SavedRecipesFragment(), args = bundle)
        }

        cardContainer.addView(categoryCard)
    }

    private fun addNewCategoryCard() {
        val inflater = LayoutInflater.from(requireContext())
        val newCategoryCard = inflater.inflate(R.layout.categories_new_card, cardContainer, false)

        newCategoryCard.setOnClickListener {
            Log.d("SavedCategoriesFragment", "New Category card clicked.")
            showAddCategoryDialog()
        }

        cardContainer.addView(newCategoryCard)
    }

    private fun showAddCategoryDialog() {
        Log.d("SavedCategoriesFragment", "showAddCategoryDialog called.")
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.modal_add_card)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val dialogWidth = (resources.displayMetrics.widthPixels * 0.8).toInt()
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
            val newCategoryName = editTextName.text.toString().trim()
            if (newCategoryName.isNotEmpty()) {
                Log.d("SavedCategoriesFragment", "Save button clicked. Saving category: $newCategoryName")
                CategoriesRepository.addNewCategory(requireContext(), newCategoryName, selectedPhoto) { success ->
                    if (success) {
                        Toast.makeText(requireContext(), "Category added successfully", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        // Refresh the UI to display the new category
                        refreshCategoriesDisplay()
                    } else {
                        Toast.makeText(requireContext(), "Failed to add category. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Log.d("SavedCategoriesFragment", "Save button clicked but category name is empty.")
                Toast.makeText(requireContext(), "Category name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener {
            Log.d("SavedCategoriesFragment", "Cancel button clicked.")
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun refreshCategoriesDisplay() {
        cardContainer.removeAllViews()
        setupCategories()

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
