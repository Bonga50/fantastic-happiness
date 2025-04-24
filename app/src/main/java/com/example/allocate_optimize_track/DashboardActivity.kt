package com.example.allocate_optimize_track

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
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

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            var selectedFragment: Fragment? = null

            when (item.itemId) {
                R.id.nav_home -> {
                    selectedFragment = HomeFragment()
                }
                R.id.nav_categories -> {
                    selectedFragment = CategoriesFragment()
                }
                R.id.nav_expenses -> {
                    selectedFragment = ExpensesFragment()
                }
                R.id.nav_logout -> {
                    performLogout()
                    return@setOnItemSelectedListener false // Don't select logout visually, we are leaving
                }
            }

            // Replace the fragment if a valid one was selected
            if (selectedFragment != null) {
                replaceFragment(selectedFragment)
                return@setOnItemSelectedListener true // Indicate selection was handled
            }

            false // Default: Indicate selection was not handled (shouldn't happen here)
        }

        // Load the default fragment (Home) when the activity is created
        if (savedInstanceState == null) { // Only load default if not restoring state
            binding.bottomNavigation.selectedItemId = R.id.nav_home // Set default selection
            replaceFragment(HomeFragment())
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment) // Use the ID of your FragmentContainerView
            // .addToBackStack(null) // Optional: Add transaction to back stack
            .commit()
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