package com.example.mealmate.dashboard.home

import androidx.lifecycle.ViewModel
import android.net.Uri

data class Category(var name: String, val imageUri: Uri?, val id: String)

class CategoriesViewModel : ViewModel() {
    val cachedCategories = mutableListOf<Category>()
    // Flag to track if the layout was already created
    var layoutInitialized = false
}
