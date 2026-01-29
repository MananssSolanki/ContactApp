package com.example.contactapp.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.contactapp.Model.PhoneContact
import com.example.contactapp.databinding.ItemContactBinding

class PhoneContactAdapter : RecyclerView.Adapter<PhoneContactAdapter.ViewHolder>() {

    private val list = ArrayList<PhoneContact>()

    fun submitList(data: List<PhoneContact>) {
        list.clear()
        list.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemContactBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class ViewHolder(val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PhoneContact) {
            binding.name.text = item.name
            binding.number.text = item.phoneNumber
        }
    }
}
