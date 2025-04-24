package com.example.allocate_optimize_track.ui

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.allocate_optimize_track.DashboardActivity
import com.example.allocate_optimize_track.R
import com.example.allocate_optimize_track.databinding.ActivityLoginBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class LoginActivity : AppCompatActivity() {


    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Check if user is already logged in
        checkCurrentUser()

        // Login Button Click
        binding.buttonLogin.setOnClickListener {
            performLogin()
        }

        // Go to Register Screen Text Click
        binding.textViewGoToRegister.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            // Optional: finish() // if you don't want user to come back to login from sign up via back button
        }
    }

    private fun checkCurrentUser() {
        // If user is already logged in, go directly to Welcome screen
        if (auth.currentUser != null) {
            navigateToWelcome(auth.currentUser?.email)
        }
    }


    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // --- Validation ---
        if (email.isEmpty()) {
            binding.etEmail.error = "Email is required"
            binding.etEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Enter a valid email"
            binding.etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Password is required"
            binding.etPassword.requestFocus()
            return
        }
        // Firebase requires password >= 6 chars, but validation here is optional
         if (password.length < 6) {
             binding.etPassword.error = "Password must be at least 6 characters"
             binding.etPassword.requestFocus()
             return
         }
        // --- End Validation ---

        setLoading(true) // Show progress bar

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                setLoading(false) // Hide progress bar
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    // Log.d(TAG, "signInWithEmail:success") // Optional logging
                    val user = auth.currentUser
                    Toast.makeText(baseContext, "Login Successful.", Toast.LENGTH_SHORT).show()
                    navigateToWelcome(user?.email)
                } else {
                    // If sign in fails, display a message to the user.
                    // Log.w(TAG, "signInWithEmail:failure", task.exception) // Optional logging
                    Toast.makeText(baseContext, "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG).show() // Show specific Firebase error
                }
            }
    }

    private fun navigateToWelcome(userEmail: String?) {
        val intent = Intent(this, DashboardActivity::class.java).apply {
            // Clear the back stack to prevent going back to Login/SignUp
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("USER_EMAIL", userEmail ?: "Unknown User") // Pass email to WelcomeActivity
        }
        startActivity(intent)
        finish() // Finish LoginActivity
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBarLogin.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonLogin.isEnabled = !isLoading
        binding.textViewGoToRegister.isEnabled = !isLoading
        // Optionally disable text fields too
        // binding.editTextLoginEmail.isEnabled = !isLoading
        // binding.editTextLoginPassword.isEnabled = !isLoading
    }
}