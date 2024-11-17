package com.example.mealmate.repository

import Category2
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.mealmate.utils.GoogleDriveHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object CategoriesRepository {
    val cachedCategories = mutableListOf<Category2>()
    private val firestore = FirebaseFirestore.getInstance()
    private var isDataLoaded = false

    // Load categories from Firestore and Google Drive
    fun loadCategories(context: Context, callback: (Boolean) -> Unit) {
        if (isDataLoaded) {
            // If data is already loaded, call the callback immediately
            callback(true)
            return
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("CategoriesRepository", "User ID is null. Cannot load categories.")
            callback(false)
            return
        }

        // Clear the cache before loading new data
        cachedCategories.clear()

        firestore.collection("users").document(userId).collection("categories")
            .get()
            .addOnSuccessListener { documents ->
                val categoriesList = mutableListOf<Category2>()
                documents.forEach { document ->
                    val categoryName = document.getString("name") ?: ""
                    val categoryId = document.id
                    val category = Category2(name = categoryName, id = categoryId)
                    categoriesList.add(category)
                }
                cachedCategories.addAll(categoriesList)
                loadCategoryPhotos(context, userId, categoriesList) { success ->
                    isDataLoaded = success
                    callback(success)
                }
            }
            .addOnFailureListener { e ->
                Log.e("CategoriesRepository", "Error fetching categories: ${e.message}", e)
                callback(false)
            }
    }

    // Load photos for each category from Google Drive
    private fun loadCategoryPhotos(
        context: Context,
        userId: String,
        categoriesList: List<Category2>,
        callback: (Boolean) -> Unit
    ) {
        val driveHelper = GoogleDriveHelper(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Use `async` to fetch category photos in parallel
                val deferredResults = categoriesList.map { category ->
                    async {
                        try {
                            val categoryFolderId = driveHelper.getOrCreateCategoryFolder(userId, category.id)
                            val bitmap = driveHelper.getCategoryPhoto(categoryFolderId)
                            category.photo = bitmap
                        } catch (e: Exception) {
                            Log.e("CategoriesRepository", "Error fetching photo for category ${category.id}: ${e.message}", e)
                        }
                    }
                }

                // Wait for all parallel tasks to complete
                deferredResults.awaitAll()

                // Switch to the main thread and invoke the callback
                withContext(Dispatchers.Main) {
                    callback(true)
                }
            } catch (e: Exception) {
                Log.e("CategoriesRepository", "Error loading photos from Drive: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(false)
                }
            }
        }
    }


    // Function to add a new category
    fun addNewCategory(context: Context, newCategoryName: String, photo: Bitmap?, callback: (Boolean) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("CategoriesRepository", "User not authenticated.")
            callback(false)
            return
        }

        val newCategoryId = java.util.UUID.randomUUID().toString()
        val categoryData = hashMapOf("name" to newCategoryName)

        firestore.collection("users").document(userId).collection("categories").document(newCategoryId)
            .set(categoryData)
            .addOnSuccessListener {
                Log.d("CategoriesRepository", "New category added to Firestore: $newCategoryName")

                if (photo != null) {
                    uploadPhotoToDrive(context, userId, newCategoryId, photo) { isSuccess ->
                        if (isSuccess) {
                            val newCategory = Category2(name = newCategoryName, id = newCategoryId, photo = photo)
                            cachedCategories.add(newCategory) // Add the new category to the cache
                        }
                        callback(isSuccess)
                    }
                } else {
                    val newCategory = Category2(name = newCategoryName, id = newCategoryId)
                    cachedCategories.add(newCategory) // Add the new category to the cache
                    callback(true)
                }
            }
            .addOnFailureListener { e ->
                Log.e("CategoriesRepository", "Error adding new category to Firestore: ${e.message}", e)
                callback(false)
            }
    }

    private fun uploadPhotoToDrive(context: Context, userId: String, categoryId: String, photo: Bitmap, callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val driveHelper = GoogleDriveHelper(context)
                val categoryFolderId = driveHelper.getOrCreateCategoryFolder(userId, categoryId)

                val outputStream = ByteArrayOutputStream()
                photo.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                val photoData = outputStream.toByteArray()

                driveHelper.uploadCategoryPhoto(categoryFolderId, "category_photo.png", photoData)
                withContext(Dispatchers.Main) {
                    callback(true)
                }
            } catch (e: Exception) {
                Log.e("CategoriesRepository", "Error uploading photo to Drive: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(false)
                }
            }
        }
    }
}
