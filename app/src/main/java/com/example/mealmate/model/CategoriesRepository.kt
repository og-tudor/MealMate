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
    var isDataLoaded = false

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

    fun clearCachedCategories() {
        cachedCategories.clear()
        Log.d("CategoriesRepository", "Cached categories cleared.")
    }

    // Function to delete a category
    fun deleteCategory(context: Context, categoryId: String, callback: (Boolean) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("CategoriesRepository", "User not authenticated.")
            callback(false)
            return
        }

        val driveHelper = GoogleDriveHelper(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Step 1: Delete the photo from Google Drive
                val categoryFolderId = driveHelper.getOrCreateCategoryFolder(userId, categoryId)
                val photoDeleted = driveHelper.deleteCategoryPhoto(categoryFolderId)

                if (!photoDeleted) {
                    Log.e("CategoriesRepository", "Error deleting photo for category $categoryId")
                }

                // Step 2: Delete the Firestore category + subcollection
                val categoryDocRef = firestore.collection("users")
                    .document(userId)
                    .collection("categories")
                    .document(categoryId)

                // Fetch all documents in the "recipes" subcollection (or any other subcollection you want to remove)
                categoryDocRef.collection("recipes")
                    .get()
                    .addOnSuccessListener { recipesSnapshot ->
                        val batch = firestore.batch()

                        // Add all recipe documents to the batch for deletion
                        for (recipeDoc in recipesSnapshot.documents) {
                            batch.delete(recipeDoc.reference)
                        }
                        // Finally, delete the category document itself
                        batch.delete(categoryDocRef)

                        // Commit all deletions in one batch
                        batch.commit()
                            .addOnSuccessListener {
                                // Remove the category from cache
                                cachedCategories.removeIf { it.id == categoryId }
                                Log.d("CategoriesRepository", "Category $categoryId and all its subcollections deleted successfully.")
                                callback(true)
                            }
                            .addOnFailureListener { e ->
                                Log.e("CategoriesRepository", "Error deleting category: ${e.message}", e)
                                callback(false)
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("CategoriesRepository", "Error fetching recipes subcollection: ${e.message}", e)
                        callback(false)
                    }

            } catch (e: Exception) {
                Log.e("CategoriesRepository", "Error deleting category $categoryId: ${e.message}", e)
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

    fun updateCategory(
        context: Context,
        categoryId: String,
        newName: String,
        newPhoto: Bitmap?,
        callback: (Boolean) -> Unit
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("CategoriesRepository", "User not authenticated.")
            callback(false)
            return
        }

        val driveHelper = GoogleDriveHelper(context)
        val categoryDocRef = firestore.collection("users")
            .document(userId)
            .collection("categories")
            .document(categoryId)

        // 1. Update just the name in Firestore
        val updates = hashMapOf<String, Any>("name" to newName)

        categoryDocRef.update(updates)
            .addOnSuccessListener {
                // 2. If user picked a new photo, upload it to Drive
                if (newPhoto != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val categoryFolderId = driveHelper.getOrCreateCategoryFolder(userId, categoryId)

                            // Overwrite the existing photo
                            val outputStream = ByteArrayOutputStream()
                            newPhoto.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                            val photoData = outputStream.toByteArray()

                            driveHelper.uploadCategoryPhoto(categoryFolderId, "category_photo.png", photoData)

                            // Update local cache as well
                            cachedCategories.find { it.id == categoryId }?.apply {
                                name = newName
                                photo = newPhoto
                            }

                            withContext(Dispatchers.Main) {
                                Log.d("CategoriesRepository", "Category $categoryId updated successfully.")
                                callback(true)
                            }
                        } catch (e: Exception) {
                            Log.e("CategoriesRepository", "Error updating category photo: ${e.message}", e)
                            withContext(Dispatchers.Main) { callback(false) }
                        }
                    }
                } else {
                    // No new photo was chosen; just update name in cache
                    cachedCategories.find { it.id == categoryId }?.apply {
                        name = newName
                        // Keep the old photo
                    }
                    Log.d("CategoriesRepository", "Category $categoryId updated (no new photo).")
                    callback(true)
                }
            }
            .addOnFailureListener { e ->
                Log.e("CategoriesRepository", "Error updating category name: ${e.message}", e)
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
