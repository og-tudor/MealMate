package com.example.mealmate

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.mealmate.dashboard.GeneralFunctions
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class RegisterFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var generalFunctions: GeneralFunctions

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register, container, false)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize GeneralFunctions
        generalFunctions = GeneralFunctions(requireActivity())

        // References to UI elements
        val emailField = view.findViewById<EditText>(R.id.email)
        val passwordField = view.findViewById<EditText>(R.id.password)
        val passwordConfirmationField = view.findViewById<EditText>(R.id.password_confirmation)
        val registerButton = view.findViewById<MaterialButton>(R.id.registerButton)

        // Set up the register button
        registerButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()
            val passwordConfirmation = passwordConfirmationField.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Snackbar.make(view, "Please enter both email and password", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (password != passwordConfirmation) {
                Snackbar.make(view, "Passwords do not match", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Register the user with Firebase Authentication
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        Log.d("RegisterFragment", "createUserWithEmail:success")
                        Snackbar.make(view, "Registration successful!", Snackbar.LENGTH_LONG).show()

                        // Navigate back to LoginFragment using GeneralFunctions
                        generalFunctions.navigateToFragment(LoginFragment())
                    } else {
                        Log.w("RegisterFragment", "createUserWithEmail:failure", task.exception)
                        Snackbar.make(
                            view,
                            "Registration failed: ${task.exception?.message}",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
        }

        return view
    }
}
