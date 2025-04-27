package com.example.allocate_optimize_track

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class CategoryViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth

    // --- Initialize repository FIRST ---
    private val repository: CategoryRepository by lazy {
        val categoryDao = AppDatabase.getDatabase(application).categoryDao()
        CategoryRepository(categoryDao)
    }

    // LiveData to hold the current user's ID (email)
    private val _currentUserId = MutableLiveData<String?>()

    // --- Initialize allCategories AFTER repository ---
    val allCategories: LiveData<List<Category>> = _currentUserId.switchMap { userId ->
        // The logic inside the lambda remains the same
        if (userId != null) {
            repository.getCategoriesForUser(userId)
        } else {
            MutableLiveData(emptyList())
        }
    }

    init {

        // Set the initial user ID
        _currentUserId.value = auth.currentUser?.email
    }

    // --- ViewModel Actions ---

    // Launching a new coroutine to insert the data in a non-blocking way
    fun insert(category: Category) = viewModelScope.launch {
        // Add check: Only insert if the category's user matches the current user
        if (category.userId == _currentUserId.value) {
            repository.insert(category)
        } else {
            Log.e("CategoryViewModel", "Attempted to insert category with mismatched userId.")
            // Optionally show an error to the user via another LiveData
        }
    }

    fun update(category: Category) = viewModelScope.launch {
        // Add check: Only update if the category's user matches the current user
        if (category.userId == _currentUserId.value) {
            repository.update(category)
        } else {
            Log.e("CategoryViewModel", "Attempted to update category with mismatched userId.")
        }
    }

    fun delete(category: Category) = viewModelScope.launch {
        // Add check: Only delete if the category's user matches the current user
        if (category.userId == _currentUserId.value) {
            repository.delete(category)
        } else {
            Log.e("CategoryViewModel", "Attempted to delete category with mismatched userId.")
        }
    }

    fun deleteById(id: Long) = viewModelScope.launch {
        // Safer approach: Fetch first to verify user ID before deleting
        val categoryToDelete = repository.getCategoryById(id)
        if (categoryToDelete != null && categoryToDelete.userId == _currentUserId.value) {
            repository.deleteById(id) // Or repository.delete(categoryToDelete)
        } else {
            Log.e("CategoryViewModel", "Attempted to delete category by ID with mismatched or unknown userId.")
        }
    }

    suspend fun getCategoryById(id: Long): Category? {
        // Consider adding a check here too, depending on usage
        val category = repository.getCategoryById(id)
        return if (category?.userId == _currentUserId.value) {
            category
        } else {
            // Return null if the fetched category doesn't belong to the current user
            Log.w("CategoryViewModel", "getCategoryById requested for category not belonging to current user.")
            null
        }
    }
}