package edu.utap.gaggle.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import edu.utap.gaggle.databinding.FragmentCreateGaggleBinding
import edu.utap.gaggle.viewmodel.GaggleViewModel

class CreateGaggleFragment : Fragment() {

    private lateinit var binding: FragmentCreateGaggleBinding
    private val taskEditTexts = mutableListOf<EditText>()
    private val viewModel: GaggleViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentCreateGaggleBinding.inflate(inflater, container, false)

        // Add task input fields dynamically
        binding.btnAddTask.setOnClickListener {
            val taskEditText = EditText(requireContext()).apply {
                hint = "Task description"
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            taskEditTexts.add(taskEditText)
            binding.taskListContainer.addView(taskEditText)
        }

        // Create Gaggle when the create button is pressed
        binding.btnCreate.setOnClickListener {
            val title = binding.editTitle.text.toString().trim()
            val desc = binding.editDescription.text.toString().trim()

            // Get all tasks and filter out any blank ones
            val tasks = taskEditTexts.map { it.text.toString().trim() }.filter { it.isNotBlank() }

            // Get selected categories (ensure at least one is selected)
            val selectedCategories = mutableListOf<String>()
            if (binding.checkboxPhysical.isChecked) selectedCategories.add("physical")
            if (binding.checkboxMental.isChecked) selectedCategories.add("mental")
            if (binding.checkboxCreative.isChecked) selectedCategories.add("creative")
            if (binding.checkboxSocial.isChecked) selectedCategories.add("social")

            // Validate input: Title, description, at least one task, and at least one category must be provided
            if (title.isBlank() || desc.isBlank() || tasks.isEmpty() || selectedCategories.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill out all fields and add at least one task and category.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // If all checks pass, create the Gaggle
            viewModel.createGaggle(title, desc, selectedCategories, tasks)

            // Navigate back after creating the Gaggle
            findNavController().navigateUp()
        }

        return binding.root
    }
}
