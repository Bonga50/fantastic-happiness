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
        return ExpenseViewHolder(view, onItemClicked, onItemLongClicked)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ExpenseViewHolder(
        itemView: View,
        private val onItemClicked: (ExpenseWithCategory) -> Unit,
        private val onItemLongClicked: (ExpenseWithCategory) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val descriptionTextView: TextView = itemView.findViewById(R.id.textViewExpenseDescription)
        private val categoryTextView: TextView = itemView.findViewById(R.id.textViewExpenseCategory)
        private val amountTextView: TextView = itemView.findViewById(R.id.textViewExpenseAmount)
        private val dateTextView: TextView = itemView.findViewById(R.id.textViewExpenseDate)
        private val receiptImageView: ImageView = itemView.findViewById(R.id.imageViewSmallReceipt)
        private var currentExpenseItemData: ExpenseWithCategory? = null

        init {
            itemView.setOnClickListener { currentExpenseItemData?.let { onItemClicked(it) } }
            itemView.setOnLongClickListener {
                currentExpenseItemData?.let { onItemLongClicked(it) }
                true
            }
        }

        fun bind(expenseItem: ExpenseWithCategory) {
            currentExpenseItemData = expenseItem
            val expense = expenseItem.expense
            val context = itemView.context

            descriptionTextView.text = expense.description
            categoryTextView.text = expenseItem.categoryName // From client-side combination

            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
            amountTextView.text = currencyFormat.format(expense.amount)

            val dateFormat = DateFormat.getMediumDateFormat(context)
            dateTextView.text = dateFormat.format(Date(expense.date))

            if (!expense.photoStoragePath.isNullOrEmpty()) {
                receiptImageView.visibility = View.VISIBLE
                val imageUrl = SupabaseImageService.getImageUrl(expense.photoStoragePath!!) // Get URL from service
                Glide.with(context)
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_image_placeholder) // Your placeholder
                    .error(R.drawable.ic_image_error)         // Your error drawable
                    .into(receiptImageView)
            } else {
                receiptImageView.visibility = View.GONE
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
