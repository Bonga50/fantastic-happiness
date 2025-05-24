package com.example.allocate_optimize_track

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button // Import Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer // Use androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder // Use Material Dialogs
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import java.util.Calendar
import java.util.Date

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CategoriesFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CategoriesFragment : Fragment() {
    // Use the KTX delegate to get the ViewModel scoped to this Fragment
    // ViewModel for categories list itself
    private val categoryViewModel: CategoryViewModel by viewModels()
    // ViewModel for expense totals and date range (shared via Activity scope)
    private val expenseViewModel: ExpenseViewModel by activityViewModels()

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddCategory: FloatingActionButton
    private lateinit var buttonStartDateCat: MaterialButton // Date button
    private lateinit var buttonEndDateCat: MaterialButton   // Date button
    private lateinit var textViewDateRangeCat: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment (assuming you have one with RecyclerView and FAB)
        val view = inflater.inflate(R.layout.fragment_categories, container, false) // Make sure you have this layout

        recyclerView = view.findViewById(R.id.recyclerViewCategories) // Add this ID to your fragment_categories.xml
        fabAddCategory = view.findViewById(R.id.fabAddCategory) // Add this ID too
        buttonStartDateCat = view.findViewById(R.id.buttonStartDateCat) // Find date buttons
        buttonEndDateCat = view.findViewById(R.id.buttonEndDateCat)
        textViewDateRangeCat = view.findViewById(R.id.textViewDateRangeCat)
        auth = Firebase.auth
        auth.currentUser?.email
        setupRecyclerView()
        setupFab()
        setupDatePickers()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe list of categories from Firebase
        categoryViewModel.allCategories.observe(viewLifecycleOwner) { categories ->
            Log.d("CategoriesFragment", "Categories list updated: ${categories?.size ?: 0}")
            categoryAdapter.submitList(categories ?: emptyList())
            // Totals will be applied by the other observer via adapter.setCategorySpending
        }

        // Observe category totals for the selected date range (calculated client-side in ExpenseViewModel)
        expenseViewModel.categoryTotals.observe(viewLifecycleOwner) { categoryTotalList ->
            Log.d("CategoriesFragment", "Category totals updated: ${categoryTotalList?.size ?: 0}")
            val spendingMap = categoryTotalList?.associate { it.categoryId to it.totalAmount } ?: emptyMap()
            categoryAdapter.setCategorySpending(spendingMap)
        }

        // Observe operation status (add, update, delete)
        categoryViewModel.operationStatus.observe(viewLifecycleOwner) { result ->
            when (result) {
                is FirebaseResult.Loading -> { /* Show loading indicator */ }
                is FirebaseResult.Success -> {
                    Toast.makeText(context, "Operation successful", Toast.LENGTH_SHORT).show()
                }
                is FirebaseResult.Failure -> {
                    Toast.makeText(context, "Operation failed: ${result.exception.message}", Toast.LENGTH_LONG).show()
                    Log.e("CategoriesFragment", "Firebase op failed", result.exception)
                }
            }
        }

        // Observe date changes to update the display TextView
        expenseViewModel.startDateMillis.observe(viewLifecycleOwner) { updateDateRangeDisplay() }
        expenseViewModel.endDateMillis.observe(viewLifecycleOwner) { updateDateRangeDisplay() }
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter(
            onItemClicked = { category -> showAddEditCategoryDialog(category) },
            onItemLongClicked = { category -> showDeleteConfirmationDialog(category) }
        )
        recyclerView.adapter = categoryAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun updateDateRangeDisplay() {
        val startDateMillis = expenseViewModel.startDateMillis.value
        val endDateMillis = expenseViewModel.endDateMillis.value
        if (startDateMillis != null && endDateMillis != null) {
            val dateFormat = DateFormat.getMediumDateFormat(requireContext())
            val startStr = dateFormat.format(Date(startDateMillis))
            val endStr = dateFormat.format(Date(endDateMillis))
            textViewDateRangeCat.text = "$startStr - $endStr"
            buttonStartDateCat.text = "From: $startStr"
            buttonEndDateCat.text = "To: $endStr"
        } else {
            textViewDateRangeCat.text = "Select date range"
            buttonStartDateCat.text = "From:"
            buttonEndDateCat.text = "To:"
        }
    }

    private fun setupFab() {
        fabAddCategory.setOnClickListener {
            showAddEditCategoryDialog(null) // Pass null for adding a new category
        }
    }

    private fun setupDatePickers() {
        buttonStartDateCat.setOnClickListener { showDatePickerDialog(true) }
        buttonEndDateCat.setOnClickListener { showDatePickerDialog(false) }
    }

    private fun showDatePickerDialog(isStartDate: Boolean) {
        val currentMillis = if (isStartDate) expenseViewModel.startDateMillis.value else expenseViewModel.endDateMillis.value
        val calendar = Calendar.getInstance().apply { timeInMillis = currentMillis ?: System.currentTimeMillis() }
        DatePickerDialog(requireContext(), { _, year, month, day ->
            val selectedCalendar = Calendar.getInstance().apply { set(year, month, day) }
            if (isStartDate) {
                selectedCalendar.set(Calendar.HOUR_OF_DAY, 0); selectedCalendar.set(Calendar.MINUTE, 0); selectedCalendar.set(Calendar.SECOND, 0); selectedCalendar.set(Calendar.MILLISECOND, 0)
                expenseViewModel.setStartDate(selectedCalendar.timeInMillis)
            } else {
                selectedCalendar.set(Calendar.HOUR_OF_DAY, 23); selectedCalendar.set(Calendar.MINUTE, 59); selectedCalendar.set(Calendar.SECOND, 59); selectedCalendar.set(Calendar.MILLISECOND, 999)
                expenseViewModel.setEndDate(selectedCalendar.timeInMillis)
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showAddEditCategoryDialog(categoryToEdit: Category?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_edit_category, null)
        val nameEditText = dialogView.findViewById<TextInputEditText>(R.id.editTextCategoryName)
        val descEditText = dialogView.findViewById<TextInputEditText>(R.id.editTextCategoryDesc)
        val limitEditText = dialogView.findViewById<TextInputEditText>(R.id.editTextCategoryLimit)
        val goalEditText = dialogView.findViewById<TextInputEditText>(R.id.editCategoryGoal) // Add to your dialog layout

        val dialogTitle = if (categoryToEdit == null) "Add Category" else "Edit Category"

        categoryToEdit?.let {
            nameEditText.setText(it.name)
            descEditText.setText(it.description)
            limitEditText.setText(it.monthlyLimit?.toString() ?: "")
            goalEditText.setText(it.monthlyGoal?.toString() ?: "") // Pre-fill goal
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(dialogTitle)
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton(if (categoryToEdit == null) "Add" else "Update", null) // Set null to override
            .show()
            .apply {
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val name = nameEditText.text.toString().trim()
                    val description = descEditText.text.toString().trim()
                    val limitString = limitEditText.text.toString().trim()
                    val limit = limitString.toDoubleOrNull()
                    val goalString = goalEditText.text.toString().trim() // Get goal
                    val goal = goalString.toDoubleOrNull()

                    if (name.isEmpty()) {
                        nameEditText.error = "Name cannot be empty"; return@setOnClickListener
                    }
                    // Get current user ID (this should be robustly available, e.g., from ViewModel)
                    val currentFbUserId = categoryViewModel.getCurrentUserId() // Ensure this method exists in ViewModel
                    if (currentFbUserId == null) {
                        Toast.makeText(context, "Error: Not logged in", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val category = Category(
                        id = categoryToEdit?.id ?: "", // Keep existing ID for update, empty for new (Firebase service will gen)
                        createdAt = categoryToEdit?.createdAt ?: System.currentTimeMillis(),
                        name = name,
                        userId = currentFbUserId, // Set Firebase UID
                        description = description.ifEmpty { null },
                        monthlyLimit = limit,
                        monthlyGoal = goal
                    )

                    if (categoryToEdit == null) {
                        categoryViewModel.insert(category)
                    } else {
                        categoryViewModel.update(category)
                    }
                    dismiss()
                }
            }
    }


    private fun showDeleteConfirmationDialog(category: Category) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete '${category.name}'? This will also delete its expenses.") // Update message
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                categoryViewModel.delete(category) // ViewModel will call repository's Firebase delete
            }
            .show()
    }
}