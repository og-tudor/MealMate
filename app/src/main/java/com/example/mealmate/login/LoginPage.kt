package com.example.mealmate.login

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mealmate.LoginFragment
import com.example.mealmate.R
import com.example.mealmate.dashboard.GeneralFunctions

class LoginPage : AppCompatActivity() {

    private lateinit var generalFunctions: GeneralFunctions

    companion object {
        private const val POST_NOTIFICATIONS_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_page)

        // Soliciti permisiunea de a trimite notificări pe Android 13+ (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkPostNotificationPermission()
        }

        // Initialize GeneralFunctions without buttons
        generalFunctions = GeneralFunctions(this)

        // Load the LoginFragment by default
        if (savedInstanceState == null) {
            generalFunctions.navigateToFragment(LoginFragment())
        }
    }

    private fun checkPostNotificationPermission() {
        val permissionStatus = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        )
        if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                POST_NOTIFICATIONS_REQUEST_CODE
            )
        }
    }

    // Dacă vrei să verifici răspunsul userului
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == POST_NOTIFICATIONS_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // OK, permisiunea a fost acordată
            } else {
                // User-ul a refuzat permisiunea
            }
        }
    }
}