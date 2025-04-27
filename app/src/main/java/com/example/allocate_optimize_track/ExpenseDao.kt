package com.example.allocate_optimize_track

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE) // Or IGNORE
    suspend fun insert(expense: Expense): Long

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): Expense?

    // Query to get Expenses JOINED with their Category Name for display
    // We order by date descending (most recent first)
    @Transaction // Recommended for queries spanning multiple tables
    @Query("""
        SELECT e.*, c.name as categoryName
        FROM expenses e
        INNER JOIN categories c ON e.categoryId = c.id
        WHERE e.userId = :userId
        ORDER BY e.date DESC
    """)
    fun getExpensesWithCategoryForUser(userId: String): LiveData<List<ExpenseWithCategory>>

    // --- New Query for Category Totals within a Date Range ---
    @Query("""
        SELECT c.id as categoryId, c.name as categoryName, SUM(e.amount) as totalAmount
        FROM expenses e
        INNER JOIN categories c ON e.categoryId = c.id
        WHERE e.userId = :userId AND e.date BETWEEN :startDate AND :endDate
        GROUP BY e.categoryId, c.name
        ORDER BY totalAmount DESC
    """)
    fun getCategoryTotalsForUserAndDateRange(userId: String, startDate: Long, endDate: Long): LiveData<List<CategoryTotal>>

    // --- Query for Grand Total within a Date Range ---
    @Query("""
        SELECT SUM(e.amount)
        FROM expenses e
        WHERE e.userId = :userId AND e.date BETWEEN :startDate AND :endDate
    """)
    fun getExpensesTotalForUserAndDateRange(userId: String, startDate: Long, endDate: Long): LiveData<Double?> // Nullable Double
}