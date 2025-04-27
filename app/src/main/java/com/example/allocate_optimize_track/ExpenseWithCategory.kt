package com.example.allocate_optimize_track

import androidx.room.Embedded
import androidx.room.Relation

data class ExpenseWithCategory(
    // Include all fields from Expense that you need
    @Embedded // Embeds all columns from Expense into this object
    val expense: Expense,

    // Directly map the joined column 'categoryName'
    val categoryName: String
)