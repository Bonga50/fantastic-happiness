package com.example.allocate_optimize_track

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage

class SupabaseClientApplication: Application() {
    companion object {
        lateinit var supabaseClient: SupabaseClient
            private set
    }

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        // Initialize Supabase Client CORRECTLY
        supabaseClient = createSupabaseClient(
            supabaseUrl = getString(R.string.YOUR_SUPABASE_URL), // Use getString()
            supabaseKey = getString(R.string.YOUR_SUPABASE_ANON_KEY)  // Use getString()
        ) {
            install(Storage)
        }
        Log.d("SupabaseInit", "Supabase URL: ${getString(R.string.YOUR_SUPABASE_URL)}") // Add log to verify
        Log.d("SupabaseInit", "Supabase Key: ${getString(R.string.YOUR_SUPABASE_ANON_KEY)}") // Add log to verify
    }
}