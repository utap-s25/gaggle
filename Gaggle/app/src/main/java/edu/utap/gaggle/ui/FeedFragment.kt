package edu.utap.gaggle.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.gaggle.adapter.FeedAdapter
import edu.utap.gaggle.databinding.FragmentFeedBinding
import edu.utap.gaggle.model.FeedItem
import java.time.LocalDate

class FeedFragment : Fragment() {
    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()
    private var feedAdapter: FeedAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        feedAdapter = FeedAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = feedAdapter

        loadFeedsForAllGaggles()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadFeedForGaggle(gaggleId: String) {
        val feedItems = mutableListOf<FeedItem>()
        val today = LocalDate.now()

        firestore.collection("gaggles")
            .document(gaggleId)
            .collection("tasks")
            .get()
            .addOnSuccessListener { taskDocs ->
                var taskRemaining = taskDocs.size()

                if (taskRemaining == 0) {
                    updateFeedUI(feedItems)
                    return@addOnSuccessListener
                }

                for (taskDoc in taskDocs) {
                    val taskTitle = taskDoc.id
                    taskDoc.reference.collection("completions")
                        .get()
                        .addOnSuccessListener { userDocs ->
                            for (userDoc in userDocs) {
                                val datesCompleted = userDoc.get("datesCompleted") as? Map<*, *> ?: continue
                                val userName = userDoc.getString("userName") ?: "Someone"
                                val timestamp = userDoc.getLong("timestamp") ?: 0L
                                val gaggleTitle = taskDoc.getString("gaggleTitle") ?: "Unnamed Gaggle"

                                if (datesCompleted[today.toString()] == true) {
                                    feedItems.add(
                                        FeedItem(
                                            userName = userName,
                                            gaggleTitle = gaggleTitle,
                                            taskTitle = taskTitle,
                                            date = today,
                                            timestamp = timestamp
                                        )
                                    )
                                }
                            }

                            taskRemaining--
                            if (taskRemaining == 0) {
                                feedItems.sortByDescending { it.timestamp }
                                updateFeedUI(feedItems)
                            }
                        }
                        .addOnFailureListener {
                            taskRemaining--
                            if (taskRemaining == 0) {
                                updateFeedUI(feedItems)
                            }
                        }
                }
            }
    }

    private fun loadFeedsForAllGaggles() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid.toString()

        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { userDoc ->
                val gaggleIds = userDoc.get("gaggles") as? List<String> ?: emptyList()

                for (gaggleId in gaggleIds) {
                    loadFeedForGaggle(gaggleId)
                }
            }
    }

    private fun updateFeedUI(feedItems: List<FeedItem>) {
        if (feedItems.isEmpty()) {
            // Show empty state view if you have one
        } else {
            feedAdapter?.submitList(feedItems)
        }
    }

}
