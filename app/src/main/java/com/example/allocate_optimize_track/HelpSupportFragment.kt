package com.example.allocate_optimize_track

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HelpSupportFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HelpSupportFragment : Fragment() {
    private lateinit var recyclerViewBooks: RecyclerView
    private lateinit var bookAdapter: RecommendedBookAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_help_support, container, false)
        recyclerViewBooks = view.findViewById(R.id.recyclerViewRecommendedBooks)
        // textViewSupportEmail = view.findViewById(R.id.textViewSupportEmail) // If needed
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBookRecyclerView()
        loadRecommendedBooks()

        // Example: Making email clickable programmatically if autoLink isn't enough
        // textViewSupportEmail.setOnClickListener {
        //     try {
        //         val intent = Intent(Intent.ACTION_SENDTO)
        //         intent.data = Uri.parse("mailto:yoursupport@example.com")
        //         startActivity(intent)
        //     } catch (e: Exception) {
        //         Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
        //     }
        // }
    }

    private fun setupBookRecyclerView() {
        bookAdapter = RecommendedBookAdapter()
        recyclerViewBooks.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewBooks.adapter = bookAdapter
        recyclerViewBooks.isNestedScrollingEnabled = false // Important for RV inside NestedScrollView
    }

    private fun loadRecommendedBooks() {
        // In a real app, you might fetch this from a remote config, Firestore, or hardcode a larger list
        val books = listOf(
            RecommendedBook(
                title = "The Total Money Makeover: A Classic Edition",
                author = "Dave Ramsey",
                amazonLink = "https://www.amazon.co.za/Total-Money-Makeover-Classic-Financial/dp/1595555277", // Example link
                coverImageUrl = "https://m.media-amazon.com/images/I/81eCiKyn8IL._SY342_.jpg" // Example image URL
            ),
            RecommendedBook(
                title = "I Will Teach You to Be Rich, Second Edition",
                author = "Ramit Sethi",
                amazonLink = "https://www.amazon.com/Will-Teach-You-Rich-Second/dp/1523505745",
                coverImageUrl = "https://m.media-amazon.com/images/I/81c9SSbG3OL._SY425_.jpg"
            ),
            RecommendedBook(
                title = "Your Money or Your Life",
                author = "Vicki Robin, Joe Dominguez",
                amazonLink = "https://www.amazon.com/Your-Money-Life-Transforming-Relationship/dp/0143115766",
                coverImageUrl = "https://m.media-amazon.com/images/I/71AL7FJJw3L._SY425_.jpg"
            )
            // Add more books
        )
        bookAdapter.submitList(books)
    }
}