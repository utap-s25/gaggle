package edu.utap.gaggle.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import edu.utap.gaggle.R
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import edu.utap.gaggle.LoginFragment
import edu.utap.gaggle.MainActivity
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

        viewModel.loadPreferences()

        viewModel.preferences.observe(viewLifecycleOwner) { prefs ->
            prefs?.let {
                binding.checkboxPhysical.isChecked = it.wantsPhysical
                binding.checkboxMental.isChecked = it.wantsMental
                binding.checkboxCreative.isChecked = it.wantsCreative
                binding.checkboxSocial.isChecked = it.wantsSocial

                val defaultName = FirebaseAuth.getInstance().currentUser?.email ?: ""
                binding.editUsername.setText(if (it.username.isBlank()) defaultName else it.username)
            }
        }


        binding.saveButton.setOnClickListener {
            val name = if (binding.editUsername.text.isBlank()) FirebaseAuth.getInstance().currentUser?.email ?: "" else binding.editUsername.text.toString()
            val prefs = UserPreferences(
                username = name,
                wantsPhysical = binding.checkboxPhysical.isChecked,
                wantsMental = binding.checkboxMental.isChecked,
                wantsCreative = binding.checkboxCreative.isChecked,
                wantsSocial = binding.checkboxSocial.isChecked
            )
            Log.d("PROFILE", "Saving prefs and navigating to Gaggle: $prefs")
            viewModel.updatePreferences(prefs)
        }


        binding.logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
