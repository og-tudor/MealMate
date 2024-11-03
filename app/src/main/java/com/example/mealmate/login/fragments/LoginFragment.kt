package com.example.mealmate

import android.content.Intent
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
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment() {
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

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

            // Sign in with Firebase Authentication
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        Log.d("LoginFragment", "signInWithEmail:success")
                        val intent = Intent(requireContext(), DashboardActivity::class.java)
                        startActivity(intent)
                        requireActivity().finish()
                    } else {
                        Log.w("LoginFragment", "signInWithEmail:failure", task.exception)
                        Snackbar.make(
                            view,
                            "Authentication failed: ${task.exception?.message}",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
        }

        // Navigate to RegisterFragment
        registerLink.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }
}
