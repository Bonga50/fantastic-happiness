package com.example.allocate_optimize_track



data class Expense(
    var id: String = "", // Firebase push key or UUID
    var userId: String = "",
    var categoryId: String = "", // ID of the Category
    var amount: Double = 0.0,
    var date: Long = 0L,
    var description: String = "",
    var photoStoragePath: String? = null // Path in Supabase Storage
) {
    constructor() : this("", "", "", 0.0, 0L, "", null)
}