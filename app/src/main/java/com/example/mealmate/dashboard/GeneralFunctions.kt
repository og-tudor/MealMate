package com.example.mealmate.dashboard

import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.example.mealmate.dashboard.home.ExploreFragment
import com.example.mealmate.dashboard.home.SavedCategoriesFragment

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
}
