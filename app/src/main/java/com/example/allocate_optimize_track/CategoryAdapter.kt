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

    private var categorySpendingMap: Map<String, Double> = emptyMap() // Key: categoryId, Value: totalSpent

    // Method for the fragment to update the spending map
    fun setCategorySpending(spendingMap: Map<String, Double>) {
        this.categorySpendingMap = spendingMap
        notifyDataSetChanged() // Or use more specific notifications if performance is an issue
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false) // Your existing item_category.xml
        return CategoryViewHolder(view, onItemClicked, onItemLongClicked)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val currentCategory = getItem(position)
        val totalSpent = categorySpendingMap[currentCategory.id] ?: 0.0 // Get spent amount
        holder.bind(currentCategory, totalSpent)
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
        private val statsLayout: LinearLayout = itemView.findViewById(R.id.layoutCategoryStats)
        private var currentCategoryData: Category? = null // Renamed to avoid confusion

        init {
            itemView.setOnClickListener { currentCategoryData?.let { onItemClicked(it) } }
            itemView.setOnLongClickListener {
                currentCategoryData?.let { onItemLongClicked(it) }
                true
            }
        }

        fun bind(category: Category, totalSpent: Double) {
            currentCategoryData = category
            nameTextView.text = category.name
            descTextView.text = category.description ?: ""
            descTextView.visibility = if (category.description.isNullOrEmpty()) View.GONE else View.VISIBLE

            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
            val spentFormatted = currencyFormat.format(totalSpent)

            var goalVisible = false
            if (category.monthlyGoal != null && category.monthlyGoal!! > 0.0) {
                val goalFormatted = currencyFormat.format(category.monthlyGoal!!)
                spentGoalTextView.text = "$spentFormatted / $goalFormatted"
                spentGoalTextView.visibility = View.VISIBLE
                goalVisible = true
            } else {
                spentGoalTextView.visibility = View.GONE
            }

            var limitVisible = false
            if (category.monthlyLimit != null && category.monthlyLimit!! > 0.0) {
                limitTextView.text = "Limit: ${currencyFormat.format(category.monthlyLimit!!)}"
                limitTextView.visibility = View.VISIBLE
                limitVisible = true
            } else {
                limitTextView.visibility = View.GONE
            }
            statsLayout.visibility = if (goalVisible || limitVisible) View.VISIBLE else View.GONE
        }
    }

    class CategoriesComparator : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.id == newItem.id // Compare by Firebase ID
        }
        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem // Data class comparison
        }
    }
}