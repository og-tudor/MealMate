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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.mealmate.dashboard.DashboardActivity
import com.example.mealmate.dashboard.GeneralFunctions
import com.example.mealmate.repository.CategoriesRepository
import com.example.mealmate.utils.AnimationHandler
import com.example.mealmate.utils.GoogleDriveHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var generalFunctions: GeneralFunctions
    private lateinit var animationHandler: AnimationHandler

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)
        initializeComponents(view)
        setupLoginButton(view)
        setupRegisterLink(view)
        return view
    }

    private fun initializeComponents(view: View) {
        auth = FirebaseAuth.getInstance()
        generalFunctions = GeneralFunctions(requireActivity())

        val lottieAnimationView = view.findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.lottie_animation_view)
        animationHandler = AnimationHandler(lottieAnimationView)
    }

    private fun setupLoginButton(view: View) {
        val emailField = view.findViewById<EditText>(R.id.email)
        val passwordField = view.findViewById<EditText>(R.id.password)
        val loginButton = view.findViewById<Button>(R.id.login)

        loginButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Snackbar.make(view, "Please enter both email and password", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            animationHandler.showAnimation(Color.parseColor("#E8602E"))
            loginUser(email, password, view)
        }
    }

    private fun loginUser(email: String, password: String, view: View) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Log.d("LoginFragment", "signInWithEmail:success")
                    verifyUserFolder { success ->
                        if (success) {
                            loadCategoriesAndNavigate(view)
                        } else {
                            showErrorSnackbar(view, "Error setting up user folder. Please try again.")
                            animationHandler.hideAnimation()
                        }
                    }
                } else {
                    handleLoginFailure(task.exception, view)
                }
            }
    }

    private fun loadCategoriesAndNavigate(view: View) {
        CategoriesRepository.loadCategories(requireContext()) { success ->
            if (success) {
                Log.d("LoginFragment", "Categories loaded successfully")
                animationHandler.hideAnimation()
                val intent = Intent(requireContext(), DashboardActivity::class.java)
                startActivity(intent)
                activity?.finish() // Ensure we don't return to login on back
            } else {
                showErrorSnackbar(view, "Failed to load categories. Please try again.")
                animationHandler.hideAnimation()
            }
        }
    }


    private fun handleLoginFailure(exception: Exception?, view: View) {
        animationHandler.hideAnimation()
        Log.w("LoginFragment", "signInWithEmail:failure", exception)
        Snackbar.make(
            view,
            "Authentication failed: ${exception?.message}",
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun verifyUserFolder(callback: (Boolean) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val googleDriveHelper = GoogleDriveHelper(requireContext())
                    val userFolderId = googleDriveHelper.getUserFolderId(currentUser.uid)
                    if (userFolderId != null) {
                        Log.d("LoginFragment", "User folder verified with ID: $userFolderId")
                        callback(true)
                    } else {
                        Log.e("LoginFragment", "User folder ID is null")
                        callback(false)
                    }
                } catch (e: Exception) {
                    Log.e("LoginFragment", "Error checking/creating user folder: ${e.message}", e)
                    callback(false)
                }
            }
        } else {
            Log.e("LoginFragment", "No authenticated user found")
            callback(false)
        }
    }

    private fun showErrorSnackbar(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
    }

    private fun setupRegisterLink(view: View) {
        val registerLink = view.findViewById<View>(R.id.register_link)
        registerLink.setOnClickListener {
            generalFunctions.navigateToFragment(RegisterFragment())
        }
    }
}
