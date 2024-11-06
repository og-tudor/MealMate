package com.example.mealmate.network

import com.example.mealmate.model.MealResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface MealApiService {
    @GET("api/json/v1/1/random.php")
    suspend fun getRandomMeal(): MealResponse

    @GET("api/json/v1/1/search.php")
    suspend fun searchMeals(@Query("s") searchQuery: String): MealResponse
}