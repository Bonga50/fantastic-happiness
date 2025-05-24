package com.example.allocate_optimize_track

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class RecommendedBookAdapter :
    ListAdapter<RecommendedBook, RecommendedBookAdapter.BookViewHolder>(BookDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recommended_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.textViewBookTitle)
        private val authorTextView: TextView = itemView.findViewById(R.id.textViewBookAuthor)
        private val coverImageView: ImageView = itemView.findViewById(R.id.imageViewBookCover)
        private val linkImageView: ImageView = itemView.findViewById(R.id.imageViewOpenLink)
        private val context = itemView.context

        fun bind(book: RecommendedBook) {
            titleTextView.text = book.title
            authorTextView.text = "By ${book.author}"

            if (!book.coverImageUrl.isNullOrEmpty()) {
                Glide.with(context)
                    .load(book.coverImageUrl)
                    .placeholder(R.drawable.library_books_24px) // Your placeholder
                    .error(R.drawable.library_books_24px)       // Fallback
                    .into(coverImageView)
            } else {
                coverImageView.setImageResource(R.drawable.library_books_24px)
            }

            itemView.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(book.amazonLink))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Handle error, e.g., show a Toast
                    Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                }
            }
            linkImageView.setOnClickListener { itemView.performClick() } // Make icon also clickable
        }
    }

    class BookDiffCallback : DiffUtil.ItemCallback<RecommendedBook>() {
        override fun areItemsTheSame(oldItem: RecommendedBook, newItem: RecommendedBook): Boolean {
            return oldItem.amazonLink == newItem.amazonLink // Assuming link is unique
        }
        override fun areContentsTheSame(oldItem: RecommendedBook, newItem: RecommendedBook): Boolean {
            return oldItem == newItem
        }
    }
}