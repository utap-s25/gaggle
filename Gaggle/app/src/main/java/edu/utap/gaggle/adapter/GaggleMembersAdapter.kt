package edu.utap.gaggle.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.utap.gaggle.R
import edu.utap.gaggle.model.GaggleMemberGroup

class GaggleMembersAdapter(
    private var gaggleGroups: List<GaggleMemberGroup>
) : RecyclerView.Adapter<GaggleMembersAdapter.GaggleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GaggleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gaggle_members, parent, false)
        return GaggleViewHolder(view)
    }

    override fun onBindViewHolder(holder: GaggleViewHolder, position: Int) {
        holder.bind(gaggleGroups[position])
    }

    override fun getItemCount(): Int = gaggleGroups.size

    fun updateData(newData: List<GaggleMemberGroup>) {
        this.gaggleGroups = newData
        notifyDataSetChanged() // or use DiffUtil for efficiency
    }


    inner class GaggleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.gaggleTitle)
        private val membersRecyclerView: RecyclerView = view.findViewById(R.id.membersRecyclerView)

        fun bind(group: GaggleMemberGroup) {
            title.text = group.gaggleTitle
            membersRecyclerView.layoutManager = GridLayoutManager(itemView.context, 3)
            membersRecyclerView.adapter = MemberIconAdapter(group.members)
        }
    }
}
