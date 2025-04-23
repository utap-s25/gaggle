package edu.utap.gaggle.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.utap.gaggle.R
import edu.utap.gaggle.model.Task

class GroupedGaggleTaskAdapter(
    private var groupedTasks: Map<String, List<Task>>,
    private val onCheckChanged: (Task, Boolean) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val itemList = mutableListOf<Any>() // Mix of String (header) and GaggleTask

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_TASK = 1
    }

    init {
        rebuildItemList()
    }

    fun updateTasks(newGroupedTasks: Map<String, List<Task>>) {
        groupedTasks = newGroupedTasks
        rebuildItemList()
        notifyDataSetChanged()
    }

    private fun rebuildItemList() {
        itemList.clear()
        for ((gaggleTitle, taskList) in groupedTasks) {
            itemList.add(gaggleTitle)
            itemList.addAll(taskList)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (itemList[position] is String) TYPE_HEADER else TYPE_TASK
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_gaggle_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_task, parent, false)
            TaskViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            holder.bind(itemList[position] as String)
        } else if (holder is TaskViewHolder) {
            holder.bind(itemList[position] as Task)
        }
    }

    override fun getItemCount(): Int = itemList.size

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleView: TextView = view.findViewById(R.id.gaggleTitle)
        fun bind(title: String) {
            titleView.text = title
        }
    }

    inner class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleView: TextView = view.findViewById(R.id.taskTitle)
        private val checkBox: CheckBox = view.findViewById(R.id.taskCheckbox)

        fun bind(task: Task) {
            titleView.text = task.title
            checkBox.isChecked = task.completed
            checkBox.setOnCheckedChangeListener(null)
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                onCheckChanged(task, isChecked)
            }
        }
    }
}
