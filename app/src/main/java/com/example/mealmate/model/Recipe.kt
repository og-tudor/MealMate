package com.example.mealmate.model

data class Recipe(
    val id: String,
    val title: String,
    val imageUrl: String,
    val instructions: String,
    val ingredientsWithQuantities: List<Pair<String, String>>
)
