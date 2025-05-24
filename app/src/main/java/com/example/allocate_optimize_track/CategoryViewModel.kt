package com.example.allocate_optimize_track

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class CategoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CategoryRepository() // No DB context needed
    val allCategories: LiveData<List<Category>> = repository.getCategoriesForCurrentUser()
    private val auth = FirebaseAuth.getInstance() // For userId in save

    // LiveData for operation results (optional, for showing toast/loading)
    private val _operationStatus = MutableLiveData<FirebaseResult<Any>>()
    val operationStatus: LiveData<FirebaseResult<Any>> = _operationStatus

    fun insert(category: Category) = viewModelScope.launch {
        _operationStatus.value = FirebaseResult.Loading
        // Category.userId is set in repository
        val result = repository.insert(category)
        _operationStatus.value = result
    }

    fun update(category: Category) = viewModelScope.launch {
        _operationStatus.value = FirebaseResult.Loading
        val result = repository.update(category)
        _operationStatus.value = result
    }

    fun delete(category: Category) = viewModelScope.launch {
        _operationStatus.value = FirebaseResult.Loading
        // Implement full cascade in repo or here by fetching expenses first
        // Current repo version has simplified delete.
        val result = repository.delete(category)
        _operationStatus.value = result
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid // For saving category
}