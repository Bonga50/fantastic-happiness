package com.example.allocate_optimize_track

import android.app.DatePickerDialog
import android.graphics.Color
import android.icu.util.Calendar
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment() {
    // Use activityViewModels to share the same ExpenseViewModel instance
    // (and its date range) with ExpensesFragment and CategoriesFragment.
    private val expenseViewModel: ExpenseViewModel by activityViewModels()
    private val categoryViewModel: CategoryViewModel by activityViewModels() // If goals are on Category model
    private val gamificationViewModel: GamificationViewModel by viewModels()
    private lateinit var buttonGraphStartDate: MaterialButton
    private lateinit var buttonGraphEndDate: MaterialButton
    private lateinit var textViewGraphDateRange: TextView
    private lateinit var spendingLineChart: LineChart // Or BarChart
    private lateinit var recyclerViewBudgetProgress: RecyclerView
    private lateinit var budgetProgressAdapter: BudgetProgressAdapter
    private lateinit var textViewStreak: TextView
    // For X-axis date formatting
    private val displaySdf = SimpleDateFormat("MMM dd", Locale.getDefault()) // e.g., Apr 01

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        buttonGraphStartDate = view.findViewById(R.id.buttonGraphStartDate)
        buttonGraphEndDate = view.findViewById(R.id.buttonGraphEndDate)
        textViewGraphDateRange = view.findViewById(R.id.textViewGraphDateRange)
        spendingLineChart = view.findViewById(R.id.spendingLineChart) // Or R.id.spendingBarChart
        recyclerViewBudgetProgress = view.findViewById(R.id.recyclerViewBudgetProgress)
        textViewStreak = view.findViewById(R.id.textViewStreakDisplay)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDatePickers()
        setupSpendingChart()
        setupBudgetProgressRecyclerView()

        observeViewModelData()

        gamificationViewModel.userGamificationData.observe(viewLifecycleOwner) { data ->
            if (data != null && data.currentStreak > 0) {
                textViewStreak.text = "Daily Streak: ${data.currentStreak}"
                textViewStreak.visibility = View.VISIBLE
            } else {
                textViewStreak.visibility = View.GONE // Hide if no streak
            }
        }
    }

    private fun setupDatePickers() {
        buttonGraphStartDate.setOnClickListener { showDatePickerDialog(true) }
        buttonGraphEndDate.setOnClickListener { showDatePickerDialog(false) }
        updateGraphDateRangeDisplay() // Initial display
    }

    private fun showDatePickerDialog(isStartDate: Boolean) {
        val currentMillis = (if (isStartDate) expenseViewModel.startDateMillis.value else expenseViewModel.endDateMillis.value)
            ?: System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = currentMillis }
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

    private fun updateGraphDateRangeDisplay() {
        val startMillis = expenseViewModel.startDateMillis.value
        val endMillis = expenseViewModel.endDateMillis.value
        if (startMillis != null && endMillis != null) {
            val mediumDateFormat = DateFormat.getMediumDateFormat(requireContext())
            val startStr = mediumDateFormat.format(Date(startMillis))
            val endStr = mediumDateFormat.format(Date(endMillis))
            textViewGraphDateRange.text = "$startStr - $endStr"
            buttonGraphStartDate.text = "From: $startStr"
            buttonGraphEndDate.text = "To: $endStr"
        } else {
            textViewGraphDateRange.text = "Select Date Range"
            buttonGraphStartDate.text = "From:"
            buttonGraphEndDate.text = "To:"
        }
    }

    private fun setupSpendingChart() {
        spendingLineChart.description.isEnabled = false
        spendingLineChart.setTouchEnabled(true)
        spendingLineChart.setPinchZoom(true)
        spendingLineChart.setDrawGridBackground(false)
        // spendingLineChart.legend.isEnabled = true // Enable if multiple datasets (e.g. categories)

        val xAxis = spendingLineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                // Convert float (representing days from a start point) back to date string
                // This assumes 'value' is an index or a relative day number
                // For simplicity, we will map actual timestamps to floats later
                return displaySdf.format(Date(value.toLong()))
            }
        }
        xAxis.granularity = 1f // Minimum interval between axis values (e.g., 1 day)
        xAxis.labelRotationAngle = -45f

        spendingLineChart.axisRight.isEnabled = false // Hide right Y-axis
        // Customize Y-axis, colors, etc. as needed
    }


    private fun updateSpendingLineChart(dailyCategorySpending: Map<Long, Map<String, Double>>?) {
        if (dailyCategorySpending == null || dailyCategorySpending.isEmpty()) {
            spendingLineChart.clear()
            spendingLineChart.invalidate()
            return
        }
        Log.d("HomeFragment", "Updating spending chart with data: $dailyCategorySpending")

        val dataSets = ArrayList<ILineDataSet>()
        val allCategoryNames = dailyCategorySpending.values.flatMap { it.keys }.distinct().sorted()
        val colors = listOf(Color.BLUE, Color.GREEN, Color.RED, Color.MAGENTA, Color.CYAN, Color.YELLOW, Color.GRAY) // Add more

        // Create a common set of X-axis entries (timestamps for each day in the period)
        val sortedDays = dailyCategorySpending.keys.sorted()
        val xValuesMap = sortedDays.mapIndexed { index, timestamp -> timestamp to index.toFloat() }.toMap()


        allCategoryNames.forEachIndexed { colorIndex, categoryName ->
            val entries = ArrayList<Entry>()
            sortedDays.forEach { dayTimestamp ->
                val xVal = xValuesMap[dayTimestamp] ?: 0f // Get float X value for the day
                val amountForCategoryOnDay = dailyCategorySpending[dayTimestamp]?.get(categoryName) ?: 0.0
                entries.add(Entry(xVal, amountForCategoryOnDay.toFloat()))
            }

            if (entries.isNotEmpty()) {
                val dataSet = LineDataSet(entries, categoryName)
                dataSet.color = colors[colorIndex % colors.size]
                dataSet.setCircleColor(colors[colorIndex % colors.size])
                dataSet.lineWidth = 2f
                dataSet.circleRadius = 3f
                dataSet.setDrawCircleHole(false)
                dataSet.valueTextSize = 9f
                dataSet.setDrawFilled(false) // Or true with a fill color for area chart
                dataSets.add(dataSet)
            }
        }


        if (dataSets.isNotEmpty()) {
            val lineData = LineData(dataSets)
            spendingLineChart.data = lineData

            // Custom X-axis formatter using the actual timestamps
            val xAxis = spendingLineChart.xAxis
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    // Find the original timestamp for this float index 'value'
                    // This requires mapping float back to the sortedDays' original timestamp
                    val originalTimestamp = sortedDays.getOrNull(value.toInt()) // value is the index
                    return if (originalTimestamp != null) {
                        displaySdf.format(Date(originalTimestamp))
                    } else {
                        ""
                    }
                }
            }
            xAxis.labelCount = sortedDays.size.coerceAtMost(7) // Limit labels for readability
            xAxis.granularity = 1f


        } else {
            spendingLineChart.clear()
        }
        spendingLineChart.invalidate() // Refresh chart
        spendingLineChart.animateX(1000) // Animate
    }

    private fun setupBudgetProgressRecyclerView() {
        budgetProgressAdapter = BudgetProgressAdapter() // Create adapter instance
        recyclerViewBudgetProgress.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewBudgetProgress.adapter = budgetProgressAdapter
        recyclerViewBudgetProgress.isNestedScrollingEnabled = false // Good for RV inside ScrollView
    }

    private fun observeViewModelData() {
        // Observe date changes to update the display for graph filter
        expenseViewModel.startDateMillis.observe(viewLifecycleOwner) { updateGraphDateRangeDisplay() }
        expenseViewModel.endDateMillis.observe(viewLifecycleOwner) { updateGraphDateRangeDisplay() }

        // Observe daily category spending for the graph
        expenseViewModel.dailyCategorySpendingOverPeriod.observe(viewLifecycleOwner) { dailyData ->
            updateSpendingLineChart(dailyData)
        }

        // Observe categories (for goals) and category totals (for spent amounts this month)
        // to update budget progress
        val categoriesLiveData = categoryViewModel.allCategories
        val categoryTotalsLiveData = expenseViewModel.categoryTotals // This is already filtered by date

        // Combine categories and their totals for the budget progress
        // Note: categoryTotals are for the selected period, budget goals are usually monthly.
        // For "current month" budget, you'd ideally set the date filters to current month.
        // Or fetch totals specifically for current month.
        // For simplicity, we'll use the categoryTotals from the selected period for now.

        // Use a simple combined observation. For more complex scenarios, use MediatorLiveData.
        categoriesLiveData.observe(viewLifecycleOwner) { categories ->
            combineAndDisplayBudgetProgress(categories, categoryTotalsLiveData.value)
        }
        categoryTotalsLiveData.observe(viewLifecycleOwner) { totals ->
            combineAndDisplayBudgetProgress(categoriesLiveData.value, totals)
        }
    }

    private fun combineAndDisplayBudgetProgress(
        categories: List<Category>?,
        categoryTotalsForPeriod: List<CategoryTotal>?
    ) {
        if (categories == null) {
            budgetProgressAdapter.submitList(emptyList())
            return
        }

        val progressItems = categories.mapNotNull { category ->
            // Find total spent for this category IN THE SELECTED PERIOD
            val totalSpentInPeriod = categoryTotalsForPeriod?.find { it.categoryId == category.id }?.totalAmount ?: 0.0

            // Only create a progress item if there's a goal defined
            if (category.monthlyGoal != null && category.monthlyGoal!! > 0.0) {
                BudgetProgressAdapter.BudgetProgressItem(
                    categoryName = category.name,
                    amountSpent = totalSpentInPeriod,
                    goalAmount = category.monthlyGoal!!,
                    limitAmount = category.monthlyLimit // Pass limit too if you want to show it
                )
            } else {
                null // No goal, no progress item for this category
            }
        }
        budgetProgressAdapter.submitList(progressItems.sortedByDescending { it.amountSpent / it.goalAmount }) // Sort by % of goal spent
    }
}