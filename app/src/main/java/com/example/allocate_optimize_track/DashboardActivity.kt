package com.example.allocate_optimize_track

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.allocate_optimize_track.databinding.ActivityDashboardBinding
import com.example.allocate_optimize_track.ui.LoginActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth = Firebase.auth

        // Get the email passed from Login/SignUp Activity
        val userEmail = intent.getStringExtra("USER_EMAIL")

        // Display the email
        binding.textViewUserEmail.text = userEmail ?: "Email not found"

        // Logout Button Click
        binding.buttonLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun performLogout() {
        auth.signOut()
        // Go back to Login Screen
        val intent = Intent(this, LoginActivity::class.java).apply {
            // Ensure the user cannot go back to Welcome screen after logout
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish() // Finish WelcomeActivity
    }
}