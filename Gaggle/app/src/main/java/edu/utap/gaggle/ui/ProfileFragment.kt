package edu.utap.gaggle.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.R
import androidx.navigation.fragment.findNavController
import edu.utap.gaggle.databinding.FragmentProfileBinding
import edu.utap.gaggle.model.UserPreferences
import edu.utap.gaggle.viewmodel.UserViewModel

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UserViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        binding.saveButton.setOnClickListener {
            val prefs = UserPreferences(
                wantsPhysical = binding.checkboxPhysical.isChecked,
                wantsMental = binding.checkboxMental.isChecked,
                wantsCreative = binding.checkboxCreative.isChecked,
                wantsSocial = binding.checkboxSocial.isChecked
            )
            Log.d("PROFILE", "Saving prefs and navigating to Gaggle: $prefs")
            viewModel.updatePreferences(prefs)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
