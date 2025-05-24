package com.example.allocate_optimize_track

import android.graphics.PorterDuff
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale
import android.view.LayoutInflater


class BudgetProgressAdapter : ListAdapter<BudgetProgressAdapter.BudgetProgressItem, BudgetProgressAdapter.BudgetProgressViewHolder>(BudgetProgressDiffCallback()) {

    data class BudgetProgressItem(
        val categoryName: String,
        val amountSpent: Double,
        val goalAmount: Double,
        val limitAmount: Double? = null // Optional
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetProgressViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_budget_progress, parent, false)
        return BudgetProgressViewHolder(view)
    }

    override fun onBindViewHolder(holder: BudgetProgressViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class BudgetProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryNameTextView: TextView = itemView.findViewById(R.id.textViewBudgetItemCategoryName)
        private val spentVsGoalTextView: TextView = itemView.findViewById(R.id.textViewBudgetItemSpentVsGoal)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBarBudgetItem)
        private val statusTextView: TextView = itemView.findViewById(R.id.textViewBudgetItemStatus)
        private val context = itemView.context

        fun bind(item: BudgetProgressItem) {
            categoryNameTextView.text = item.categoryName
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
            spentVsGoalTextView.text = "${currencyFormat.format(item.amountSpent)} / ${currencyFormat.format(item.goalAmount)}"

            val progressPercentage = if (item.goalAmount > 0) {
                ((item.amountSpent / item.goalAmount) * 100).toInt().coerceIn(0, 1000) // Allow over 100% for visual
            } else {
                0
            }
            progressBar.progress = progressPercentage.coerceAtMost(100) // Cap progress bar at 100 visual

            statusTextView.visibility = View.GONE // Default hide
            // progressBar.progressTintList = null // Reset tint

            if (item.amountSpent > item.goalAmount) {
                statusTextView.text = "Over Goal!"
                statusTextView.setTextColor(ContextCompat.getColor(context, R.color.red_warning)) // Define this color
                statusTextView.visibility = View.VISIBLE
                // Optionally change progress bar color for overspending on goal
                progressBar.progressDrawable?.setColorFilter(ContextCompat.getColor(context, R.color.red_warning), PorterDuff.Mode.SRC_IN)
            } else {
                statusTextView.visibility = View.GONE
                // Reset progress bar color if not overspent
                progressBar.progressDrawable?.setColorFilter(ContextCompat.getColor(context, R.color.primaryDark), PorterDuff.Mode.SRC_IN) // Your primary color
            }

            // Additional check for limit if provided and different from goal
            if (item.limitAmount != null && item.limitAmount > 0 && item.amountSpent > item.limitAmount) {
                statusTextView.text = "Over Limit!" // Prioritize limit warning
                statusTextView.setTextColor(ContextCompat.getColor(context, R.color.red_critical)) // Define a more critical red
                statusTextView.visibility = View.VISIBLE
                progressBar.progressDrawable?.setColorFilter(ContextCompat.getColor(context, R.color.red_critical), PorterDuff.Mode.SRC_IN)
            } else if (item.amountSpent <= item.goalAmount) { // Only set "On Track" if not over goal and not over limit
                statusTextView.text = "On Track"
                statusTextView.setTextColor(ContextCompat.getColor(context, R.color.green_success)) // Define this color
                statusTextView.visibility = View.VISIBLE
                progressBar.progressDrawable?.setColorFilter(ContextCompat.getColor(context, R.color.green_success), PorterDuff.Mode.SRC_IN)
            }
        }
    }

    class BudgetProgressDiffCallback : DiffUtil.ItemCallback<BudgetProgressItem>() {
        override fun areItemsTheSame(oldItem: BudgetProgressItem, newItem: BudgetProgressItem): Boolean {
            return oldItem.categoryName == newItem.categoryName // Assuming category names are unique for this display
        }
        override fun areContentsTheSame(oldItem: BudgetProgressItem, newItem: BudgetProgressItem): Boolean {
            return oldItem == newItem
        }
    }
}