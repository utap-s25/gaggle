package edu.utap.gaggle.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.utap.gaggle.databinding.FragmentFeedBinding
import edu.utap.gaggle.adapter.FeedAdapter
import edu.utap.gaggle.viewmodel.FeedViewModel

class FeedFragment : Fragment() {
    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private val feedViewModel: FeedViewModel by viewModels()
    private lateinit var feedAdapter: FeedAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        feedAdapter = FeedAdapter()
        binding.feedRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = feedAdapter
        }

        feedViewModel.feedItems.observe(viewLifecycleOwner, Observer { feedItems ->
            feedAdapter.submitList(feedItems)
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
