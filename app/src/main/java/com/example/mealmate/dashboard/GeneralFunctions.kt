package com.example.mealmate.dashboard

import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.example.mealmate.R
import com.example.mealmate.dashboard.home.ExploreFragment
import com.example.mealmate.dashboard.home.SavedCategoriesFragment
import android.content.Context
import android.view.inputmethod.InputMethodManager

class GeneralFunctions(
    private val activity: FragmentActivity,
    var homeButton: ImageButton,
    var discoverButton: ImageButton,
    var settingsButton: ImageButton
) {
    // Function to select a button and handle navigation
    fun selectButton(selectedButton: ImageButton) {
        // Deselect all buttons
        homeButton.isSelected = false
        discoverButton.isSelected = false
        settingsButton.isSelected = false

        // Select the chosen button
        selectedButton.isSelected = true

        // Handle navigation based on the selected button
        when (selectedButton) {
            homeButton -> navigateToFragment(SavedCategoriesFragment())
            discoverButton -> navigateToFragment(ExploreFragment()) // Replace with DiscoverFragment
            settingsButton -> navigateToFragment(SavedCategoriesFragment()) // Replace with SettingsFragment
        }
    }

    // Function to navigate to the specified fragment
    private fun navigateToFragment(fragment: Fragment) {
        activity.supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment)
            .addToBackStack(null)
            .commit()
    }


    fun setupSearchBar(searchBarContainer: LinearLayout, searchInput: EditText, searchIcon: ImageView, onSearchClick: (String) -> Unit) {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) {
                    // Change the background color of the search bar and the icon color when text is entered
                    searchIcon.setColorFilter(Color.parseColor("#D94209")) // Change to white or any color that contrasts
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
