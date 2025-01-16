package com.example.mealmate.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

import java.io.FileOutputStream


class GoogleDriveHelper(private val context: Context) {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    companion object {
        private const val MEALMATE_FOLDER_ID = "1PAnMRX-Lco3npWNjz5IZZcNqB32O4wlY" // Constant for the MealMate folder ID
        private const val PROFILE_PICTURE_NAME = "profile_picture.png"
        private const val CATEGORY_PHOTO_NAME = "category_photo.png"
        private const val RECIPE_PHOTO_NAME = "recipe_photo.png"
    }

    private fun getDriveService(): Drive {
        val jsonFactory = JacksonFactory.getDefaultInstance()
        val httpTransport = NetHttpTransport()

        // Load the JSON key file from the assets folder
        val inputStream: InputStream = context.assets.open("drive_service_account.json")

        val credential = GoogleCredential.fromStream(inputStream, httpTransport, jsonFactory)
            .createScoped(listOf(DriveScopes.DRIVE))

        return Drive.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName("MealMate")
            .build()
    }

    suspend fun getUserFolderId(userId: String): String {
        val driveService = getDriveService()
        val query = "name = '$userId' and '$MEALMATE_FOLDER_ID' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false"

        val result: FileList = driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()

        return if (result.files.isNotEmpty()) {
            // Return the ID of the first folder found
            result.files[0].id
        } else {
            // If no folder exists, create one
            val folderMetadata = File().apply {
                name = userId
                mimeType = "application/vnd.google-apps.folder"
                parents = listOf(MEALMATE_FOLDER_ID)
            }
            val folder = driveService.files().create(folderMetadata)
                .setFields("id")
                .execute()
            Log.d("GoogleDriveHelper", "Created new folder for user $userId with ID: ${folder.id}")
            folder.id
        }
    }

    suspend fun getOrCreateCategoryFolder(userId: String, categoryId: String): String {
        val driveService = getDriveService()
        val userFolderId = getUserFolderId(userId)

        val query = "name = '$categoryId' and '$userFolderId' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        val result: FileList = driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()

        return if (result.files.isNotEmpty()) {
            result.files[0].id
        } else {
            val folderMetadata = File().apply {
                name = categoryId
                mimeType = "application/vnd.google-apps.folder"
                parents = listOf(userFolderId)
            }
            val folder = driveService.files().create(folderMetadata)
                .setFields("id")
                .execute()
            Log.d("GoogleDriveHelper", "Created new folder for category $categoryId with ID: ${folder.id}")
            folder.id
        }
    }

    fun uploadCategoryPhoto(categoryFolderId: String, fileName: String, photoData: ByteArray) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val driveService = getDriveService()

                // Check if a category photo already exists and delete it
                val query = "name = '$CATEGORY_PHOTO_NAME' and '$categoryFolderId' in parents and trashed = false"
                val result: FileList = driveService.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("files(id, name)")
                    .execute()

                if (result.files.isNotEmpty()) {
                    for (file in result.files) {
                        driveService.files().delete(file.id).execute()
                        Log.d("GoogleDriveHelper", "Deleted existing category photo: ${file.id}")
                    }
                }

                // Create a temporary file from the byte array
                val tempFile = java.io.File.createTempFile("temp_category_photo", ".png", context.cacheDir)
                tempFile.writeBytes(photoData)

                // Upload the new category photo
                val fileMetadata = File().apply {
                    name = fileName
                    parents = listOf(categoryFolderId)
                }
                val mediaContent = FileContent("image/png", tempFile)

                val uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()

                Log.d("GoogleDriveHelper", "Category photo uploaded successfully. File ID: ${uploadedFile.id}")

