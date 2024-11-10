import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import com.example.mealmate.R
import com.example.mealmate.dashboard.GeneralFunctions
import com.example.mealmate.login.LoginPage
import com.example.mealmate.utils.GoogleDriveHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class SettingsFragment : Fragment() {

    private lateinit var coverPhotoImage: ImageView
    private var selectedImageUri: Uri? = null
    private val db = FirebaseFirestore.getInstance()
    private lateinit var cardContainer: GridLayout
    private lateinit var generalFunctions: GeneralFunctions
    private lateinit var homeButton: ImageButton
    private lateinit var discoverButton: ImageButton
    private lateinit var settingsButton: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.settings_page, container, false)

        homeButton = view.findViewById(R.id.home_button)
        discoverButton = view.findViewById(R.id.discover_button)
        settingsButton = view.findViewById(R.id.settings_button)
        cardContainer = view.findViewById(R.id.card_container)
        settingsButton.isSelected = true

        generalFunctions = GeneralFunctions(requireActivity(), homeButton, discoverButton, settingsButton)
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

        val uploadButton = view.findViewById<Button>(R.id.upload_settings_button)
        uploadButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Get a drawable resource and create a temporary file in cache directory
                    val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.placeholder)
                    val file = File(requireContext().cacheDir, "placeholder.png")

                    // Write the drawable to the file as a PNG
                    val outputStream = FileOutputStream(file)
                    drawable?.toBitmap()?.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    outputStream.close()

                    // Use the GoogleDriveHelper to upload the file
                    val googleDriveHelper = GoogleDriveHelper(requireContext())
                    googleDriveHelper.uploadSingleFile(file)

                    // Log message to confirm action initiated
                    Log.d("SettingsFragment", "Upload process started for file: ${file.name}")
                } catch (e: Exception) {
                    Log.e("SettingsFragment", "Error preparing file for upload: ${e.message}", e)
                }
            }
        }


        return view
    }
}
