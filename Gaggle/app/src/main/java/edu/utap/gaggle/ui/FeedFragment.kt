package edu.utap.gaggle.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.utap.gaggle.R
import edu.utap.gaggle.databinding.FragmentFeedBinding
import edu.utap.gaggle.adapter.FeedAdapter
import edu.utap.gaggle.adapter.GaggleMembersAdapter
import edu.utap.gaggle.viewmodel.FeedViewModel

class FeedFragment : Fragment() {
    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private val feedViewModel: FeedViewModel by viewModels()
    private lateinit var gaggleAdapter: GaggleMembersAdapter
    private lateinit var feedAdapter: FeedAdapter
    private lateinit var concatAdapter: ConcatAdapter // so we can move both feeds in one

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gaggleAdapter = GaggleMembersAdapter(emptyList())
        feedAdapter = FeedAdapter()

        concatAdapter = ConcatAdapter(gaggleAdapter, feedAdapter)
        binding.feedRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = concatAdapter
        }

        feedViewModel.feedItems.observe(viewLifecycleOwner, Observer { feedItems ->
            val filteredFeedItems = feedItems.filter { it.completed == true }

            if (filteredFeedItems.isEmpty()) {
                binding.emptyFeedPlaceholder.visibility = View.VISIBLE
            } else {
                binding.emptyFeedPlaceholder.visibility = View.GONE
            }

            feedAdapter.submitList(filteredFeedItems)
        })

        feedViewModel.gaggleMemberGroups.observe(viewLifecycleOwner) { gaggleGroups ->
            if (gaggleGroups.isEmpty()) {
                // Show the "No gaggles? Join one!" prompt if no gaggles
                binding.noGagglesPrompt.visibility = View.VISIBLE
            } else {
                // Hide the prompt if gaggles exist
                binding.noGagglesPrompt.visibility = View.GONE
            }

            gaggleAdapter.updateData(gaggleGroups)
        }
    }

    // Navigate to GaggleFragment when the user clicks the prompt
    fun onJoinGaggleClick(view: View) {
        val navController = findNavController()
        navController.navigate(R.id.gaggleFragment)
    }

    override fun onResume() {
        super.onResume()
        feedViewModel.refreshFeed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
