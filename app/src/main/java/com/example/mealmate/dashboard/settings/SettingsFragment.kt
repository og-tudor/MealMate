import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mealmate.R
import com.example.mealmate.dashboard.GeneralFunctions
import com.example.mealmate.login.LoginPage
import com.example.mealmate.utils.GoogleDriveHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class SettingsFragment : Fragment() {

    private lateinit var coverPhotoImage: ImageView
    private lateinit var profilePicture: ImageView
    private lateinit var homeButton: ImageButton
    private lateinit var discoverButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.settings_page, container, false)

        homeButton = view.findViewById(R.id.home_button)
        discoverButton = view.findViewById(R.id.discover_button)
        settingsButton = view.findViewById(R.id.settings_button)
        profilePicture = view.findViewById(R.id.profile_picture)
        settingsButton.isSelected = true

        val generalFunctions = GeneralFunctions(requireActivity(), homeButton, discoverButton, settingsButton)
        homeButton.setOnClickListener { generalFunctions.selectButton(homeButton) }
        discoverButton.setOnClickListener { generalFunctions.selectButton(discoverButton) }
        settingsButton.setOnClickListener { generalFunctions.selectButton(settingsButton) }

        val currentUser = FirebaseAuth.getInstance().currentUser
        val email = currentUser?.email
        val emailTextView = view.findViewById<TextView>(R.id.settings_email_display)
        emailTextView.text = email

        val logoutButton = view.findViewById<Button>(R.id.logout_button)
        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(activity, LoginPage::class.java)
            startActivity(intent)
            activity?.finish()
        }

        // Load profile picture using caching
        currentUser?.let {
            loadProfilePicture(it.uid)
        }

        // Set profile picture click listener to open the gallery
        profilePicture.setOnClickListener {
            openGallery()
        }

        return view
    }

    private fun loadProfilePicture(userId: String) {
        val googleDriveHelper = GoogleDriveHelper(requireContext())

        // Check if profile picture is already cached
        val cachedBitmap = googleDriveHelper.getCachedProfilePicture(userId)
        if (cachedBitmap != null) {
            // Set the cached image on the main thread
            profilePicture.post {
                profilePicture.setImageBitmap(cachedBitmap)
            }
        } else {
            // Fetch from Drive if not cached
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val bitmap: Bitmap? = googleDriveHelper.getUserProfilePicture(userId)
                    if (bitmap != null) {
                        // Set the image and cache it
                        profilePicture.setImageBitmap(bitmap)
                        googleDriveHelper.cacheProfilePicture(bitmap, userId)
                    } else {
                        Log.d("SettingsFragment", "No profile picture found, using default.")
                    }
                } catch (e: Exception) {
                    Log.e("SettingsFragment", "Error loading profile picture: ${e.message}", e)
                }
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val selectedImageUri: Uri = data.data!!
            try {
                val inputStream: InputStream? = requireContext().contentResolver.openInputStream(selectedImageUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)

                // Save the selected image to cache
                val cacheFile = File(requireContext().cacheDir, "selected_profile_picture.png")
                FileOutputStream(cacheFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }

                // Upload the selected image to Google Drive
                val googleDriveHelper = GoogleDriveHelper(requireContext())
                CoroutineScope(Dispatchers.IO).launch {
                    googleDriveHelper.uploadUserProfilePicture(cacheFile)
                    googleDriveHelper.cacheProfilePicture(bitmap, FirebaseAuth.getInstance().currentUser!!.uid)

                    // Reload the fragment on the main thread
                    CoroutineScope(Dispatchers.Main).launch {
                        loadProfilePicture(FirebaseAuth.getInstance().currentUser!!.uid)
                    }
                }

            } catch (e: Exception) {
                Log.e("SettingsFragment", "Error handling selected image: ${e.message}", e)
            }
        }
    }
}
