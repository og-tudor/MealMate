package com.example.mealmate

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.Fragment
import com.example.mealmate.dashboard.DashboardActivity
import com.example.mealmate.dashboard.GeneralFunctions
import com.example.mealmate.utils.AnimationHandler
import com.example.mealmate.utils.GoogleDriveHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var generalFunctions: GeneralFunctions
    private lateinit var animationHandler: AnimationHandler

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize GeneralFunctions
        generalFunctions = GeneralFunctions(requireActivity())

        // Initialize AnimationHandler
        val lottieAnimationView = view.findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.lottie_animation_view)
        animationHandler = AnimationHandler(lottieAnimationView)

        // References to UI elements
        val emailField = view.findViewById<EditText>(R.id.email)
        val passwordField = view.findViewById<EditText>(R.id.password)
        val loginButton = view.findViewById<Button>(R.id.login)
        val registerLink = view.findViewById<View>(R.id.register_link)

        // Set up the login button
        loginButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Snackbar.make(view, "Please enter both email and password", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Show animation while verifying login and folder setup
            animationHandler.showAnimation(Color.parseColor("#E8602E"))

            // Sign in with Firebase Authentication
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        Log.d("LoginFragment", "signInWithEmail:success")
                        verifyUserFolder { success ->
                            animationHandler.hideAnimation() // Hide animation after verification
                            if (success) {
                                navigateToDashboard()
                            } else {
                                Snackbar.make(
                                    view,
                                    "Error setting up user folder. Please try again.",
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }
                        }
                    } else {
                        animationHandler.hideAnimation() // Hide animation on failure
                        Log.w("LoginFragment", "signInWithEmail:failure", task.exception)
                        Snackbar.make(
                            view,
                            "Authentication failed: ${task.exception?.message}",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
        }

        // Navigate to RegisterFragment using GeneralFunctions
        registerLink.setOnClickListener {
            generalFunctions.navigateToFragment(RegisterFragment())
        }

        return view
    }

    private fun verifyUserFolder(callback: (Boolean) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val googleDriveHelper = GoogleDriveHelper(requireContext())
                    val userFolderId = googleDriveHelper.getUserFolderId(currentUser.uid)
                    Log.d("LoginFragment", "User folder verified with ID: $userFolderId")
                    callback(true)
                } catch (e: Exception) {
                    Log.e("LoginFragment", "Error checking/creating user folder: ${e.message}", e)
                    callback(false)
                }
            }
        } else {
            callback(false)
        }
    }

    private fun navigateToDashboard() {
        CoroutineScope(Dispatchers.Main).launch {
            val intent = Intent(requireContext(), DashboardActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
    }
}
