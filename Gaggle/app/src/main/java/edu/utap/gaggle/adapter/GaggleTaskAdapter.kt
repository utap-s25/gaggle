package edu.utap.gaggle.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.gaggle.R
import edu.utap.gaggle.model.GaggleTask
import java.time.LocalDate

class GaggleTaskAdapter(
    private var tasks: List<GaggleTask>,
    private val gaggleId: String,
    private val onTaskChecked: (GaggleTask, Boolean) -> Unit
) : RecyclerView.Adapter<GaggleTaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val taskTitle: TextView = view.findViewById(R.id.taskTitle)
        val taskCheckBox: CheckBox = view.findViewById(R.id.taskCheckbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gaggle_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        holder.taskTitle.text = task.title
        holder.taskCheckBox.setOnCheckedChangeListener(null)
        holder.taskCheckBox.isChecked = task.date.toString() == LocalDate.now().toString()

        holder.taskCheckBox.setOnCheckedChangeListener { _, isChecked ->
            onTaskChecked(task, isChecked)
        }
    }

    override fun getItemCount(): Int = tasks.size

    fun updateTasks(newTasks: List<GaggleTask>) {
        tasks = newTasks
        notifyDataSetChanged()
    }
}