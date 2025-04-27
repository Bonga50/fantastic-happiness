package com.example.allocate_optimize_track

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

class CategoryAdapter(
    private val onItemClicked: (Category) -> Unit, // Lambda for item click
    private val onItemLongClicked: (Category) -> Unit // Lambda for item long click (for delete/edit menu)
) : ListAdapter<Category, CategoryAdapter.CategoryViewHolder>(CategoriesComparator()) {

    // Store the totals map provided by the fragment
    private var totalsMap: Map<Long, Double> = emptyMap()

    // Method for the fragment to update the totals map
    fun updateTotalsMap(newMap: Map<Long, Double>) {
        totalsMap = newMap
        // Important: Notify adapter that data MIGHT have changed visually,
        // even if the category list itself didn't change.
        // This is needed because an external map affects binding.
        notifyDataSetChanged() // Use this or iterate/notifyItemChanged if performance is critical
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view, onItemClicked, onItemLongClicked)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val currentCategory = getItem(position)
        // Look up the total spent for this category from the map
        val totalSpent = totalsMap[currentCategory.id] ?: 0.0
        holder.bind(currentCategory, totalSpent) // Pass total to bind
    }

    class CategoryViewHolder(
        itemView: View,
        private val onItemClicked: (Category) -> Unit,
        private val onItemLongClicked: (Category) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.textViewCategoryName)
        private val descTextView: TextView = itemView.findViewById(R.id.textViewCategoryDesc)
        private val limitTextView: TextView = itemView.findViewById(R.id.textViewCategoryLimit)
        private val spentGoalTextView: TextView = itemView.findViewById(R.id.textViewCategorySpentGoal)
        private val statsLayout: LinearLayout = itemView.findViewById(R.id.layoutCategoryStats) // Get stats layout
        private var currentCategory: Category? = null

        init {
            itemView.setOnClickListener {
                currentCategory?.let { category ->
                    onItemClicked(category) // Trigger click callback
                }
            }
            itemView.setOnLongClickListener {
                currentCategory?.let { category ->
                    onItemLongClicked(category) // Trigger long click callback
                    return@setOnLongClickListener true // Consume the long click
                }
                false
            }
        }


        fun bind(category: Category, totalSpent: Double) {
            currentCategory = category
            nameTextView.text = category.name
            descTextView.text = category.description ?: ""
            descTextView.visibility = if (category.description.isNullOrEmpty()) View.GONE else View.VISIBLE

            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
            val spentFormatted = currencyFormat.format(totalSpent)

            var goalVisible = false
            // Handle Spent/Goal TextView
            // Check if monthlyGoal is not null AND greater than 0
            if (category.monthlyGoal != null && category.monthlyGoal > 0.0) {
                val goalFormatted = currencyFormat.format(category.monthlyGoal)
                spentGoalTextView.text = "$spentFormatted / $goalFormatted"
                spentGoalTextView.visibility = View.VISIBLE
                goalVisible = true
            } else {
                spentGoalTextView.visibility = View.GONE
            }

            var limitVisible = false
            // Handle Limit TextView
            // Check if monthlyLimit is not null AND greater than 0
            if (category.monthlyLimit != null && category.monthlyLimit > 0.0) {
                limitTextView.text = "Limit: ${currencyFormat.format(category.monthlyLimit)}"
                limitTextView.visibility = View.VISIBLE
                limitVisible = true
            } else {
                limitTextView.visibility = View.GONE
            }

            // Hide the whole stats layout if neither goal nor limit is visible
            statsLayout.visibility = if (goalVisible || limitVisible) View.VISIBLE else View.GONE
        }
    }

    // DiffUtil helps ListAdapter determine changes efficiently
    class CategoriesComparator : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem // Data class equals checks all properties
        }
    }
}