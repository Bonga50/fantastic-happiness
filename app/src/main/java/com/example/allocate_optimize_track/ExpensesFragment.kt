package com.example.allocate_optimize_track

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*
import androidx.lifecycle.Observer
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.NumberFormat


/**
 * A simple [Fragment] subclass.
 * Use the [ExpensesFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ExpensesFragment : Fragment() {
    private val expenseViewModel: ExpenseViewModel by viewModels() // Use the ExpenseViewModel
    private lateinit var expenseAdapter: ExpenseAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddExpense: FloatingActionButton
    private lateinit var buttonStartDate: MaterialButton
    private lateinit var buttonEndDate: MaterialButton
    private lateinit var textViewDateRange: TextView
    private lateinit var textViewListTitle: TextView
    private lateinit var textViewGrandTotalAmount: TextView
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_expenses, container, false) // Ensure layout exists

        recyclerView = view.findViewById(R.id.recyclerViewExpenses) // Add this ID to fragment_expenses.xml
        fabAddExpense = view.findViewById(R.id.fabAddExpense) // Add this ID
        buttonStartDate = view.findViewById(R.id.buttonStartDate)
        buttonEndDate = view.findViewById(R.id.buttonEndDate)
        textViewDateRange = view.findViewById(R.id.textViewDateRange)
        textViewListTitle = view.findViewById(R.id.textViewListTitle)
        textViewGrandTotalAmount = view.findViewById(R.id.textViewGrandTotalAmount)

        setupRecyclerView()
        setupFab()
        setupDatePickers()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe ALL Expenses (filtered by date range)
        // NOTE: We currently fetch ALL expenses for the user and rely on the grandTotal/categoryTotals
        //       to reflect the date range. For large datasets, you might want to also filter
        //       this list based on dates, requiring another MediatorLiveData setup similar to totals.
        //       For now, we display all expenses and the totals separately reflect the selected range.

        // --- Observe the NEW Filtered Expenses List ---
        expenseViewModel.filteredExpensesWithCategory.observe(viewLifecycleOwner, Observer { expenses ->
            // This list is now filtered by the selected date range
            Log.d("ExpensesFragment", "Filtered expenses updated: ${expenses?.size ?: 0}")
            textViewListTitle.text = "Expenses (${expenses?.size ?: 0})" // Update title with count
            expenseAdapter.submitList(expenses) // Submit the filtered list
        })

        // Observe date changes to update the display TextView (remains the same)
        expenseViewModel.startDateMillis.observe(viewLifecycleOwner) { updateDateRangeDisplay() }
        expenseViewModel.endDateMillis.observe(viewLifecycleOwner) { updateDateRangeDisplay() }

        // Observe Grand Total (remains the same)
        expenseViewModel.grandTotal.observe(viewLifecycleOwner, Observer { total ->
            updateGrandTotalDisplay(total)
        })
    }

    private fun updateGrandTotalDisplay(total: Double?) {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
        textViewGrandTotalAmount.text = currencyFormat.format(total ?: 0.0) // Display $0.00 if null
    }

    private fun setupDatePickers() {
        buttonStartDate.setOnClickListener {
            showDatePickerDialog(true) // true indicates setting start date
        }
        buttonEndDate.setOnClickListener {
            showDatePickerDialog(false) // false indicates setting end date
        }
    }


    private fun setupRecyclerView() {
        // Pass both lambdas to the adapter
        expenseAdapter = ExpenseAdapter(
            onItemClicked = { expenseWithCategory ->
                // Navigate to AddEditExpenseFragment with the expense ID
                Log.d("ExpensesFragment", "Editing expense ID: ${expenseWithCategory.expense.id}")
                val action = ExpensesFragmentDirections
                    .actionExpensesFragmentToAddEditExpenseFragment(expenseWithCategory.expense.id) // Use Safe Args generated action
                findNavController().navigate(action)
            },
            onItemLongClicked = { expenseWithCategory ->
                // Show delete confirmation dialog
                showDeleteConfirmationDialog(expenseWithCategory.expense) // Pass the Expense object
            }
        )
        recyclerView.adapter = expenseAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun showDeleteConfirmationDialog(expense: Expense) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete this expense?\n\"${expense.description}\"")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                expenseViewModel.delete(expense) // Call ViewModel delete function
                Toast.makeText(context, "Expense deleted", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun setupFab() {
        fabAddExpense.setOnClickListener {
            // Navigate to the Add/Edit Fragment
            // Make sure you have this navigation action defined in your nav_graph.xml
            findNavController().navigate(R.id.action_expensesFragment_to_addEditExpenseFragment) // Replace with your actual action ID
        }
    }

    private fun showDatePickerDialog(isStartDate: Boolean) {
        val currentMillis = if (isStartDate) {
            expenseViewModel.startDateMillis.value
        } else {
            expenseViewModel.endDateMillis.value
        } ?: System.currentTimeMillis() // Fallback to now

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentMillis

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(selectedYear, selectedMonth, selectedDay)

            // Set time to start/end of day for accurate BETWEEN query
            if (isStartDate) {
                selectedCalendar.set(Calendar.HOUR_OF_DAY, 0)
                selectedCalendar.set(Calendar.MINUTE, 0)
                selectedCalendar.set(Calendar.SECOND, 0)
                selectedCalendar.set(Calendar.MILLISECOND, 0)
                expenseViewModel.setStartDate(selectedCalendar.timeInMillis)
            } else {
                selectedCalendar.set(Calendar.HOUR_OF_DAY, 23)
                selectedCalendar.set(Calendar.MINUTE, 59)
                selectedCalendar.set(Calendar.SECOND, 59)
                selectedCalendar.set(Calendar.MILLISECOND, 999)
                expenseViewModel.setEndDate(selectedCalendar.timeInMillis)
            }
            // Date range display TextView will update via its observer
        }, year, month, day).show()
    }

    private fun updateDateRangeDisplay() {
        val startDateMillis = expenseViewModel.startDateMillis.value
        val endDateMillis = expenseViewModel.endDateMillis.value
        val context = requireContext()

        if (startDateMillis != null && endDateMillis != null) {
            val dateFormat = DateFormat.getMediumDateFormat(context) // Or SHORT, LONG, FULL
            val startDateStr = dateFormat.format(Date(startDateMillis))
            val endDateStr = dateFormat.format(Date(endDateMillis))
            textViewDateRange.text = "$startDateStr - $endDateStr"
            // Update button text for clarity (optional)
            buttonStartDate.text = "From: $startDateStr"
            buttonEndDate.text = "To: $endDateStr"
        } else {
            textViewDateRange.text = "Select date range"
            buttonStartDate.text = "From:"
            buttonEndDate.text = "To:"
        }
    }
}