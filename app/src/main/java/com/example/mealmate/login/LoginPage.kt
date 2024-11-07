package com.example.mealmate.login

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mealmate.LoginFragment
import com.example.mealmate.R
import com.example.mealmate.dashboard.GeneralFunctions

class LoginPage : AppCompatActivity() {
    private lateinit var generalFunctions: GeneralFunctions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_page)

        // Initialize GeneralFunctions without buttons
        generalFunctions = GeneralFunctions(this)

        // Load the LoginFragment by default
        if (savedInstanceState == null) {
            generalFunctions.navigateToFragment(LoginFragment())
        }
    }
}
