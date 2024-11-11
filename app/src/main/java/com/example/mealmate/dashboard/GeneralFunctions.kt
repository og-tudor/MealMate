package com.example.mealmate.dashboard

import SettingsFragment
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.example.mealmate.R
import com.example.mealmate.dashboard.home.ExploreFragment
import com.example.mealmate.dashboard.home.SavedCategoriesFragment

class GeneralFunctions(private val activity: FragmentActivity,
                       var homeButton: ImageButton? = null,
                       var discoverButton: ImageButton? = null,
                       var settingsButton: ImageButton? = null
) {

    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private var onImageSelected: ((Bitmap) -> Unit)? = null

    init {
        // Initialize the gallery launcher
        galleryLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val selectedImageUri: Uri? = result.data?.data
                if (selectedImageUri != null) {
                    val bitmap = getBitmapFromUri(selectedImageUri)
                    if (bitmap != null) {
                        onImageSelected?.invoke(bitmap)
                    } else {
                        Toast.makeText(activity, "Failed to convert image", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(activity, "No image selected", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Function to open the gallery
    fun openGallery(onImageSelected: (Bitmap) -> Unit) {
        this.onImageSelected = onImageSelected
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        galleryLauncher.launch(intent)
    }

    // Convert URI to Bitmap
    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(activity.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(activity.contentResolver, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Handle the selected image URI
    private fun handleSelectedImage(uri: Uri) {
        val bitmap = getBitmapFromUri(uri)
        if (bitmap != null) {
            Toast.makeText(activity, "Image selected successfully", Toast.LENGTH_SHORT).show()
        }
    }

    //  Another constructor to initialize GeneralFunctions without buttons
    constructor(activity: FragmentActivity) : this(activity, null, null, null)
    // Function to select a button and handle navigation
    fun selectButton(selectedButton: ImageButton) {
        // Ensure buttons are non-null before use
        homeButton?.isSelected = false
        discoverButton?.isSelected = false
        settingsButton?.isSelected = false

        // Select the chosen button
        selectedButton.isSelected = true

        // Handle navigation based on the selected button
        when (selectedButton) {
            homeButton -> navigateToFragment(SavedCategoriesFragment())
            discoverButton -> navigateToFragment(ExploreFragment()) // Replace with DiscoverFragment if needed
            settingsButton -> navigateToFragment(SettingsFragment()) // Replace with SettingsFragment if needed
        }
    }

    // General function to navigate to any fragment with optional arguments and an optional view to hide
    fun navigateToFragment(fragment: Fragment, args: Bundle? = null, hideView: View? = null) {
        if (args != null) {
            fragment.arguments = args
        }

        activity.supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()

        hideView?.visibility = View.INVISIBLE
    }

    fun navigateToActivity(targetActivity: Class<*>) {
        Log.d("GeneralFunctions", "Navigating to ${targetActivity.simpleName}")
        val intent = Intent(activity, targetActivity)
        activity.startActivity(intent)
        activity.finish() // Optional: finish the current activity if needed
    }

    fun setupSearchBar(searchBarContainer: LinearLayout, searchInput: EditText, searchIcon: ImageView, onSearchClick: (String) -> Unit) {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) {
                    // Change the background color of the search bar and the icon color when text is entered
                    searchIcon.setColorFilter(Color.parseColor("#D94209")) // Change to any contrasting color
                } else {
                    // Reset to the original background and icon color when the text is cleared
                    searchIcon.setColorFilter(Color.parseColor("#2A302D")) // Original color (or any default color)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Set click listener on the search icon
        searchIcon.setOnClickListener {
            val query = searchInput.text.toString().trim()
            if (query.isNotEmpty()) {
                onSearchClick(query)

                // Clear the search input after the search
                searchInput.text.clear()

                // Hide the keyboard
                val inputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(searchInput.windowToken, 0)

            } else {
                Toast.makeText(activity, "Please enter a valid search", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
