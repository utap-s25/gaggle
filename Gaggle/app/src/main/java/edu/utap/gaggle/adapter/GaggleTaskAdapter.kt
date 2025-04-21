package edu.utap.gaggle.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import edu.utap.gaggle.R
import edu.utap.gaggle.model.GaggleTask

class GaggleTaskAdapter(
    private var tasks: List<GaggleTask>,
    private val onTaskChecked: (GaggleTask, Boolean) -> Unit
) : RecyclerView.Adapter<GaggleTaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkbox: CheckBox = itemView.findViewById(R.id.taskCheckbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gaggle_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.checkbox.text = task.title
        holder.checkbox.isChecked = task.completed
        holder.checkbox.setOnCheckedChangeListener(null) // prevent triggering old listener
        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            onTaskChecked(task, isChecked)
        }
    }

    override fun getItemCount(): Int = tasks.size

    fun updateTasks(newTasks: List<GaggleTask>) {
        tasks = newTasks
        notifyDataSetChanged()
    }
}