package com.example.allocate_optimize_track

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao // Room annotation
interface CategoryDao {

    // --- Create ---
    @Insert(onConflict = OnConflictStrategy.IGNORE) // Ignore if category with same ID exists (shouldn't happen with autoGenerate)
    suspend fun insert(category: Category): Long // Use suspend for coroutines

    // --- Read ---
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): LiveData<List<Category>> // Use LiveData for observable queries

    @Query("SELECT * FROM categories WHERE userId = :id ORDER BY name ASC")
    fun getAllCategoriesByUserId(id: String): LiveData<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category? // Suspend function for single fetch

    // --- Update ---
    @Update
    suspend fun update(category: Category)

    // --- Delete ---
    @Delete
    suspend fun delete(category: Category)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)
}