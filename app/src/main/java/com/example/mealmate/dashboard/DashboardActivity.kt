package com.example.mealmate.dashboard

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.mealmate.R
import com.example.mealmate.dashboard.home.SavedCategoriesFragment

class DashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Load the SavedRecipesFragment by default
        if (savedInstanceState == null) {
            loadFragment(SavedCategoriesFragment())
        }
    }

    // Function to load a fragment into the fragment container
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
