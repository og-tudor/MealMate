package com.example.mealmate.dashboard.home
//
//import android.app.Application
//import android.graphics.Bitmap
//import android.util.Log
//import androidx.lifecycle.AndroidViewModel
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//import com.example.mealmate.utils.GoogleDriveHelper
//import com.google.firebase.firestore.FirebaseFirestore
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import java.io.ByteArrayOutputStream
//import java.util.UUID
//
//data class Category2(
//    var name: String,
//    val id: String,
//    var photo: Bitmap? = null
//)
//
//class CategoriesViewModel2(application: Application) : AndroidViewModel(application) {
//    val cachedCategories = mutableListOf<Category2>()
//    val categoriesLiveData = MutableLiveData<List<Category2>>()
//    val isLoading = MutableLiveData<Boolean>()
//    private val firestore = FirebaseFirestore.getInstance()
//    private val driveHelper = GoogleDriveHelper(context)
//
//    fun loadCategories(userId: String) {
//        if (cachedCategories.isNotEmpty()) {
//            categoriesLiveData.postValue(cachedCategories)
//            isLoading.postValue(false)
//            return
//        }
//
//        isLoading.postValue(true)
//        firestore.collection("users").document(userId).collection("categories")
//            .get()
//            .addOnSuccessListener { documents ->
//                val categoriesList = mutableListOf<Category2>()
//                documents.forEach { document ->
//                    val categoryName = document.getString("name") ?: ""
//                    val categoryId = document.id
//                    val category = Category2(name = categoryName, id = categoryId)
//                    categoriesList.add(category)
//                }
//                cachedCategories.clear()
//                cachedCategories.addAll(categoriesList)
//                categoriesLiveData.postValue(categoriesList)
//                isLoading.postValue(false)
//
//                // Load category photos after basic info is loaded
//                loadCategoryPhotos(userId, categoriesList)
//            }
//            .addOnFailureListener { e ->
//                Log.e("CategoriesViewModel2", "Error fetching categories: ${e.message}", e)
//                isLoading.postValue(false)
//            }
//    }
//
//    private fun loadCategoryPhotos(userId: String, categoriesList: List<Category2>) {
//        categoriesList.forEach { category ->
//            loadCategoryPhotoFromDrive(userId, category.id) { bitmap ->
//                category.photo = bitmap
//                categoriesLiveData.postValue(ArrayList(categoriesList))
//            }
//        }
//    }
//
//    private fun loadCategoryPhotoFromDrive(userId: String, categoryId: String, callback: (Bitmap?) -> Unit) {
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                val categoryFolderId = driveHelper.getOrCreateCategoryFolder(userId, categoryId)
//                val bitmap = driveHelper.getCategoryPhoto(categoryFolderId)
//                callback(bitmap)
//            } catch (e: Exception) {
//                Log.e("CategoriesViewModel2", "Error loading photo from Drive: ${e.message}", e)
//                callback(null)
//            }
//        }
//    }
//
//    fun addNewCategory(newCategoryName: String, photo: Bitmap? = null) {
//        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
//        if (userId == null) {
//            Log.e("CategoriesViewModel2", "User not authenticated.")
//            return
//        }
//
//        val newCategoryId = UUID.randomUUID().toString()
//        val categoryData = hashMapOf(
//            "name" to newCategoryName
//        )
//
//        firestore.collection("users").document(userId).collection("categories").document(newCategoryId)
//            .set(categoryData)
//            .addOnSuccessListener {
//                Log.d("CategoriesViewModel2", "New category added to Firestore: $newCategoryName")
//
//                // Upload the photo to Google Drive if provided
//                if (photo != null) {
//                    uploadPhotoToDrive(userId, newCategoryId, photo) { isSuccess ->
//                        if (isSuccess) {
//                            Log.d("CategoriesViewModel2", "Photo uploaded successfully for category: $newCategoryName")
//                            loadCategories(userId) // Refresh categories to include the new one
//                        } else {
//                            Log.e("CategoriesViewModel2", "Failed to upload photo for category: $newCategoryName")
//                        }
//                    }
//                } else {
//                    loadCategories(userId) // Refresh categories to include the new one if no photo provided
//                }
//            }
//            .addOnFailureListener { e ->
//                Log.e("CategoriesViewModel2", "Error adding new category to Firestore: ${e.message}", e)
//            }
//    }
//
//    private fun uploadPhotoToDrive(userId: String, categoryId: String, photo: Bitmap, callback: (Boolean) -> Unit) {
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                val categoryFolderId = driveHelper.getOrCreateCategoryFolder(userId, categoryId)
//
//                val outputStream = ByteArrayOutputStream()
//                photo.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
//                val photoData = outputStream.toByteArray()
//
//                driveHelper.uploadCategoryPhoto(categoryFolderId, "category_photo.png", photoData)
//                callback(true)
//            } catch (e: Exception) {
//                Log.e("CategoriesViewModel2", "Error uploading photo to Drive: ${e.message}", e)
//                callback(false)
//            }
//        }
//    }
//}
