package com.example.mealmate

import InitialsDrawable
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream

class SavedRecipesFragment : Fragment() {

    // Firebase instances
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var homeButton: ImageButton
    private lateinit var discoverButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private lateinit var cardContainer: LinearLayout
    private lateinit var coverPhotoImage: ImageView
    private var selectedImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.recipes_saved, container, false)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize buttons and containers
        homeButton = view.findViewById(R.id.home_button)
        discoverButton = view.findViewById(R.id.discover_button)
        settingsButton = view.findViewById(R.id.settings_button)
        cardContainer = view.findViewById(R.id.card_container)
        homeButton.isSelected = true

        // Set up click listeners for each button
        homeButton.setOnClickListener { selectButton(homeButton) }
        discoverButton.setOnClickListener { selectButton(discoverButton) }
        settingsButton.setOnClickListener { selectButton(settingsButton) }

        // Apply window insets to padding for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Find the "New Category" card
//        val newCategoryCard = view.findViewById<View>(R.id.new_category_card)
//        newCategoryCard.setOnClickListener { showAddCategoryDialog() }
//
//        // Load categories from Firestore
//        loadCategoriesFromFirestore()

        return view
    }

    // Function to select a button and handle navigation
    private fun selectButton(selectedButton: ImageButton) {
        homeButton.isSelected = false
        discoverButton.isSelected = false
        settingsButton.isSelected = false

        selectedButton.isSelected = true

        // Handle navigation based on selected button
        when (selectedButton) {
            homeButton -> navigateToFragment(SavedRecipesFragment())
            discoverButton -> navigateToFragment(SavedRecipesFragment()) // Replace with DiscoverFragment
            settingsButton -> navigateToFragment(SavedRecipesFragment()) // Replace with SettingsFragment
        }
    }

    // Function to navigate to a fragment
    private fun navigateToFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    // Other functions (loadCategoriesFromFirestore, showAddCategoryDialog, etc.) remain unchanged
}
