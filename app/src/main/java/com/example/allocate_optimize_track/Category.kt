package com.example.allocate_optimize_track

import androidx.room.PrimaryKey
import androidx.room.Entity
import java.util.Date // Using java.util.Date for simplicity, or use Long for timestamp

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) // Auto-incrementing ID
    val id: Long = 0, // Use Long for IDs

    val createdAt: Long = System.currentTimeMillis(), // Store creation time as milliseconds

    val name: String,
    val userId: String,

    val description: String?, // Nullable description

    val monthlyLimit: Double?, // Nullable monthly limit (use Double or BigDecimal)
    val monthlyGoal: Double?
)
