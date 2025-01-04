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
import com.example.mealmate.repository.CategoriesRepository
import com.example.mealmate.utils.AnimationHandler
import com.example.mealmate.utils.GoogleDriveHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var generalFunctions: GeneralFunctions
    private lateinit var animationHandler: AnimationHandler
    private lateinit var googleSignInClient: GoogleSignInClient

    private val RC_SIGN_IN = 9001

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

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        setupGoogleSignInButton(view)
    }

    private fun setupGoogleSignInButton(view: View) {
        val googleSignInButton = view.findViewById<View>(R.id.google_icon)
        googleSignInButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: com.google.android.gms.tasks.Task<GoogleSignInAccount>) {
        try {
            animationHandler.showAnimation(Color.parseColor("#E8602E")) // Show loading animation
            val account = completedTask.getResult(ApiException::class.java)!!
            Log.d("LoginFragment", "firebaseAuthWithGoogle: ${account.id}")
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            animationHandler.hideAnimation() // Hide animation on failure
            Log.w("LoginFragment", "Google sign-in failed", e)
            Snackbar.make(requireView(), "Google Sign-In failed: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Log.d("LoginFragment", "signInWithCredential:success")
                    handleSuccessfulLogin(requireView())
                } else {
                    animationHandler.hideAnimation() // Hide animation on failure
                    Log.w("LoginFragment", "signInWithCredential:failure", task.exception)
                    Snackbar.make(requireView(), "Authentication Failed.", Snackbar.LENGTH_SHORT).show()
                }
            }
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
                    handleSuccessfulLogin(view)
                } else {
                    handleLoginFailure(task.exception, view)
                }
            }
    }

    private fun handleSuccessfulLogin(view: View) {
        verifyUserFolder { success ->
            if (success) {
                CategoriesRepository.clearCachedCategories() // Clear any leftover cache
                CategoriesRepository.isDataLoaded = false // Reset the flag
                loadCategoriesAndNavigate(view) // Load new categories and navigate
            } else {
                showErrorSnackbar(view, "Error setting up user folder. Please try again.")
                animationHandler.hideAnimation()
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
                activity?.finish()
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
