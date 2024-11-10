package com.example.mealmate.utils

import android.content.Context
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
import java.io.InputStream

class GoogleDriveHelper(private val context: Context) {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    companion object {
        private const val MEALMATE_FOLDER_ID = "1PAnMRX-Lco3npWNjz5IZZcNqB32O4wlY" // Constant for the MealMate folder ID
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

    private suspend fun getUserFolderId(userId: String): String {
        val driveService = getDriveService()
        val query = "name = '$userId' and '$MEALMATE_FOLDER_ID' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false"

        val result: FileList = driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()

        return if (result.files.isNotEmpty()) {
            result.files[0].id
        } else {
            val folderMetadata = File().apply {
                name = userId
                mimeType = "application/vnd.google-apps.folder"
                parents = listOf(MEALMATE_FOLDER_ID)
            }
            val folder = driveService.files().create(folderMetadata)
                .setFields("id")
                .execute()
            folder.id
        }
    }

    fun uploadFileToUserFolder(filePath: java.io.File) {
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

                // Prepare the file metadata for uploading
                val fileMetadata = File().apply {
                    name = filePath.name
                    parents = listOf(userFolderId)
                }

                val mediaContent = FileContent("image/jpeg", filePath)

                val uploadedFile = getDriveService().files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()

                Log.d("GoogleDriveHelper", "File uploaded successfully. File ID: ${uploadedFile.id}")
            } catch (e: Exception) {
                Log.e("GoogleDriveHelper", "Error uploading file: ${e.message}", e)
            }
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

                // Prepare the file metadata for the profile picture upload
                val fileMetadata = File().apply {
                    name = "profile_picture.png"
                    parents = listOf(userFolderId)
                }

                val mediaContent = FileContent("image/png", filePath)

                val uploadedFile = getDriveService().files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()

                Log.d("GoogleDriveHelper", "Profile picture uploaded successfully. File ID: ${uploadedFile.id}")
            } catch (e: Exception) {
                Log.e("GoogleDriveHelper", "Error uploading profile picture: ${e.message}", e)
            }
        }
    }
}
