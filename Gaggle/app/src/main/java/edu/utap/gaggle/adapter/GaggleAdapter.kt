package edu.utap.gaggle.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import edu.utap.gaggle.databinding.ItemGaggleBinding
import edu.utap.gaggle.model.Gaggle

class GaggleAdapter(private var gaggles: List<Gaggle>) :
    RecyclerView.Adapter<GaggleAdapter.GaggleViewHolder>() {

    inner class GaggleViewHolder(private val binding: ItemGaggleBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(gaggle: Gaggle) {
            binding.gaggleTitle.text = gaggle.title
            binding.gaggleDescription.text = gaggle.description
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GaggleViewHolder {
        val binding = ItemGaggleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GaggleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GaggleViewHolder, position: Int) {
        holder.bind(gaggles[position])
    }

    override fun getItemCount() = gaggles.size

    fun updateGaggles(newList: List<Gaggle>) {
        Log.d("GAGGLE", "akmu 200% $newList")
        gaggles = newList
        notifyDataSetChanged()
    }
}
