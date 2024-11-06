package com.example.mealmate.network

import com.example.mealmate.model.MealResponse
import retrofit2.http.GET

interface MealApiService {
    @GET("api/json/v1/1/random.php")
    suspend fun getRandomMeal(): MealResponse
}