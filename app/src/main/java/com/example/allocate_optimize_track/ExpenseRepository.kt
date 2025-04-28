package com.example.allocate_optimize_track

import androidx.lifecycle.LiveData

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    // No direct LiveData exposed here, ViewModel will request based on user ID

    fun getExpensesWithCategoryForUser(userId: String): LiveData<List<ExpenseWithCategory>> {
        return expenseDao.getExpensesWithCategoryForUser(userId)
    }

    suspend fun insert(expense: Expense): Long {
        return expenseDao.insert(expense)
    }

    suspend fun update(expense: Expense) {
        expenseDao.update(expense)
    }

    suspend fun delete(expense: Expense) {
        expenseDao.delete(expense)
    }

    suspend fun getExpenseById(id: Long): Expense? {
        return expenseDao.getExpenseById(id)
    }

    fun getExpensesTotalForUserAndDateRange(userId: String, startDate: Long, endDate: Long): LiveData<Double?> {
        return expenseDao.getExpensesTotalForUserAndDateRange(userId, startDate, endDate)
    }

    fun getCategoryTotalsForUserAndDateRange(userId: String, startDate: Long, endDate: Long): LiveData<List<CategoryTotal>> {
        return expenseDao.getCategoryTotalsForUserAndDateRange(userId, startDate, endDate)
    }

    fun getFilteredExpensesWithCategoryForUser(userId: String, startDate: Long, endDate: Long): LiveData<List<ExpenseWithCategory>> {
        return expenseDao.getFilteredExpensesWithCategoryForUser(userId, startDate, endDate)
    }
}