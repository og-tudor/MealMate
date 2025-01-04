package com.example.mealmate.dashboard

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.example.mealmate.R
import com.example.mealmate.SettingsFragment
import com.example.mealmate.dashboard.home.ExploreFragment
import com.example.mealmate.dashboard.home.SavedCategoriesFragment

class GeneralFunctions(private val activity: FragmentActivity,
                       var homeButton: ImageButton? = null,
                       var discoverButton: ImageButton? = null,
                       var settingsButton: ImageButton? = null
) {

    private var onImageSelected: ((Bitmap) -> Unit)? = null

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


    // Function to open the gallery via DashboardActivity
    fun openGallery(onImageSelected: (Bitmap) -> Unit) {
        this.onImageSelected = onImageSelected
        if (activity is DashboardActivity) {
            activity.openGallery { bitmap ->
                onImageSelected(bitmap)
            }
        } else {
            Toast.makeText(activity, "Failed to open gallery", Toast.LENGTH_SHORT).show()
        }
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
