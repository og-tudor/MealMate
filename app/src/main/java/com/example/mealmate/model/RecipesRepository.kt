package com.example.mealmate.repository

import Ingredient
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.mealmate.model.Recipe
import com.example.mealmate.utils.GoogleDriveHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object RecipesRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val cachedRecipes = mutableMapOf<String, MutableList<Recipe>>() // Cache recipes per category
    private var isDataLoaded = false



    fun loadRecipes(context: Context, categoryId: String, callback: (Boolean) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("RecipesRepository", "User ID is null. Cannot load recipes.")
            callback(false)
            return
        }

        if (isDataLoaded && cachedRecipes.containsKey(categoryId)) {
            callback(true) // If data is already loaded for this category, call the callback immediately
            return
        }

        // Clear existing recipes for this category
        cachedRecipes[categoryId] = mutableListOf()

        firestore.collection("users").document(userId).collection("categories").document(categoryId)
            .collection("recipes")
            .get()
            .addOnSuccessListener { documents ->
                val recipesList = mutableListOf<Recipe>()
                documents.forEach { document ->
                    val recipe = document.toObject(Recipe::class.java).copy(id = document.id)
                    recipesList.add(recipe)
                }
                cachedRecipes[categoryId] = recipesList
                loadRecipePhotos(context, userId, categoryId, recipesList) { success ->
                    isDataLoaded = success
                    callback(success)
                }
            }
            .addOnFailureListener { e ->
                Log.e("RecipesRepository", "Error fetching recipes: ${e.message}", e)
                callback(false)
            }
    }

    private fun loadRecipePhotos(context: Context, userId: String, categoryId: String, recipesList: List<Recipe>, callback: (Boolean) -> Unit) {
        val driveHelper = GoogleDriveHelper(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                recipesList.forEach { recipe ->
                    val recipeFolderId = driveHelper.getOrCreateRecipeFolder(userId, categoryId, recipe.id)
                    val bitmap = driveHelper.getRecipePhoto(recipeFolderId)
                    // Assuming `Recipe` has a property to hold the photo, if not add one like `var photo: Bitmap? = null`
                    recipe.imageBitmap = bitmap
                }
                withContext(Dispatchers.Main) {
                    callback(true)
                }
            } catch (e: Exception) {
                Log.e("RecipesRepository", "Error loading photos from Drive: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(false)
                }
            }
        }
    }

    fun addNewRecipe(context: Context, categoryId: String, recipe: Recipe, photo: Bitmap?, callback: (Boolean) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("RecipesRepository", "User not authenticated.")
            callback(false)
            return
        }

        val newRecipeId = recipe.id.ifEmpty { java.util.UUID.randomUUID().toString() }
        val recipeData = hashMapOf(
            "title" to recipe.title,
            "instructions" to recipe.instructions,
            "ingredientsWithQuantities" to recipe.ingredientsWithQuantities
        )

        firestore.collection("users").document(userId).collection("categories").document(categoryId)
            .collection("recipes").document(newRecipeId)
            .set(recipeData)
            .addOnSuccessListener {
                Log.d("RecipesRepository", "New recipe added to Firestore: ${recipe.title}")

                if (photo != null) {
                    uploadRecipePhotoToDrive(context, userId, categoryId, newRecipeId, photo) { isSuccess ->
                        if (isSuccess) {
                            // Create the updated recipe with the new photo
                            val updatedRecipe = recipe.copy(id = newRecipeId, imageBitmap = photo)
                            cachedRecipes[categoryId]?.add(updatedRecipe)

                            callback(true)
                        } else {
                            callback(false)
                        }
                    }
                } else {
                    val updatedRecipe = recipe.copy(id = newRecipeId)
                    cachedRecipes[categoryId]?.add(updatedRecipe)
                    callback(true)
                }

            }
            .addOnFailureListener { e ->
                Log.e("RecipesRepository", "Error adding new recipe to Firestore: ${e.message}", e)
                callback(false)
            }
    }

    private fun uploadRecipePhotoToDrive(context: Context, userId: String, categoryId: String, recipeId: String, photo: Bitmap, callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val driveHelper = GoogleDriveHelper(context)
                val recipeFolderId = driveHelper.getOrCreateRecipeFolder(userId, categoryId, recipeId)

                val outputStream = ByteArrayOutputStream()
                photo.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                val photoData = outputStream.toByteArray()

                driveHelper.uploadRecipePhoto(recipeFolderId, photoData)
                withContext(Dispatchers.Main) {
                    callback(true)
                }
            } catch (e: Exception) {
                Log.e("RecipesRepository", "Error uploading recipe photo to Drive: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(false)
                }
            }
        }
    }

    fun getCachedRecipesForCategory(categoryId: String): List<Recipe> {
        return cachedRecipes[categoryId] ?: emptyList()
    }

    fun addIngredient(
        context: Context,
        categoryId: String,
        recipeId: String,
        newIngredient: Ingredient,
        callback: (Boolean) -> Unit
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("RecipesRepository", "User not authenticated.")
            callback(false)
            return
        }

        val recipeRef = firestore.collection("users").document(userId)
            .collection("categories").document(categoryId)
            .collection("recipes").document(recipeId)

        // Use Firestore's arrayUnion to add a new ingredient to the ingredients list
        recipeRef.update("ingredientsWithQuantities", FieldValue.arrayUnion(newIngredient))
            .addOnSuccessListener {
                Log.d("RecipesRepository", "Ingredient added successfully.")

                // Update the cached version of the recipe if available
                cachedRecipes[categoryId]?.find { it.id == recipeId }?.let { recipe ->
                    // Directly add the new ingredient to the ingredientsWithQuantities list
                    val updatedIngredients = recipe.ingredientsWithQuantities.toMutableList()
                    updatedIngredients.add(newIngredient)

                    // Replace the recipe in the cache with the updated ingredients list
                    val updatedRecipe = recipe.copy(ingredientsWithQuantities = updatedIngredients)

                    // Update the recipe in the cache
                    cachedRecipes[categoryId] = cachedRecipes[categoryId]?.map {
                        if (it.id == recipeId) updatedRecipe else it
                    }?.toMutableList() ?: mutableListOf()
                }

                callback(true)
            }
            .addOnFailureListener { e ->
                Log.e("RecipesRepository", "Error adding ingredient: ${e.message}", e)
                callback(false)
            }
    }

    fun updateRecipeInstructions(
        context: Context,
        categoryId: String,
        recipeId: String,
        updatedInstructions: String,
        callback: (Boolean) -> Unit
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("RecipesRepository", "User not authenticated.")
            callback(false)
            return
        }

        val recipeRef = firestore.collection("users").document(userId)
            .collection("categories").document(categoryId)
            .collection("recipes").document(recipeId)

        // Update the instructions field in Firestore
        recipeRef.update("instructions", updatedInstructions)
            .addOnSuccessListener {
                Log.d("RecipesRepository", "Instructions updated successfully in Firestore.")

                // Update the cached version of the recipe
                cachedRecipes[categoryId]?.find { it.id == recipeId }?.let { recipe ->
                    val updatedRecipe = recipe.copy(instructions = updatedInstructions)

                    // Update the recipe in the cache
                    cachedRecipes[categoryId] = cachedRecipes[categoryId]?.map {
                        if (it.id == recipeId) updatedRecipe else it
                    }?.toMutableList() ?: mutableListOf()
                }

                callback(true)
            }
            .addOnFailureListener { e ->
                Log.e("RecipesRepository", "Error updating instructions: ${e.message}", e)
                callback(false)
            }
    }



}
