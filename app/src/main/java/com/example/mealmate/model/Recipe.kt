package com.example.mealmate.model

import Ingredient
import android.graphics.Bitmap

data class Recipe(
    val id: String = "",
    val title: String = "",
    val imageUrl: String = "",
    val instructions: String = "",
    val ingredientsWithQuantities: List<Ingredient> = emptyList(),
    var imageBitmap: Bitmap? = null,
    val recipeCategory: String = "",
)
