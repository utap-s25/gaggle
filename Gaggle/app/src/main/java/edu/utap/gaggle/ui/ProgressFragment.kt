package edu.utap.gaggle.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import edu.utap.gaggle.R
import edu.utap.gaggle.adapter.GaggleTaskAdapter
import edu.utap.gaggle.model.GaggleTask
import java.time.LocalDate

class ProgressFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GaggleTaskAdapter
    private lateinit var emptyView: TextView
    private lateinit var completeAllButton: Button

    private var tasks: MutableList<GaggleTask> = mutableListOf()
    private val firestore = FirebaseFirestore.getInstance()

    // These should be passed to the fragment, set via arguments or ViewModel
    private val gaggleId: String = "OiweflJ5yff7gzX0Em7e" // TODO: Replace with real gaggleId
    private val userId: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_progress, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.progressRecyclerView)
        emptyView = view.findViewById(R.id.noTasksMessage)
        completeAllButton = view.findViewById(R.id.btnCompleteAll)

        adapter = GaggleTaskAdapter(tasks) { task, isChecked ->
            task.completed = isChecked
            markTaskComplete(task)
            checkEmptyState()
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        completeAllButton.setOnClickListener {
            tasks.forEach { task ->
                task.completed = true
                markTaskComplete(task)
            }
            adapter.updateTasks(tasks)
        }

        loadTasks()
    }

    private fun markTaskComplete(task: GaggleTask) {
        val today = LocalDate.now().toString()
        val uid = userId ?: return
        val taskId = task.title ?: return // Assuming each GaggleTask has a unique ID

        val taskRef = firestore.collection("gaggles")
            .document(gaggleId)
            .collection("tasks")
            .document(taskId)
            .collection("completions")
            .document(uid)

        taskRef.set(mapOf("datesCompleted.$today" to true), SetOptions.merge())
    }

    private fun loadTasks() {
        if (gaggleId.isBlank() || userId == null) return

        val today = LocalDate.now()

        firestore.collection("gaggles")
            .document(gaggleId)
            .get()
            .addOnSuccessListener { document ->
                val taskTitles = document.get("tasks") as? List<String> ?: emptyList()
                val newTasks = mutableListOf<GaggleTask>()

                var remaining = taskTitles.size
                if (remaining == 0) {
                    adapter.updateTasks(newTasks)
                    checkEmptyState()
                    return@addOnSuccessListener
                }

                for (taskTitle in taskTitles) {
                    Log.d("Jisoo", "taskTitle: $taskTitle")
                    Log.d("Jisoo", "userId: $userId")
                    Log.d("Jisoo", "today: $today")
                    val completionRef = firestore.collection("gaggles")
                        .document(gaggleId)
                        .collection("tasks")
                        .document(taskTitle)
                        .collection("completions")
                        .document(userId!!)

                    completionRef.get()
                        .addOnSuccessListener { completionDoc ->
                            Log.d("Jisoo", "completionRef succeeded")
                            val completed =
                                completionDoc.getBoolean("datesCompleted.$today") == true
                            val task = GaggleTask(
                                title = taskTitle,
                                date = today,
                                completed = completed
                            )
                            newTasks.add(task)

                            remaining--
                            if (remaining == 0) {
                                adapter.updateTasks(newTasks)
                                checkEmptyState()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("gaggle", "Error loading completion for $taskTitle", e)
                            remaining--
                            if (remaining == 0) {
                                adapter.updateTasks(newTasks)
                                checkEmptyState()
                            }
                        }

                }
            }
            .addOnFailureListener { e ->
                Log.e("gaggle", "Failed to load gaggle", e)
            }
    }

    private fun fetchCompletionStatus(taskList: MutableList<GaggleTask>) {
        val uid = userId ?: return
        val today = LocalDate.now().toString()

        val updatedTasks = mutableListOf<GaggleTask>()
        var completedCount = 0
        val total = taskList.size

        taskList.forEach { task ->
            val taskRef = firestore.collection("gaggles")
                .document(gaggleId)
                .collection("tasks")
                .document(task.title) // assuming title is unique

            taskRef.collection("completions")
                .document(uid)
                .get()
                .addOnSuccessListener { snapshot ->
                    val isCompletedToday = snapshot.getBoolean("datesCompleted.$today") == true
                    task.completed = isCompletedToday
                    updatedTasks.add(task)
                    completedCount++
                    if (completedCount == total) {
                        tasks = updatedTasks
                        adapter.updateTasks(tasks)
                        checkEmptyState()
                    }
                }
                .addOnFailureListener {
                    completedCount++
                    if (completedCount == total) {
                        tasks = updatedTasks
                        adapter.updateTasks(tasks)
                        checkEmptyState()
                    }
                }
        }
    }



    private fun checkEmptyState() {
        val hasTasks = tasks.isNotEmpty()
        emptyView.visibility = if (hasTasks) View.GONE else View.VISIBLE
        completeAllButton.visibility = if (hasTasks) View.VISIBLE else View.GONE
    }
}
