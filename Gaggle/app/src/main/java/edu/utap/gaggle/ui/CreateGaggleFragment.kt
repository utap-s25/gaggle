package edu.utap.gaggle.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import edu.utap.gaggle.databinding.FragmentCreateGaggleBinding
import edu.utap.gaggle.model.GaggleViewModel

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
            val title = binding.editTitle.text.toString()
            val desc = binding.editDescription.text.toString()
            val tasks = taskEditTexts.map { it.text.toString() }.filter { it.isNotBlank() }
            val categories = listOf("physical") // or pull from checkboxes / chips

            viewModel.createGaggle(title, desc, categories, tasks)
            findNavController().navigateUp()
        }

        return binding.root
    }
}
