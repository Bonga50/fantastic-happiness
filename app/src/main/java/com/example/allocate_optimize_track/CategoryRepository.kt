package com.example.allocate_optimize_track

import androidx.lifecycle.LiveData
import com.google.firebase.auth.FirebaseAuth

class CategoryRepository(private val categoryDao: CategoryDao) {

    private lateinit var auth: FirebaseAuth
    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    //val allCategories: LiveData<List<Category>> = categoryDao.getAllCategoriesByUserId()

    // By default Room runs suspend queries off the main thread, therefore, we don't need to
    // implement anything else to ensure we're not doing long running database work
    // off the main thread.

    fun getCategoriesForUser(userId: String): LiveData<List<Category>> {
        return categoryDao.getAllCategoriesByUserId(userId)
    }

    suspend fun insert(category: Category): Long {
        return categoryDao.insert(category)
    }

    suspend fun update(category: Category) {
        categoryDao.update(category)
    }

    suspend fun delete(category: Category) {
        categoryDao.delete(category)
    }

    suspend fun deleteById(id: Long) {
        categoryDao.deleteById(id)
    }

    suspend fun getCategoryById(id: Long): Category? {
        return categoryDao.getCategoryById(id)
    }
}