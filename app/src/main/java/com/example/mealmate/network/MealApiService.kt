package com.example.mealmate.network

import com.example.mealmate.model.CategoryResponse
import com.example.mealmate.model.MealBriefResponse
import com.example.mealmate.model.MealResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface MealApiService {
    @GET("api/json/v1/1/random.php")
    suspend fun getRandomMeal(): MealResponse

    @GET("api/json/v1/1/search.php")
    suspend fun searchMeals(@Query("s") searchQuery: String): MealResponse

    @GET("api/json/v1/1/categories.php")
    suspend fun getCategories(): CategoryResponse

    @GET("api/json/v1/1/filter.php")
    suspend fun getMealsByCategory(@Query("c") category: String): MealBriefResponse

    @GET("api/json/v1/1/lookup.php")
    suspend fun getMealById(@Query("i") id: String): MealResponse
}