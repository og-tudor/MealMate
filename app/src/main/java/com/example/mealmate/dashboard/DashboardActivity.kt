package com.example.mealmate.dashboard

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.mealmate.R
import com.example.mealmate.dashboard.home.SavedCategoriesFragment

class DashboardActivity : AppCompatActivity() {
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private var onImageSelected: ((Bitmap) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize the gallery launcher
        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val selectedImageUri = result.data?.data
                if (selectedImageUri != null) {
                    val bitmap = getBitmapFromUri(selectedImageUri)
                    if (bitmap != null) {
                        onImageSelected?.invoke(bitmap)
                    }
                } else {
                    Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Load the default fragment
        if (savedInstanceState == null) {
            loadFragment(SavedCategoriesFragment())
        }
    }

    // Open the gallery and pass a callback for the selected image
    fun openGallery(onImageSelected: (Bitmap) -> Unit) {
        this.onImageSelected = onImageSelected
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        galleryLauncher.launch(intent)
    }

    // Helper function to convert URI to Bitmap
    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
