package edu.utap.gaggle.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.gaggle.R
import edu.utap.gaggle.adapter.GaggleAdapter
import edu.utap.gaggle.databinding.FragmentGaggleBinding
import edu.utap.gaggle.model.Gaggle
import edu.utap.gaggle.viewmodel.UserViewModel

class GaggleFragment : Fragment() {
    private var _binding: FragmentGaggleBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UserViewModel by activityViewModels()
    private lateinit var adapter: GaggleAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGaggleBinding.inflate(inflater, container, false)

        adapter = GaggleAdapter(emptyList())
        binding.gaggleRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.gaggleRecyclerView.adapter = adapter

        binding.goToProfileButton.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }

        viewModel.preferences.observe(viewLifecycleOwner) { prefs ->
            Log.d("GAGGLEW", "Preferences observed: $prefs")

            val selected = listOfNotNull(
                if (prefs.wantsPhysical) "physical" else null,
                if (prefs.wantsMental) "mental" else null,
                if (prefs.wantsCreative) "creative" else null,
                if (prefs.wantsSocial) "social" else null
            )

            Log.d("GAGGLEW", "Selected: $selected")

            if (selected.isEmpty()) {
                showEmptyState()
            } else {
                fetchGagglesMatching(selected)
            }
        }

        return binding.root
    }

    private fun showEmptyState() {
        binding.emptyStateContainer.isVisible = true
        binding.gaggleRecyclerView.isVisible = false
    }

    private fun showGaggles(gaggles: List<Gaggle>) {
        binding.emptyStateContainer.isVisible = false
        binding.gaggleRecyclerView.isVisible = true
        Log.d("GAGGLE", "hey we made it this far")
        adapter.updateGaggles(gaggles)
    }

    private fun fetchGagglesMatching(categories: List<String>) {
        Log.d("GAGGLEW", "Fetching gaggles with categories: $categories")
        db.collection("gaggles")
            .whereArrayContainsAny("categories", categories)
            .get()
            .addOnSuccessListener { result ->
                val gaggleList = result.mapNotNull { it.toObject(Gaggle::class.java) }
                Log.d("GAGGLEW", "Fetched gaggles: $gaggleList")
                showGaggles(gaggleList)
            }
            .addOnFailureListener { e ->
                Log.e("GAGGLEW", "Firestore fetch failed", e)
                showEmptyState()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
