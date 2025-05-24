package com.example.allocate_optimize_track


import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import io.ktor.client.engine.cio.*
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID


object SupabaseImageService {
    // Ktor HTTP Client
    private val httpClient = HttpClient(CIO) { // Or Android client engine
        install(ContentNegotiation) {
            json() // Default kotlinx.serialization.json
        }
        // Add default request headers if needed, e.g., API key if your function is protected by one
        // (though here we use Firebase token)
    }

    // Your Supabase Function URL - get this from your Supabase dashboard after deploying
    // It's better to store this in a config file or string resources
    private const val UPLOAD_FUNCTION_URL = "https://xfbqyvvbzegyosjqqzpb.supabase.co/functions/v1/upload-receipt"
    // Example: "https://<project-ref>.supabase.co/functions/v1/upload-receipt"


    suspend fun uploadReceiptImageViaFunction(
        firebaseUserId: String, // Pass Firebase UID
        expenseId: String,
        imageUri: Uri,
        contentResolver: ContentResolver
    ): String? { // Returns storagePath or null on failure
        val firebaseIdToken = try {
            Firebase.auth.currentUser?.getIdToken(false)?.await()?.token
        } catch (e: Exception) {
            Log.e("SupabaseImageService", "Failed to get Firebase ID token", e)
            null
        }

        if (firebaseIdToken == null) {
            Log.e("SupabaseImageService", "Firebase ID Token is null. Cannot upload.")
            return null
        }

        Log.d("SupabaseImageService", "Attempting to upload image for user: $firebaseUserId, expense: $expenseId")

        return try {
            val fileBytes = contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
            if (fileBytes == null) {
                Log.e("SupabaseImageService", "Could not read bytes from image URI.")
                return null
            }

            val originalFileName = imageUri.lastPathSegment ?: "receipt.jpg" // Get a filename

            val response: UploadResponse = httpClient.post(UPLOAD_FUNCTION_URL) {
                // Set Firebase ID Token in Authorization header
                header(HttpHeaders.Authorization, "Bearer $firebaseIdToken")
                // Set custom headers (must match what Edge Function expects)
                header("X-User-Id", firebaseUserId)
                header("X-Expense-Id", expenseId)

                // Send as multipart form data
                setBody(
                    MultiPartFormDataContent(
                    formData {
                        append("file", fileBytes, Headers.build {
                            append(HttpHeaders.ContentType, contentResolver.getType(imageUri) ?: "image/jpeg")
                            append(HttpHeaders.ContentDisposition, "filename=\"$originalFileName\"")
                        })
                    }
                ))
            }.body() // Deserialize JSON response to UploadResponse

            Log.i("SupabaseImageService", "Upload successful. Path: ${response.storagePath}")
            response.storagePath // Return the storage path from the function's response
        } catch (e: Exception) {
            Log.e("SupabaseImageService", "Error uploading image via Edge Function", e)
            e.printStackTrace()
            null
        }
    }

    // Delete still uses the Supabase client directly, but RLS must allow it for anon
    // OR you create a delete Edge Function similar to upload.
    // For now, assuming your "Allow anonymous deletes from user paths" RLS is in place
    // and your client uses the anon key for the Supabase client.
    suspend fun deleteReceiptImage(storagePath: String): Boolean {
        Log.d("SupabaseImageService", "Attempting to delete image at path: $storagePath")
        return try {
            val client = SupabaseClientApplication.supabaseClient // Get client instance
            client.storage["receipt-images"].delete(listOf(storagePath))
            Log.i("SupabaseImageService", "Image deletion successful for path: $storagePath")
            true
        } catch (e: Exception) {
            Log.e("SupabaseImageService", "Error deleting image from Supabase Storage", e)
            e.printStackTrace()
            false
        }
    }

    // Get public URL still uses Supabase client directly
    fun getImageUrl(storagePath: String): String {
        Log.d("SupabaseImageService", "Getting public URL for path: $storagePath")
        val client = SupabaseClientApplication.supabaseClient
        return client.storage["receipt-images"].publicUrl(storagePath)
    }
}