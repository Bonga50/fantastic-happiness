package com.example.allocate_optimize_track

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
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
    private val categoryRepository = CategoryRepository() // For category names
    private val expenseRepository = ExpenseRepository(categoryRepository) // Inject
    private val auth = FirebaseAuth.getInstance()

    private val _currentUserId = MutableLiveData<String?>() // Still useful

    private val _startDateMillis = MutableLiveData<Long>()
    val startDateMillis: LiveData<Long> get() = _startDateMillis // Expose immutable LiveData

    private val _endDateMillis = MutableLiveData<Long>()
    val endDateMillis: LiveData<Long> get() = _endDateMillis

    // Raw list of all expenses for the user from Firebase
    private val _rawExpensesWithCategory: LiveData<List<ExpenseWithCategory>> =
        _currentUserId.switchMap { userId ->
            if (userId != null) {
                expenseRepository.getExpensesWithCategoryForUser() // Gets all
            } else {
                MutableLiveData(emptyList())
            }
        }

    // Filtered list based on date range (client-side filtering)
    val filteredExpensesWithCategory: MediatorLiveData<List<ExpenseWithCategory>> = MediatorLiveData()
    val categoryTotals: MediatorLiveData<List<CategoryTotal>> = MediatorLiveData()
    val grandTotal: MediatorLiveData<Double?> = MediatorLiveData()

    val userCategories: LiveData<List<Category>> = categoryRepository.getCategoriesForCurrentUser() // For dropdown

    private val _saveExpenseStatus = MutableLiveData<FirebaseResult<String>?>() // Make it nullable
    val saveExpenseStatus: LiveData<FirebaseResult<String>?> = _saveExpenseStatus

    // LiveData for daily spending (Map: Day Timestamp -> Total Amount)
    val dailySpendingOverPeriod: MediatorLiveData<Map<Long, Double>> = MediatorLiveData()

    // LiveData for daily spending broken down by category (Map: Day Timestamp -> Map<CategoryName, Amount>)
    val dailyCategorySpendingOverPeriod: MediatorLiveData<Map<Long, Map<String, Double>>> = MediatorLiveData()

    init {
        _currentUserId.value = auth.currentUser?.uid
        if (_startDateMillis.value == null || _endDateMillis.value == null) {
            setDefaultDates()
        } else {
            Log.d("ExpenseViewModel", "Dates already initialized, skipping setDefaultDates. Start: ${_startDateMillis.value}, End: ${_endDateMillis.value}")
        }
// Your existing function

        // Combine and filter client-side
        filteredExpensesWithCategory.addSource(_rawExpensesWithCategory) { applyFilters() }
        filteredExpensesWithCategory.addSource(startDateMillis) { applyFilters() }
        filteredExpensesWithCategory.addSource(endDateMillis) { applyFilters() }

        // These also depend on the client-side filtered list
        categoryTotals.addSource(filteredExpensesWithCategory) { expenses -> calculateCategoryTotals(expenses) }
        grandTotal.addSource(filteredExpensesWithCategory) { expenses -> calculateGrandTotal(expenses) }

        // Observe filtered expenses to calculate daily spending
        dailySpendingOverPeriod.addSource(filteredExpensesWithCategory) { expenses ->
            calculateDailySpending(expenses)
        }
        dailyCategorySpendingOverPeriod.addSource(filteredExpensesWithCategory) { expenses ->
            calculateDailyCategorySpending(expenses)
        }
    }

    private fun calculateDailySpending(expenses: List<ExpenseWithCategory>?) {
        if (expenses == null) {
            dailySpendingOverPeriod.value = emptyMap()
            return
        }
        val dailyTotals = expenses
            .groupBy {
                // Normalize date to the start of the day for grouping
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = it.expense.date
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis // Key is the timestamp for the start of the day
            }
            .mapValues { entry ->
                entry.value.sumOf { it.expense.amount }
            }
            .toSortedMap() // Sort by date
        dailySpendingOverPeriod.value = dailyTotals
        Log.d("ExpenseViewModel", "Calculated daily spending: $dailyTotals")
    }

    private fun calculateDailyCategorySpending(expenses: List<ExpenseWithCategory>?) {
        if (expenses == null) {
            dailyCategorySpendingOverPeriod.value = emptyMap()
            return
        }
        val dailyCategoryTotals = expenses
            .groupBy { // Group by day (start of day timestamp)
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = it.expense.date
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            .mapValues { dailyEntry -> // For each day
                dailyEntry.value // List of ExpenseWithCategory for that day
                    .groupBy { it.categoryName ?: "Uncategorized" } // Group by category name
                    .mapValues { categoryEntry -> // For each category within that day
                        categoryEntry.value.sumOf { it.expense.amount } // Sum amounts for that category on that day
                    }
            }
            .toSortedMap() // Sort by date
        dailyCategorySpendingOverPeriod.value = dailyCategoryTotals
        Log.d("ExpenseViewModel", "Calculated daily category spending: $dailyCategoryTotals")
    }

    private fun applyFilters() {
        val rawList = _rawExpensesWithCategory.value ?: emptyList()
        val start = startDateMillis.value
        val end = endDateMillis.value

        if (start != null && end != null) {
            val filtered = rawList.filter { it.expense.date in start..end }
            filteredExpensesWithCategory.value = filtered
        } else {
            filteredExpensesWithCategory.value = rawList // Show all if dates not set
        }
    }

    private fun calculateCategoryTotals(expenses: List<ExpenseWithCategory>?) {
        if (expenses == null) {
            categoryTotals.value = emptyList()
            return
        }
        val totalsMap = expenses
            .groupBy { it.expense.categoryId to (it.categoryName ?: "Unknown") }
            .map { (key, groupedExpenses) ->
                CategoryTotal(
                    categoryId = key.first,
                    categoryName = key.second,
                    totalAmount = groupedExpenses.sumOf { it.expense.amount }
                )
            }
        categoryTotals.value = totalsMap.sortedByDescending { it.totalAmount }
    }

    private fun calculateGrandTotal(expenses: List<ExpenseWithCategory>?) {
        grandTotal.value = expenses?.sumOf { it.expense.amount }
    }


    fun insertExpense(expense: Expense, imageUri: Uri?, contentResolver: ContentResolver) = viewModelScope.launch {
        _saveExpenseStatus.value = FirebaseResult.Loading
        val result = expenseRepository.insert(expense, imageUri, contentResolver)
        _saveExpenseStatus.value = result
    }

    fun updateExpense(expense: Expense, imageUri: Uri?, contentResolver: ContentResolver) = viewModelScope.launch {
        _saveExpenseStatus.value = FirebaseResult.Loading // Can reuse status LiveData
        val result = expenseRepository.update(expense, imageUri, contentResolver)
        _saveExpenseStatus.value = if(result is FirebaseResult.Success) FirebaseResult.Success("Updated") else result as FirebaseResult<String>
    }

    fun deleteExpense(expense: Expense) = viewModelScope.launch {
        // Optionally have a delete status LiveData
        expenseRepository.delete(expense)
    }

    fun clearSaveExpenseStatus() {
        _saveExpenseStatus.value = null // Reset to null after being handled
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

    // getExpenseById will now call expenseRepository.getExpenseById which is suspend
    suspend fun getExpenseById(id: String): Expense? {
        val result = expenseRepository.getExpenseById(id)
        return if (result is FirebaseResult.Success) result.data else null
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

}