package com.example.allocate_optimize_track

import androidx.room.Embedded
import androidx.room.Relation

data class ExpenseWithCategory(
    val expense: Expense,
    val categoryName: String? // Category might not exist if data is inconsistent
)