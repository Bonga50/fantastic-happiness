package com.example.allocate_optimize_track

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.example.allocate_optimize_track.databinding.ActivityDashboardBinding
import com.example.allocate_optimize_track.ui.LoginActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var navController: NavController // Declare NavController
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Ensure this works as intended with your theme/layout
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle window insets (keep this if needed for edge-to-edge)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets -> // Use binding.main if that's your root ID
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = Firebase.auth // Initialize Firebase Auth

        // --- Setup Navigation Component ---

        // 1. Find the NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment // Use the ID from your layout

        // 2. Get the NavController from the NavHostFragment *BEFORE* using it
        navController = navHostFragment.navController

        // 3. Connect BottomNavigationView to NavController using NavigationUI
        // This automatically handles navigation when bottom nav items are clicked,
        // IF the menu item IDs (e.g., R.id.nav_home) match the
        // fragment destination IDs (e.g., android:id="@+id/homeFragment") in nav_graph.xml
        binding.bottomNavigation.setupWithNavController(navController)

        // --- Optional: Customize Listener for specific actions like Logout ---
        // setupWithNavController sets its own listener. If you need custom behavior
        // like logout, set your listener *after* setupWithNavController or handle
        // it carefully within the listener by also calling NavigationUI.onNavDestinationSelected

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_logout -> {
                    performLogout()
                    // Return false because we are leaving the activity, don't
                    // let the nav component try to navigate or select the item.
                    return@setOnItemSelectedListener false
                }
                // For all other menu items, let NavigationUI handle the navigation based on the graph.
                else -> {
                    // Use NavigationUI to navigate based on the selected item ID and the NavController.
                    // This ensures the Nav Graph is respected.
                    return@setOnItemSelectedListener NavigationUI.onNavDestinationSelected(item, navController)
                }
            }
            // Fallback, should ideally not be reached if all cases are handled
            // false
        }

        // --- Remove Manual Fragment Transaction Logic ---
        // The 'replaceFragment' function and the code creating fragment instances
        // in the listener are no longer needed because Navigation Component handles it.
    }

    // Keep your performLogout function as is
    private fun performLogout() {
        auth.signOut()
        // Go back to Login Screen
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    // Remove the replaceFragment function if you had one defined here.
    // private fun replaceFragment(fragment: Fragment) { ... } // DELETE THIS
}