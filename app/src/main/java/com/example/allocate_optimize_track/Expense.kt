package com.example.allocate_optimize_track

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    // Define foreign key relationship with Categories
    foreignKeys = [ForeignKey(
        entity = Category::class,
        parentColumns = ["id"], // Column in the Category table
        childColumns = ["categoryId"], // Column in the Expense table
        onDelete = ForeignKey.RESTRICT // Prevent deleting category if expenses exist (or use CASCADE, SET_NULL)
    )],
    // Index categoryId for faster lookups
    indices = [Index("categoryId"), Index("userId")]
)
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val userId: String, // Foreign key to user (Firebase email)
    val categoryId: Long, // Foreign key to Category table

    val amount: Double,
    val date: Long, // Store as timestamp (milliseconds since epoch)
    val description: String,
    val photoUri: String? = null // Nullable String to store URI of the attached photo
)