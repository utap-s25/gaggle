package edu.utap.gaggle.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import android.content.Context
import android.widget.LinearLayout
import android.widget.Toast
import java.time.format.DateTimeFormatter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
        checkIfNewDayAndReset()
        return inflater.inflate(R.layout.fragment_progress, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.progressRecyclerView)
        adapter = GroupedGaggleTaskAdapter(groupedTasks) { task, isChecked ->
            task.completed = isChecked
            task.timestamp = if (isChecked) System.currentTimeMillis() else 0
            if (isChecked) {
                Log.d("ProgressFragment", "Marking task as complete: $task")
                markTaskComplete(task)
            } else {
                Log.d("ProgressFragment", "Marking task as incomplete: $task")
                markTaskComplete(task)
            }
            updateUIState()
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        viewModel.userGaggles.observe(viewLifecycleOwner, Observer { gaggles ->
            Log.d("ProgressFragment", "User Gaggles: $gaggles")
            groupedTasks.clear()

            if (gaggles.isNullOrEmpty()) {
                Log.d("ProgressFragment", "gaggles.isNullOrEmpty()")
                // No gaggles joined
                updateUIState()
                return@Observer
            }

            // Load tasks for each gaggle
            gaggles.forEach { gaggleId ->
                loadTasksForGaggle(gaggleId)
            }
            updateUIState()  // Ensure UI is updated after checking gaggles
        })
    }

    private fun checkIfNewDayAndReset() {
        val prefs = requireContext().getSharedPreferences("gaggle_prefs", Context.MODE_PRIVATE)
        val lastDateString = prefs.getString("last_opened_date", null)
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ISO_DATE

        if (lastDateString == null || LocalDate.parse(lastDateString, formatter) != today) {
            resetTaskUI()

            prefs.edit().putString("last_opened_date", today.format(formatter)).apply()
        }
    }

    private fun loadTasksForGaggle(gaggleId: String) {
        val confettiContainer = view?.findViewById<View>(R.id.confettiContainer)
        confettiContainer?.visibility = View.GONE
        val recycler = view?.findViewById<RecyclerView>(R.id.progressRecyclerView)
        recycler?.visibility = View.VISIBLE

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
                    updateUIState()
                    return@addOnSuccessListener
                }

                for (taskTitle in taskTitles) {
                    val taskDocId = "${taskTitle}_$today"

                    val taskRef = firestore
                        .collection("tasks")
                        .document(userId!!)
                        .collection("userTasks")
                        .document(taskDocId)

                    taskRef.addSnapshotListener { taskDoc, _ ->
                        if (taskDoc != null && taskDoc.exists()) {
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
                            val taskList = groupedTasks.getOrPut(gaggleTitle) { mutableListOf() }
                            val existingIndex = taskList.indexOfFirst { it.title == task.title }
                            if (existingIndex >= 0) {
                                taskList[existingIndex] = task
                            } else {
                                taskList.add(task)
                            }

                            adapter.updateTasks(groupedTasks)
                            updateUIState()


                            val allCompleted = taskList.all { it.completed }
                            if (allCompleted) {
                                handleStreakAndShowConfetti(gaggleId, gaggleTitle)
                            }
                        } else {
                            val newTask = Task(
                                title = taskTitle,
                                date = today,
                                completed = false,
                                gaggleTitle = gaggleTitle,
                                userId = userId!!,
                                timestamp = System.currentTimeMillis()
                            )

                            taskRef.set(newTask).addOnSuccessListener {
                            }.addOnFailureListener { exception ->
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
        }
    }

    private fun markTaskComplete(task: Task) {
        Log.d("ProgressFragment", "Marking task as complete: $task")
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

    private fun updateUIState() {
        val allTasks = groupedTasks.values.flatten()
        val allCompleted = allTasks.isNotEmpty() && allTasks.all { it.completed }
        Log.d("ProgressFragment", "All Tasks: $allTasks")
        val isEmpty = allTasks.isEmpty()
        Log.d("ProgressFragment", "isEmpty: $isEmpty")

        val recycler = view?.findViewById<RecyclerView>(R.id.progressRecyclerView)
        val container = view?.findViewById<LinearLayout>(R.id.noTasksContainer)
        val message = view?.findViewById<TextView>(R.id.noTasksMessage)

        if (viewModel.userGaggles.value.isNullOrEmpty()) {
            message?.text = "You haven't joined any Gaggles yet!"  // User hasn't joined any gaggles
        } else {
            message?.text = "No tasks found for today."  // There are gaggles, but no tasks for the day
        }

        recycler?.visibility = if (isEmpty) View.GONE else View.VISIBLE
        container?.visibility = if (isEmpty) View.VISIBLE else View.GONE
        message?.visibility = if (isEmpty) View.VISIBLE else View.GONE

        if (allCompleted && !isEmpty) {
            showConfettiInline()
        }
    }

    private fun resetTaskUI() {
        val recycler = view?.findViewById<RecyclerView>(R.id.progressRecyclerView)
        recycler?.visibility = View.VISIBLE
    }

    private fun hasShownToastToday(): Boolean {
        val prefs = requireContext().getSharedPreferences("gaggle_prefs", Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        val lastShownDate = prefs.getString("toast_shown_date", null)
        return lastShownDate == today
    }

    private fun markToastShownToday() {
        val prefs = requireContext().getSharedPreferences("gaggle_prefs", Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        prefs.edit().putString("toast_shown_date", today).apply()
    }

    private fun showConfettiInline() {
        if (!hasShownToastToday()) {
            Toast.makeText(requireContext(), "You completed all your tasks today! 🎉", Toast.LENGTH_SHORT).show()
            markToastShownToday()
        }
    }
}
