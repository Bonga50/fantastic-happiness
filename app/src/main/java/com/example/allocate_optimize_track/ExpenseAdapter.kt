package com.example.allocate_optimize_track

import android.net.Uri
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.util.*

class ExpenseAdapter(
    private val onItemClicked: (ExpenseWithCategory) -> Unit,
    private val onItemLongClicked: (ExpenseWithCategory) -> Unit // Add long click listener
)  : ListAdapter<ExpenseWithCategory, ExpenseAdapter.ExpenseViewHolder>(ExpensesComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        // Pass both listeners to ViewHolder
        return ExpenseViewHolder(view, onItemClicked, onItemLongClicked)
    }


    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current)
    }

    class ExpenseViewHolder(
        itemView: View,
        private val onItemClicked: (ExpenseWithCategory) -> Unit,
        private val onItemLongClicked: (ExpenseWithCategory) -> Unit // Add listener param
    ) : RecyclerView.ViewHolder(itemView) {

        private val descriptionTextView: TextView = itemView.findViewById(R.id.textViewExpenseDescription)
        private val categoryTextView: TextView = itemView.findViewById(R.id.textViewExpenseCategory)
        private val amountTextView: TextView = itemView.findViewById(R.id.textViewExpenseAmount)
        private val dateTextView: TextView = itemView.findViewById(R.id.textViewExpenseDate)
        private val receiptImageView: ImageView = itemView.findViewById(R.id.imageViewSmallReceipt) // Add to layout
        private var currentExpenseItem: ExpenseWithCategory? = null

        init {
            itemView.setOnClickListener {
                currentExpenseItem?.let { item -> onItemClicked(item) }
            }
            // Add long click listener
            itemView.setOnLongClickListener {
                currentExpenseItem?.let { item ->
                    onItemLongClicked(item) // Trigger the callback
                    return@setOnLongClickListener true // Consume the event
                }
                false // Don't consume if item is null
            }
        }


        fun bind(expenseItem: ExpenseWithCategory) {
            currentExpenseItem = expenseItem
            val expense = expenseItem.expense
            val context = itemView.context

            descriptionTextView.text = expense.description
            categoryTextView.text = expenseItem.categoryName // Use the joined name

            // Format currency
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
            amountTextView.text = currencyFormat.format(expense.amount)

            // Format date
            val dateFormat = DateFormat.getMediumDateFormat(context) // Or getDateFormat
            dateTextView.text = dateFormat.format(Date(expense.date))

            // Load receipt image thumbnail if URI exists
            if (!expense.photoUri.isNullOrEmpty()) {
                receiptImageView.visibility = View.VISIBLE
                Glide.with(context)
                    .load(Uri.parse(expense.photoUri))
                    .centerCrop()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .into(receiptImageView)
            } else {
                receiptImageView.visibility = View.GONE // Hide if no image
            }
        }
    }

    class ExpensesComparator : DiffUtil.ItemCallback<ExpenseWithCategory>() {
        override fun areItemsTheSame(oldItem: ExpenseWithCategory, newItem: ExpenseWithCategory): Boolean {
            return oldItem.expense.id == newItem.expense.id
        }

        override fun areContentsTheSame(oldItem: ExpenseWithCategory, newItem: ExpenseWithCategory): Boolean {
            return oldItem == newItem
        }
    }
}
