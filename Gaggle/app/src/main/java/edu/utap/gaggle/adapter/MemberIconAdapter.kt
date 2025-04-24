package edu.utap.gaggle.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import edu.utap.gaggle.R
import edu.utap.gaggle.model.MemberIcon

class MemberIconAdapter(
    private val members: List<MemberIcon>
) : RecyclerView.Adapter<MemberIconAdapter.MemberViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_member_icon, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(members[position])
    }

    override fun getItemCount(): Int = members.size

    inner class MemberViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val image: ImageView = view.findViewById(R.id.memberImage)
        private val name: TextView = view.findViewById(R.id.memberName)

        fun bind(member: MemberIcon) {
            name.text = member.username
            if (member.profileImageUrl != null) {
                Glide.with(image.context).load(member.profileImageUrl).into(image)
            } else {
                image.setImageResource(R.drawable.gaggle_logo)
            }
        }
    }
}
