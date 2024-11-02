package com.example.mealmate

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class LoginPage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)
        this.enableEdgeToEdge()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()

        // Reference the main view
        val mainView = findViewById<View>(R.id.main)
        if (mainView == null) {
            Log.e("LoginPage", "Error: View with ID 'main' is null. Check your layout file.")
            return
        }

        // Apply window insets
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // References to the email and password fields
        val emailField = findViewById<EditText>(R.id.email)
        val passwordField = findViewById<EditText>(R.id.password)
        val loginButton = findViewById<Button>(R.id.login)
        val registerLink = findViewById<TextView>(R.id.register_link)

        // Set up the login button
        loginButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Snackbar.make(mainView, "Please enter both email and password", Snackbar.LENGTH_LONG)
                    .show()
                return@setOnClickListener
            }

            // Sign in with Firebase Authentication
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d("LoginPage", "signInWithEmail:success")
                        val intent = Intent(this@LoginPage, SavedRecipiesPage::class.java)
                        startActivity(intent)
                        finish() // Close the login activity
                    } else {
                        Log.w("LoginPage", "signInWithEmail:failure", task.exception)
                        Snackbar.make(
                            mainView,
                            "Authentication failed: ${task.exception?.message}",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
        }


    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        var currentUser = auth.currentUser
        currentUser = null;
        if (currentUser != null) {
            val intent = Intent(this@LoginPage, SavedRecipiesPage::class.java)
            startActivity(intent)
            finish() // Close the login activity
        }
    }
}
