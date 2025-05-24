package com.example.allocate_optimize_track

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.switchMap
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ExpenseRepository(private val categoryRepository: CategoryRepository) {

    private val firebaseService = FirebaseCrudService(Expense::class.java)
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val EXPENSES_PATH = "expenses"
    }

    // Get all expenses for the user, then map to ExpenseWithCategory
    fun getExpensesWithCategoryForUser(): LiveData<List<ExpenseWithCategory>> {
        val result = MediatorLiveData<List<ExpenseWithCategory>>()

        val allExpensesLiveData = firebaseService.getAll(EXPENSES_PATH)
        val allCategoriesLiveData = categoryRepository.getCategoriesForCurrentUser()

        // Local variables to hold the latest values from each source
        var expensesList: List<Expense>? = null
        var categoriesList: List<Category>? = null // Use your Category model

        val combineAndEmit = {
            // Only proceed if both lists have been loaded
            val currentExpenses = expensesList
            val currentCategories = categoriesList

            if (currentExpenses != null && currentCategories != null) {
                val categoryMap = currentCategories.associateBy { it.id }
                val combinedList = currentExpenses.map { expense ->
                    ExpenseWithCategory(
                        expense,
                        categoryMap[expense.categoryId]?.name ?: "Unknown Category"
                    )
                }
                result.postValue(combinedList)
            }
        }

        result.addSource(allExpensesLiveData) { newExpenses ->
            expensesList = newExpenses
            combineAndEmit()
        }

        result.addSource(allCategoriesLiveData) { newCategories ->
            categoriesList = newCategories
            combineAndEmit()
        }

        return result
    }


    suspend fun insert(expense: Expense, imageUri: Uri?, contentResolver: ContentResolver): FirebaseResult<String> {
        val userId = auth.currentUser?.uid ?: return FirebaseResult.Failure(Exception("User not authenticated"))
        expense.userId = userId

        val newExpenseRef = FirebaseDatabase.getInstance().getReference(EXPENSES_PATH).child(userId).push()
        val expenseId = newExpenseRef.key ?: return FirebaseResult.Failure(Exception("Failed to generate expense ID"))
        expense.id = expenseId

        imageUri?.let { uri ->
            // Call the Edge Function through the service
            val imagePath = SupabaseImageService.uploadReceiptImageViaFunction(userId, expenseId, uri, contentResolver)
            if (imagePath != null) {
                expense.photoStoragePath = imagePath
            } else {
                Log.w("ExpenseRepository", "Image upload failed for expense $expenseId, proceeding without image path.")
                // Decide if this is a critical failure or if expense can be saved without image
            }
        }
        return firebaseService.create(expense, EXPENSES_PATH, { it.id }, { item, id -> item.id = id })
    }


    suspend fun update(expense: Expense, newImageUri: Uri?, contentResolver: ContentResolver): FirebaseResult<Unit> {
        if (expense.id.isEmpty()) return FirebaseResult.Failure(Exception("Expense ID is empty"))
        val userId = auth.currentUser?.uid ?: return FirebaseResult.Failure(Exception("User not authenticated"))
        expense.userId = userId // Ensure userId is set

        if (newImageUri != null) {
            // Delete old image if it exists (still direct client call, consider moving to function if needed)
            expense.photoStoragePath?.let { oldPath ->
                SupabaseImageService.deleteReceiptImage(oldPath)
            }
            // Upload new image via Edge Function
            val newImagePath = SupabaseImageService.uploadReceiptImageViaFunction(userId, expense.id, newImageUri, contentResolver)
            expense.photoStoragePath = newImagePath // Update path (could be null if upload failed)
        } else if (expense.photoStoragePath != null && /* logic to know if user wants to remove image */ false ) {
            // Handle explicit image removal (if user cleared the image preview)
            SupabaseImageService.deleteReceiptImage(expense.photoStoragePath!!)
            expense.photoStoragePath = null
        }
        // If newImageUri is null, user might want to remove the image
        // else if (expense.photoStoragePath != null && newImageUri == null) { // Logic for explicit removal
        //    SupabaseImageService.deleteReceiptImage(expense.photoStoragePath!!)
        //    expense.photoStoragePath = null
        // }

        return firebaseService.update(expense.id, expense, EXPENSES_PATH)
    }

    suspend fun delete(expense: Expense): FirebaseResult<Unit> {
        if (expense.id.isEmpty()) return FirebaseResult.Failure(Exception("Expense ID is empty"))
        // Delete image from Supabase first
        expense.photoStoragePath?.let { path ->
            SupabaseImageService.deleteReceiptImage(path)
        }
        // Then delete expense from Firebase
        return firebaseService.delete(expense.id, EXPENSES_PATH)
    }

    suspend fun getExpenseById(id: String): FirebaseResult<Expense?> {
        return firebaseService.getById(id, EXPENSES_PATH)
    }
}