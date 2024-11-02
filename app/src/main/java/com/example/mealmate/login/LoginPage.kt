package com.example.mealmate.login

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mealmate.LoginFragment
import com.example.mealmate.R

class LoginPage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_page)

        // Load the LoginFragment by default
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .commit()
        }
    }
}
