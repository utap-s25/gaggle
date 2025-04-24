package edu.utap.gaggle.adapter

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import edu.utap.gaggle.databinding.ItemGaggleBinding
import edu.utap.gaggle.model.Gaggle
import edu.utap.gaggle.viewmodel.GaggleViewModel

class GaggleAdapter(private val viewModel: GaggleViewModel) :
    RecyclerView.Adapter<GaggleAdapter.GaggleViewHolder>() {

    private var gaggles: List<Gaggle> = listOf()

    inner class GaggleViewHolder(private val binding: ItemGaggleBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(gaggle: Gaggle) {
            binding.gaggleTitle.text = gaggle.title
            binding.gaggleDescription.text = gaggle.description

            val isInitiallyInGaggle = viewModel.userGaggles.value?.contains(gaggle.id) == true
            binding.joinLeaveButton.text = if (isInitiallyInGaggle) "Leave" else "Join"

            binding.joinLeaveButton.setOnClickListener {
                val currentlyInGaggle = viewModel.userGaggles.value?.contains(gaggle.id) == true

                showJoinConfirmationDialog(currentlyInGaggle, binding.root.context) {
                    val successFlag: Boolean
                    val newButtonText: String
                    if (currentlyInGaggle) {
                        successFlag = viewModel.leaveGaggle(gaggle.id)
                        newButtonText = "Join"
                    } else {
                        successFlag = viewModel.joinGaggle(gaggle.id)
                        newButtonText = "Leave"
                    }
                    if (successFlag) {
                        binding.joinLeaveButton.text = newButtonText
                    }
                }
            }
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
        Log.d("GAGGLE_ADAPTER", "Updated list: $newList")
        gaggles = newList
        notifyDataSetChanged()
    }

    private fun showJoinConfirmationDialog(leaveFlag: Boolean, context: Context, onConfirm: () -> Unit) {
        val verb = if (leaveFlag) "leave" else "join"
        val verbCamel = if (leaveFlag) "Leave" else "Join"
        AlertDialog.Builder(context)
            .setTitle("$verbCamel Gaggle")
            .setMessage("Are you sure you want to $verb this gaggle?")
            .setPositiveButton(verbCamel) { _, _ -> onConfirm() }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
