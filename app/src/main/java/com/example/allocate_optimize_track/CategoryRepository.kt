package com.example.allocate_optimize_track


import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.map
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class CategoryRepository {
    private val firebaseService = FirebaseCrudService(Category::class.java)
    private val expenseService = FirebaseCrudService(Expense::class.java)
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val CATEGORIES_PATH = "categories"
        private const val EXPENSES_PATH = "expenses"
    }

    fun getCategoriesForCurrentUser(): LiveData<List<Category>> {
        return firebaseService.getAll(CATEGORIES_PATH)
    }

    suspend fun insert(category: Category): FirebaseResult<String> {
        category.userId = auth.currentUser?.uid ?: return FirebaseResult.Failure(Exception("User not authenticated for insert"))
        return firebaseService.create(category, CATEGORIES_PATH, { it.id }, { item, id -> item.id = id })
    }

    suspend fun update(category: Category): FirebaseResult<Unit> {
        if (category.id.isEmpty()) return FirebaseResult.Failure(Exception("Category ID is empty for update"))
        category.userId = auth.currentUser?.uid ?: return FirebaseResult.Failure(Exception("User not authenticated for update"))
        return firebaseService.update(category.id, category, CATEGORIES_PATH)
    }

    suspend fun delete(category: Category): FirebaseResult<Unit> {
        if (category.id.isEmpty()) return FirebaseResult.Failure(Exception("Category ID is empty for delete"))
        val userId = auth.currentUser?.uid ?: return FirebaseResult.Failure(Exception("User not authenticated for delete"))

        // Use coroutineScope to ensure all child coroutines complete before proceeding
        return try {
            coroutineScope { // Ensures structured concurrency
                // 1. Fetch all expenses for this category ONCE
                val expensesResult = expenseService.queryByFieldOnce(EXPENSES_PATH, "categoryId", category.id)

                if (expensesResult is FirebaseResult.Success) {
                    val expensesList = expensesResult.data

                    // 2. Delete associated images and expenses (can be done concurrently)
                    val deleteExpenseJobs = expensesList.map { expense ->
                        async(Dispatchers.IO) { // Launch each deletion in a separate coroutine job
                            // Delete associated image from Supabase if it exists
                            expense.photoStoragePath?.let { path ->
                                SupabaseImageService.deleteReceiptImage(path)
                                // Log or handle image deletion failure if needed
                            }
                            // Delete the expense from Firebase RTDB
                            expenseService.delete(expense.id, EXPENSES_PATH)
                            // Log or handle expense deletion failure if needed
                        }
                    }
                    deleteExpenseJobs.awaitAll() // Wait for all expense and image deletions to complete

                    // 3. After deleting all associated expenses and images, delete the category
                    firebaseService.delete(category.id, CATEGORIES_PATH)
                    // Note: firebaseService.delete already returns FirebaseResult<Unit>
                } else if (expensesResult is FirebaseResult.Failure) {
                    // Failed to fetch expenses, cannot proceed with full cascade.
                    // Decide on behavior: fail the whole operation, or just try to delete the category?
                    // For robustness, let's fail the whole operation if fetching expenses fails.
                    return@coroutineScope FirebaseResult.Failure(
                        Exception("Failed to fetch expenses for category ${category.id} during delete: ${expensesResult.exception.message}")
                    )
                } else {
                    // Handle Loading state if you added it to FirebaseResult, though queryByFieldOnce shouldn't remain in Loading
                    return@coroutineScope FirebaseResult.Failure(Exception("Unexpected state while fetching expenses for delete"))
                }
            }
            // If coroutineScope completes without returning a Failure, it implies success.
            // The last call to firebaseService.delete(category.id, CATEGORIES_PATH) will return the result.
            // However, the above return@coroutineScope FirebaseResult.Failure might override.
            // Let's ensure a clear success return if we reach here after successful expense deletion.
            // The final delete of the category itself will provide the result.
            // The structure above returns the result of the category delete,
            // or a failure if fetching expenses failed.

            // If the logic reached the point of calling firebaseService.delete(category.id, CATEGORIES_PATH),
            // that function's result will be the return value of the coroutineScope block if it's the last expression.
            // This structure is fine.
        } catch (e: Exception) {
            // Catch any other unexpected exceptions during the process
            FirebaseResult.Failure(Exception("Error during category cascade delete: ${e.message}", e))
        }
    }


    suspend fun getCategoryById(id: String): FirebaseResult<Category?> {
        return firebaseService.getById(id, CATEGORIES_PATH)
    }
}