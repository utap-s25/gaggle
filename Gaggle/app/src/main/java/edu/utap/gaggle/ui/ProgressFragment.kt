package edu.utap.gaggle.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.gaggle.R
import edu.utap.gaggle.model.GaggleViewModel
import edu.utap.gaggle.adapter.GroupedGaggleTaskAdapter
import edu.utap.gaggle.model.GaggleTask
import java.time.LocalDate

class ProgressFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GroupedGaggleTaskAdapter
    private val viewModel: GaggleViewModel by activityViewModels()

    private val firestore = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private val groupedTasks: MutableMap<String, MutableList<GaggleTask>> = mutableMapOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_progress, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.progressRecyclerView)
        adapter = GroupedGaggleTaskAdapter(groupedTasks) { task, isChecked ->
            task.completed = isChecked
            markTaskComplete(task)
            checkEmptyState()
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        viewModel.userGaggles.observe(viewLifecycleOwner, Observer { gaggles ->
            groupedTasks.clear()
            gaggles.forEach { gaggleId ->
                loadTasksForGaggle(gaggleId)
            }
        })
    }

    private fun loadTasksForGaggle(gaggleId: String) {
        firestore.collection("gaggles")
            .document(gaggleId)
            .get()
            .addOnSuccessListener { document ->
                val taskTitles = document.get("tasks") as? List<String> ?: emptyList()
                val gaggleTitle = document.getString("title") ?: "Unnamed Gaggle"
                val today = LocalDate.now()
                val newTasks = mutableListOf<GaggleTask>()
                var remaining = taskTitles.size

                if (remaining == 0) {
                    groupedTasks[gaggleTitle] = newTasks
                    adapter.updateTasks(groupedTasks)
                    checkEmptyState()
                    return@addOnSuccessListener
                }

                for (taskTitle in taskTitles) {
                    val completionRef = firestore.collection("gaggles")
                        .document(gaggleId)
                        .collection("tasks")
                        .document(taskTitle)
                        .collection("completions")
                        .document(userId!!)

                    completionRef.get()
                        .addOnSuccessListener { completionDoc ->
                            val completed = completionDoc.getBoolean("datesCompleted.$today") == true
                            val task = GaggleTask(
                                title = taskTitle,
                                date = today,
                                completed = completed,
                                gaggleTitle = gaggleTitle
                            )
                            newTasks.add(task)

                            remaining--
                            if (remaining == 0) {
                                groupedTasks[gaggleTitle] = newTasks
                                adapter.updateTasks(groupedTasks)
                                checkEmptyState()
                            }
                        }
                        .addOnFailureListener {
                            remaining--
                            if (remaining == 0) {
                                groupedTasks[gaggleTitle] = newTasks
                                adapter.updateTasks(groupedTasks)
                                checkEmptyState()
                            }
                        }
                }
            }
    }

    private fun markTaskComplete(task: GaggleTask) {
        val gaggleId = viewModel.gaggles.value?.firstOrNull { it.title == task.gaggleTitle }?.id
        if (gaggleId != null) {
            val completionRef = firestore.collection("gaggles")
                .document(gaggleId)
                .collection("tasks")
                .document(task.title)
                .collection("completions")
                .document(userId!!)

            completionRef.get().addOnSuccessListener { doc ->
                val updates = doc.get("datesCompleted") as? MutableMap<String, Boolean> ?: mutableMapOf()
                updates[task.date.toString()] = task.completed
                completionRef.set(mapOf("datesCompleted" to updates))
            }
        }
    }

    private fun checkEmptyState() {
        val isEmpty = groupedTasks.values.flatten().isEmpty()
        view?.findViewById<TextView>(R.id.noTasksMessage)?.visibility =
            if (isEmpty) View.VISIBLE else View.GONE
    }
}
