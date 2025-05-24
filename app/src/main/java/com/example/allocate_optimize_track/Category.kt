package com.example.allocate_optimize_track

data class Category(
    var id: String = "", // Will be Firebase push key or UUID
    var createdAt: Long = System.currentTimeMillis(),
    var name: String = "",
    var userId: String = "", // Store Firebase Auth UID
    var description: String? = null,
    var monthlyLimit: Double? = null,
    var monthlyGoal: Double? = null,

    // Example: if you had a derived field for display, exclude it
    // @get:Exclude
    // var isOverLimit: Boolean = false
) {
    // Add a no-argument constructor for Firebase deserialization
    constructor() : this("", 0L, "", "", null, null, null)
}
