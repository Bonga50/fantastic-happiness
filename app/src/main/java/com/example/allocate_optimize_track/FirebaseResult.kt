package com.example.allocate_optimize_track

sealed class FirebaseResult<out T> {
    data class Success<out T>(val data: T) : FirebaseResult<T>()
    data class Failure(val exception: Exception) : FirebaseResult<Nothing>()
    object Loading : FirebaseResult<Nothing>() // Optional
}