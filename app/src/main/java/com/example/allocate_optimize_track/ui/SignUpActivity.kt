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
import com.example.allocate_optimize_track.databinding.ActivitySignUpBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Sign Up Button Click
        binding.buttonSignUp.setOnClickListener {
            performSignUp()
        }

        // Go to Login Screen Text Click
        binding.textViewGoToLogin.setOnClickListener {
            // Simply finish this activity to go back to LoginActivity
            // Assumes LoginActivity was not finished when starting SignUpActivity
            // If LoginActivity might be finished, use:
            // val intent = Intent(this, LoginActivity::class.java)
            // startActivity(intent)
            finish()
        }
    }

    private fun performSignUp() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

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

        if (password.length < 6) {
            binding.etPassword.error = "Password must be at least 6 characters"
            binding.etPassword.requestFocus()
            return
        }

        if (confirmPassword.isEmpty()) {
            binding.etConfirmPassword.error = "Confirm Password is required"
            binding.etConfirmPassword.requestFocus()
            return
        }

        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Passwords do not match"
            binding.etConfirmPassword.requestFocus()
            // Also clear the confirm password field potentially
            // binding.etConfirmPassword.text.clear()
            return
        }
        // --- End Validation ---

        setLoading(true) // Show progress bar

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                setLoading(false) // Hide progress bar
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    // Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    Toast.makeText(baseContext, "Sign up successful.", Toast.LENGTH_SHORT).show()
                    navigateToWelcome(user?.email) // Go to welcome screen after sign up
                } else {
                    // If sign in fails, display a message to the user.
                    // Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Sign up failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG).show() // Show specific Firebase error
                }
            }
    }

    // Re-use the same navigation logic
    private fun navigateToWelcome(userEmail: String?) {
        val intent = Intent(this, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("USER_EMAIL", userEmail ?: "Unknown User")
        }
        startActivity(intent)
        finish() // Finish SignUpActivity
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBarSignUp.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonSignUp.isEnabled = !isLoading
        binding.textViewGoToLogin.isEnabled = !isLoading
        // Optionally disable text fields too
        // binding.etEmail.isEnabled = !isLoading
        // binding.etPassword.isEnabled = !isLoading
        // binding.etConfirmPassword.isEnabled = !isLoading
    }
}