                // Delete the temporary file after upload
                tempFile.delete()

            } catch (e: Exception) {
                Log.e("GoogleDriveHelper", "Error uploading category photo: ${e.message}", e)
            }
        }
    }


    suspend fun getCategoryPhoto(categoryFolderId: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService()
            val query = "name = '$CATEGORY_PHOTO_NAME' and '$categoryFolderId' in parents and trashed = false"
            val result: FileList = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()

            if (result.files.isNotEmpty()) {
                val fileId = result.files[0].id
                val inputStream = driveService.files().get(fileId).executeMediaAsInputStream()
                BitmapFactory.decodeStream(inputStream)
            } else {
                Log.d("GoogleDriveHelper", "No category photo found.")
                null
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveHelper", "Error fetching category photo: ${e.message}", e)
            null
        }
    }


    suspend fun deleteRecipeFolder(recipeFolderId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService()
            // Construim o interogare pentru a lista toate fișierele din folderul dat
            val query = "'$recipeFolderId' in parents and trashed = false"
            val result: FileList = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id)")
                .execute()

            // Ștergem fiecare fișier găsit în folder
            result.files.forEach { file ->
                driveService.files().delete(file.id).execute()
                Log.d("GoogleDriveHelper", "Deleted file: ${file.id} in recipe folder: $recipeFolderId")
            }

            // Ștergem apoi folderul propriu-zis
            driveService.files().delete(recipeFolderId).execute()
            Log.d("GoogleDriveHelper", "Recipe folder deleted successfully for folder: $recipeFolderId")
            true
        } catch (e: Exception) {
            Log.e("GoogleDriveHelper", "Error deleting recipe folder: $recipeFolderId. ${e.message}", e)
            false
        }
    }



    fun uploadUserProfilePicture(filePath: java.io.File) {
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    Log.e("GoogleDriveHelper", "User is not logged in")
                    return@launch
                }

                val userId = currentUser.uid

                // Get or create the user-specific folder ID
                val userFolderId = getUserFolderId(userId)

                // Search for any existing profile picture in the folder
                val driveService = getDriveService()
                val query = "name = '$PROFILE_PICTURE_NAME' and '$userFolderId' in parents and mimeType != 'application/vnd.google-apps.folder' and trashed = false"
                val result: FileList = driveService.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("files(id, name)")
                    .execute()

                // Delete any existing profile picture
                if (result.files.isNotEmpty()) {
                    for (file in result.files) {
                        driveService.files().delete(file.id).execute()
                        Log.d("GoogleDriveHelper", "Deleted existing profile picture: ${file.id}")
                    }
                }

                // Prepare the file metadata for the new profile picture upload
                val fileMetadata = File().apply {
                    name = PROFILE_PICTURE_NAME
                    parents = listOf(userFolderId)
                }

                val mediaContent = FileContent("image/png", filePath)

                val uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()

                Log.d("GoogleDriveHelper", "Profile picture uploaded successfully. File ID: ${uploadedFile.id}")
            } catch (e: Exception) {
                Log.e("GoogleDriveHelper", "Error uploading profile picture: ${e.message}", e)
            }
        }
    }


    suspend fun getUserProfilePicture(userId: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService()
            val userFolderId = getUserFolderId(userId)
            Log.d("GoogleDriveHelper", "Using folder ID: $userFolderId for profile picture search")

            val query = "name = '$PROFILE_PICTURE_NAME' and '$userFolderId' in parents and mimeType != 'application/vnd.google-apps.folder' and trashed = false"
            Log.d("GoogleDriveHelper", "Executing query: $query")

            val result: FileList = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()

            if (result.files.isNotEmpty()) {
                Log.d("GoogleDriveHelper", "Profile picture found: ${result.files[0].id}")
                val fileId = result.files[0].id
                val inputStream = driveService.files().get(fileId).executeMediaAsInputStream()
                BitmapFactory.decodeStream(inputStream)
            } else {
                Log.d("GoogleDriveHelper", "No profile picture found.")
                null // No profile picture found
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveHelper", "Error fetching profile picture: ${e.message}", e)
            null
        }
    }


    fun cacheProfilePicture(bitmap: Bitmap, userId: String) {
        try {
            val cacheDir = java.io.File(context.cacheDir, "profile_pictures")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val file = java.io.File(cacheDir, "$userId-profile_picture.png")
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            Log.d("GoogleDriveHelper", "Profile picture cached at: ${file.path}")
        } catch (e: Exception) {
            Log.e("GoogleDriveHelper", "Error caching profile picture: ${e.message}", e)
        }
    }

    fun getCachedProfilePicture(userId: String): Bitmap? {
        val cacheFile =
            java.io.File(context.cacheDir, "profile_pictures/$userId-profile_picture.png")
        return if (cacheFile.exists()) {
            BitmapFactory.decodeFile(cacheFile.path).also {
                Log.d("GoogleDriveHelper", "Loaded profile picture from cache.")
            }
        } else {
            Log.d("GoogleDriveHelper", "No cached profile picture found.")
            null
        }
    }

    // Function to retrieve or create a folder for a specific recipe in Google Drive
    suspend fun getOrCreateRecipeFolder(userId: String, categoryId: String, recipeId: String): String {
        val driveService = getDriveService()
        val categoryFolderId = getOrCreateCategoryFolder(userId, categoryId)

        val query = "name = '$recipeId' and '$categoryFolderId' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        val result: FileList = driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()

        return if (result.files.isNotEmpty()) {
            result.files[0].id
        } else {
            val folderMetadata = File().apply {
                name = recipeId
                mimeType = "application/vnd.google-apps.folder"
                parents = listOf(categoryFolderId)
            }
            val folder = driveService.files().create(folderMetadata)
                .setFields("id")
                .execute()
            Log.d("GoogleDriveHelper", "Created new folder for recipe $recipeId with ID: ${folder.id}")
            folder.id
        }
    }

    // Function to upload a recipe photo to the specified recipe folder in Google Drive
    fun uploadRecipePhoto(recipeFolderId: String, photoData: ByteArray) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val driveService = getDriveService()

                // Check if a recipe photo already exists and delete it
                val query = "name = '$RECIPE_PHOTO_NAME' and '$recipeFolderId' in parents and trashed = false"
                val result: FileList = driveService.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("files(id, name)")
                    .execute()

                if (result.files.isNotEmpty()) {
                    for (file in result.files) {
                        driveService.files().delete(file.id).execute()
                        Log.d("GoogleDriveHelper", "Deleted existing recipe photo: ${file.id}")
                    }
                }

                // Create a temporary file from the byte array
                val tempFile = java.io.File.createTempFile("temp_recipe_photo", ".png", context.cacheDir)
                tempFile.writeBytes(photoData)

                // Upload the new recipe photo
                val fileMetadata = File().apply {
                    name = RECIPE_PHOTO_NAME
                    parents = listOf(recipeFolderId)
                }
                val mediaContent = FileContent("image/png", tempFile)

                val uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()

                Log.d("GoogleDriveHelper", "Recipe photo uploaded successfully. File ID: ${uploadedFile.id}")

                // Delete the temporary file after upload
                tempFile.delete()

            } catch (e: Exception) {
                Log.e("GoogleDriveHelper", "Error uploading recipe photo: ${e.message}", e)
            }
        }
    }

    // Function to retrieve a recipe photo from Google Drive
    suspend fun getRecipePhoto(recipeFolderId: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService()
            val query = "name = '$RECIPE_PHOTO_NAME' and '$recipeFolderId' in parents and trashed = false"
            val result: FileList = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()

            if (result.files.isNotEmpty()) {
                val fileId = result.files[0].id
                val inputStream = driveService.files().get(fileId).executeMediaAsInputStream()
                BitmapFactory.decodeStream(inputStream)
            } else {
                Log.d("GoogleDriveHelper", "No recipe photo found.")
                null
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveHelper", "Error fetching recipe photo: ${e.message}", e)
            null
        }
    }

    fun deleteCategoryPhoto(folderId: String): Boolean {
        return try {
            val driveService = getDriveService() // Ensure the Drive service is initialized
            // Delete the entire folder and its contents
            driveService.files().delete(folderId).execute()
            Log.d("GoogleDriveHelper", "Category folder and its contents deleted successfully for folder: $folderId")
            true
        } catch (e: Exception) {
            Log.e("GoogleDriveHelper", "Error deleting category folder: $folderId. ${e.message}", e)
            false
        }
    }


}
