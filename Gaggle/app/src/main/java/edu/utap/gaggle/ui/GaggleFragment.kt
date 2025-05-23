
package edu.utap.gaggle.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.gaggle.R
import edu.utap.gaggle.adapter.GaggleAdapter
import edu.utap.gaggle.databinding.FragmentGaggleBinding
import edu.utap.gaggle.model.Gaggle
import edu.utap.gaggle.viewmodel.GaggleViewModel
import edu.utap.gaggle.viewmodel.UserViewModel
class GaggleFragment : Fragment() {
    private var _binding: FragmentGaggleBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UserViewModel by activityViewModels()
    private val gaggleViewModel: GaggleViewModel by activityViewModels()
    private lateinit var adapter: GaggleAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onResume() {
        super.onResume()
        viewModel.loadPreferences()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGaggleBinding.inflate(inflater, container, false)

        adapter = GaggleAdapter(gaggleViewModel)
        binding.gaggleRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.gaggleRecyclerView.adapter = adapter

        binding.goToProfileButton.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }

        binding.fabCreateGaggle.setOnClickListener {
            val parentNavController = parentFragment?.findNavController()
            parentNavController?.navigate(R.id.action_gaggleFragment_to_createGaggleFragment)
        }

        viewModel.loadPreferences()
        viewModel.preferences.observe(viewLifecycleOwner) { prefs ->
            Log.d("GAGGLEW", "Preferences observed: $prefs")

            val selected = listOfNotNull(
                if (prefs.wantsPhysical) "physical" else null,
                if (prefs.wantsMental) "mental" else null,
                if (prefs.wantsCreative) "creative" else null,
                if (prefs.wantsSocial) "social" else null
            )

            Log.d("GAGGLEW", "Selected categories: $selected")

            if (selected.isEmpty()) {
                showEmptyState()
            } else {
                fetchGagglesMatching(selected)
            }
        }

        return binding.root
    }

    private fun showEmptyState() {
        _binding?.let { binding ->
            binding.emptyStateContainer.isVisible = true
            binding.gaggleRecyclerView.isVisible = false
            binding.fabCreateGaggle.isVisible = false
        }
    }

    private fun showGaggles(gaggles: List<Gaggle>) {
        _binding?.let { binding ->
            binding.emptyStateContainer.isVisible = false
            binding.gaggleRecyclerView.isVisible = true
            binding.fabCreateGaggle.isVisible = true
            Log.d("GAGGLE", "Showing gaggles: $gaggles")
            adapter.updateGaggles(gaggles)
        }
    }

    private fun fetchGagglesMatching(categories: List<String>) {
        Log.d("GAGGLEW", "Fetching gaggles with categories: $categories")
        db.collection("gaggles")
            .whereArrayContainsAny("categories", categories)
            .get()
            .addOnSuccessListener { result ->
                val gaggleList = result.mapNotNull { it.toObject(Gaggle::class.java) }
                if (view != null && isAdded) {
                    showGaggles(gaggleList)
                }
            }
            .addOnFailureListener { e ->
                Log.e("GAGGLEW", "Firestore fetch failed", e)
                showEmptyState()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("GAGGLE", "onDestroyView called, nulling _binding")
        _binding = null
    }
}
