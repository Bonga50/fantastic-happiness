package com.example.allocate_optimize_track

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import java.util.Calendar

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth
    val grandTotal: MediatorLiveData<Double?> = MediatorLiveData()
    // Repositories using lazy initialization
    private val expenseRepository: ExpenseRepository by lazy {
        val expenseDao = AppDatabase.getDatabase(application).expenseDao()
        ExpenseRepository(expenseDao)
    }
    private val categoryRepository: CategoryRepository by lazy { // Added Category Repository
        val categoryDao = AppDatabase.getDatabase(application).categoryDao()
        CategoryRepository(categoryDao)
    }

    private val _startDateMillis = MutableLiveData<Long>()
    val startDateMillis: LiveData<Long> get() = _startDateMillis // Expose immutable LiveData

    private val _endDateMillis = MutableLiveData<Long>()
    val endDateMillis: LiveData<Long> get() = _endDateMillis

    // LiveData for current user ID
    private val _currentUserId = MutableLiveData<String?>()

    // --- LiveData for displaying Category Totals (using MediatorLiveData) ---
    val categoryTotals: MediatorLiveData<List<CategoryTotal>> = MediatorLiveData()
    val filteredExpensesWithCategory: MediatorLiveData<List<ExpenseWithCategory>> = MediatorLiveData()
    // LiveData for Expenses (with category names) for the current user
    val allExpensesWithCategory: LiveData<List<ExpenseWithCategory>> = _currentUserId.switchMap { userId ->
        if (userId != null) {
            expenseRepository.getExpensesWithCategoryForUser(userId)
        } else {
            MutableLiveData(emptyList())
        }
    }

    // LiveData for Categories for the current user (for the dropdown)
    val userCategories: LiveData<List<Category>> = _currentUserId.switchMap { userId ->
        if (userId != null) {
            categoryRepository.getCategoriesForUser(userId) // Get categories for the user
        } else {
            MutableLiveData(emptyList())
        }
    }

    init {
        // Set the initial user ID
        _currentUserId.value = auth.currentUser?.email
        setDefaultDates() // Set initial dates (e.g., current month)

        val sourceUserId = _currentUserId
        val sourceStartDate = _startDateMillis
        val sourceEndDate = _endDateMillis

        // Observe sources for Filtered Expenses List
        filteredExpensesWithCategory.addSource(sourceUserId) { fetchFilteredExpensesList(it, sourceStartDate.value, sourceEndDate.value) }
        filteredExpensesWithCategory.addSource(sourceStartDate) { fetchFilteredExpensesList(sourceUserId.value, it, sourceEndDate.value) }
        filteredExpensesWithCategory.addSource(sourceEndDate) { fetchFilteredExpensesList(sourceUserId.value, sourceStartDate.value, it) }


        // Observe sources for Category Totals
        categoryTotals.addSource(sourceUserId) { fetchCategoryTotals(it, sourceStartDate.value, sourceEndDate.value) }
        categoryTotals.addSource(sourceStartDate) { fetchCategoryTotals(sourceUserId.value, it, sourceEndDate.value) }
        categoryTotals.addSource(sourceEndDate) { fetchCategoryTotals(sourceUserId.value, sourceStartDate.value, it) }

        // Observe sources for Grand Total
        grandTotal.addSource(sourceUserId) { fetchGrandTotal(it, sourceStartDate.value, sourceEndDate.value) }
        grandTotal.addSource(sourceStartDate) { fetchGrandTotal(sourceUserId.value, it, sourceEndDate.value) }
        grandTotal.addSource(sourceEndDate) { fetchGrandTotal(sourceUserId.value, sourceStartDate.value, it) }
    }

    private fun setDefaultDates() {
        val calendar = Calendar.getInstance()
        // End Date: End of today
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        _endDateMillis.value = calendar.timeInMillis

        // Start Date: Beginning of the current month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        _startDateMillis.value = calendar.timeInMillis
    }

    // Helper to fetch totals, called by MediatorLiveData observers
    private var currentTotalsSource: LiveData<List<CategoryTotal>>? = null
    private fun fetchCategoryTotals(userId: String?, startDate: Long?, endDate: Long?) {
        // Remove previous source to avoid duplicates if fetching rapidly
        currentTotalsSource?.let { categoryTotals.removeSource(it) }

        if (userId != null && startDate != null && endDate != null) {
            // Input is valid, fetch data using the CORRECT repository method
            val newSource: LiveData<List<CategoryTotal>> =
                expenseRepository.getCategoryTotalsForUserAndDateRange(userId, startDate, endDate) // <-- CORRECT METHOD

            currentTotalsSource = newSource // Keep track of the current source

            // Add the new source to the MediatorLiveData
            categoryTotals.addSource(newSource) { totals ->
                // When the new source emits data (List<CategoryTotal>), update the MediatorLiveData
                categoryTotals.value = totals
            }
            Log.d("ExpenseViewModel", "Fetching category totals for $userId from $startDate to $endDate")
        } else {
            // Invalid input (user logged out, dates not set), post empty list
            categoryTotals.value = emptyList()
            Log.d("ExpenseViewModel", "Not fetching category totals - invalid input: User=$userId, Start=$startDate, End=$endDate")
        }
    }

    private var currentGrandTotalSource: LiveData<Double?>? = null
    private fun fetchGrandTotal(userId: String?, startDate: Long?, endDate: Long?) {
        currentGrandTotalSource?.let { grandTotal.removeSource(it) }

        if (userId != null && startDate != null && endDate != null) {
            val newSource = expenseRepository.getExpensesTotalForUserAndDateRange(userId, startDate, endDate)
            currentGrandTotalSource = newSource
            grandTotal.addSource(newSource) { total ->
                grandTotal.value = total // Post the result (could be null)
            }
            Log.d("ExpenseViewModel", "Fetching grand total for $userId from $startDate to $endDate")
        } else {
            grandTotal.value = null // Post null if input invalid
            Log.d("ExpenseViewModel", "Not fetching grand total - invalid input.")
        }
    }

    // --- Public functions to update dates ---
    fun setStartDate(millis: Long) {
        // Optional: Add validation (e.g., start date <= end date)
        if (millis <= (_endDateMillis.value ?: Long.MAX_VALUE)) {
            _startDateMillis.value = millis
        } else {
            Log.w("ExpenseViewModel", "Attempted to set start date after end date.")
            // Optionally show error via another LiveData
        }
    }

    fun setEndDate(millis: Long) {
        // Optional: Add validation (e.g., end date >= start date)
        if (millis >= (_startDateMillis.value ?: 0L)) {
            _endDateMillis.value = millis
        } else {
            Log.w("ExpenseViewModel", "Attempted to set end date before start date.")
            // Optionally show error
        }
    }


    // --- Expense Actions ---
    fun insert(expense: Expense) = viewModelScope.launch {
        if (expense.userId == _currentUserId.value) {
            expenseRepository.insert(expense)
        } else {
            Log.e("ExpenseViewModel", "Attempted to insert expense with mismatched userId.")
            // Handle error appropriately
        }
    }

    fun update(expense: Expense) = viewModelScope.launch {
        if (expense.userId == _currentUserId.value) {
            expenseRepository.update(expense)
        } else {
            Log.e("ExpenseViewModel", "Attempted to update expense with mismatched userId.")
        }
    }

    fun delete(expense: Expense) = viewModelScope.launch {
        if (expense.userId == _currentUserId.value) {
            expenseRepository.delete(expense)
        } else {
            Log.e("ExpenseViewModel", "Attempted to delete expense with mismatched userId.")
        }
    }

    // Consider fetching ExpenseWithCategory if you need associated data before deleting by ID
    fun deleteExpenseById(id: Long) = viewModelScope.launch {
        val expenseToDelete = expenseRepository.getExpenseById(id)
        if (expenseToDelete != null && expenseToDelete.userId == _currentUserId.value) {
            expenseRepository.delete(expenseToDelete) // Use the object to be safe
        } else {
            Log.e("ExpenseViewModel", "Attempted to delete expense by ID, but it wasn't found or didn't belong to the user.")
        }
    }

    suspend fun getExpenseById(id: Long): Expense? {
        val expense = expenseRepository.getExpenseById(id)
        return if (expense?.userId == _currentUserId.value) {
            expense
        } else {
            null // Don't return expense if it doesn't belong to the user
        }
    }

    // --- Helper to fetch Filtered Expenses List ---
    private var currentFilteredListSource: LiveData<List<ExpenseWithCategory>>? = null
    private fun fetchFilteredExpensesList(userId: String?, startDate: Long?, endDate: Long?) {
        currentFilteredListSource?.let { filteredExpensesWithCategory.removeSource(it) }

        if (userId != null && startDate != null && endDate != null) {
            val newSource = expenseRepository.getFilteredExpensesWithCategoryForUser(userId, startDate, endDate)
            currentFilteredListSource = newSource
            filteredExpensesWithCategory.addSource(newSource) { expenses ->
                filteredExpensesWithCategory.value = expenses // Post the filtered list
            }
            Log.d("ExpenseViewModel", "Fetching filtered expenses for $userId from $startDate to $endDate")
        } else {
            filteredExpensesWithCategory.value = emptyList() // Post empty list if input invalid
            Log.d("ExpenseViewModel", "Not fetching filtered expenses - invalid input.")
        }
    }

}