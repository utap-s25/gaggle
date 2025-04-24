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

        binding.btnAddTask.setOnClickListener {
            val taskEditText = EditText(requireContext()).apply {
                hint = "Task description"
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            taskEditTexts.add(taskEditText)
            binding.taskListContainer.addView(taskEditText)
        }

        binding.btnCreate.setOnClickListener {
            val title = binding.editTitle.text.toString().trim()
            val desc = binding.editDescription.text.toString().trim()

            val tasks = taskEditTexts.map { it.text.toString().trim() }.filter { it.isNotBlank() }

            val selectedCategories = mutableListOf<String>()
            if (binding.checkboxPhysical.isChecked) selectedCategories.add("physical")
            if (binding.checkboxMental.isChecked) selectedCategories.add("mental")
            if (binding.checkboxCreative.isChecked) selectedCategories.add("creative")
            if (binding.checkboxSocial.isChecked) selectedCategories.add("social")

            if (title.isBlank() || desc.isBlank() || tasks.isEmpty() || selectedCategories.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill out all fields and add at least one task and category.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.createGaggle(title, desc, selectedCategories, tasks)

            findNavController().navigateUp()
        }

        return binding.root
    }
}
