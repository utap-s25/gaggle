package edu.utap.gaggle.ui

import android.content.Intent
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
import edu.utap.gaggle.ConfettiActivity
import edu.utap.gaggle.R
import edu.utap.gaggle.viewmodel.GaggleViewModel
import edu.utap.gaggle.adapter.GroupedGaggleTaskAdapter
import edu.utap.gaggle.model.Task
import java.time.LocalDate

class ProgressFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GroupedGaggleTaskAdapter
    private val viewModel: GaggleViewModel by activityViewModels()

    private val firestore = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private val groupedTasks: MutableMap<String, MutableList<Task>> = mutableMapOf()

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
            task.timestamp = if (isChecked) LocalDate.now().toEpochDay() else 0
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
                val newTasks = mutableListOf<Task>()
                var remaining = taskTitles.size

                if (remaining == 0) {
                    groupedTasks[gaggleTitle] = newTasks
                    adapter.updateTasks(groupedTasks)
                    checkEmptyState()
                    return@addOnSuccessListener
                }

                for (taskTitle in taskTitles) {
                    val taskDocId = "${taskTitle}_$today"

                    val taskRef = firestore
                        .collection("tasks")
                        .document(userId!!)
                        .collection("userTasks")
                        .document(taskDocId)

                    taskRef.get()
                        .addOnSuccessListener { taskDoc ->
                            val completed = taskDoc.getBoolean("completed") == true
                            val timestamp = taskDoc.getLong("timestamp") ?: 0L

                            val task = Task(
                                title = taskTitle,
                                date = today,
                                completed = completed,
                                gaggleTitle = gaggleTitle,
                                userId = userId!!,
                                timestamp = timestamp
                            )
                            newTasks.add(task)

                            remaining--
                            if (remaining == 0) {
                                groupedTasks[gaggleTitle] = newTasks
                                adapter.updateTasks(groupedTasks)
                                checkEmptyState()

                                val allCompleted = newTasks.all { it.completed }
                                if (allCompleted) {
                                    handleStreakAndShowConfetti(gaggleId, gaggleTitle)
                                }
                            }
                        }
                        .addOnFailureListener {
                            remaining--
                            if (remaining == 0) {
                                groupedTasks[gaggleTitle] = newTasks
                                adapter.updateTasks(groupedTasks)
                                checkEmptyState()

                                val allCompleted = newTasks.all { it.completed }
                                if (allCompleted) {
                                    handleStreakAndShowConfetti(gaggleId, gaggleTitle)
                                }
                            }
                        }
                }
            }
    }

    private fun handleStreakAndShowConfetti(gaggleId: String, gaggleTitle: String) {
        val streakRef = firestore
            .collection("tasksCompleted")
            .document(userId!!)
            .collection("streaks")
            .document(gaggleId)

        streakRef.get().addOnSuccessListener { doc ->
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)
            val lastDateStr = doc.getString("lastCompletedDate")
            val lastDate = lastDateStr?.let { LocalDate.parse(it) }
            val currentStreak = doc.getLong("streakCount") ?: 0L

            val newStreak = if (lastDate == yesterday) currentStreak + 1 else 1

            streakRef.set(
                mapOf(
                    "lastCompletedDate" to today.toString(),
                    "streakCount" to newStreak
                )
            )

            showConfettiScreen(newStreak.toInt(), gaggleTitle)
        }
    }

    private fun showConfettiScreen(streak: Int, gaggleTitle: String) {
        val intent = Intent(requireContext(), ConfettiActivity::class.java)
        intent.putExtra("STREAK_COUNT", streak)
        intent.putExtra("GAGGLE_TITLE", gaggleTitle)
        startActivity(intent)
    }

    private fun markTaskComplete(task: Task) {
        val formattedDate = task.date.toString()
        val taskDocId = "${task.title}_$formattedDate"

        val taskRef = firestore
            .collection("tasks")
            .document(task.userId)
            .collection("userTasks")
            .document(taskDocId)

        val taskData = mapOf(
            "title" to task.title,
            "date" to formattedDate,
            "completed" to task.completed,
            "gaggleTitle" to task.gaggleTitle,
            "userId" to task.userId,
            "timestamp" to task.timestamp
        )

        taskRef.set(taskData)
    }

    private fun checkEmptyState() {
        val isEmpty = groupedTasks.values.flatten().isEmpty()
        view?.findViewById<TextView>(R.id.noTasksMessage)?.visibility =
            if (isEmpty) View.VISIBLE else View.GONE
    }
}
