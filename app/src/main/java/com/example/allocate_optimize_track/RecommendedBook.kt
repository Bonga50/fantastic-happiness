package com.example.allocate_optimize_track

data class RecommendedBook(
    val title: String,
    val author: String,
    val amazonLink: String, // Or any other relevant link
    val coverImageUrl: String? = null // Optional: URL to a book cover image
)
