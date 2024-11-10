package com.example.mealmate

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mealmate.dashboard.GeneralFunctions
import com.example.mealmate.login.LoginPage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.settings_page, container, false)

        // Initialize UI elements
        homeButton = view.findViewById(R.id.home_button)
        discoverButton = view.findViewById(R.id.discover_button)
        settingsButton = view.findViewById(R.id.settings_button)
        cardContainer = view.findViewById(R.id.card_container)
        settingsButton.isSelected = true


        generalFunctions = GeneralFunctions(requireActivity(), homeButton, discoverButton, settingsButton)
        // Set up click listeners for each button
        homeButton.setOnClickListener { generalFunctions.selectButton(homeButton) }
        discoverButton.setOnClickListener { generalFunctions.selectButton(discoverButton) }
        settingsButton.setOnClickListener { generalFunctions.selectButton(settingsButton) }

        // Load the email of the current user
        val currentUser = FirebaseAuth.getInstance().currentUser
        val email = currentUser?.email
        val emailTextView = view.findViewById<TextView>(R.id.settings_email_display)
        emailTextView.text = email

        // logout button
        val logoutButton = view.findViewById<Button>(R.id.logout_button)
        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            // end current activity and start LoginPage activity
            val intent = Intent(activity, LoginPage::class.java)
            startActivity(intent)
            activity?.finish()
        }

        return view
    }
}
