package com.example.contactapp.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.contactapp.RoomDatabase.Contact
import com.example.contactapp.databinding.ItemContactBinding

class ContactAdapter : RecyclerView.Adapter<ContactAdapter.ViewHolder>() {



    private val list = ArrayList<Contact>()


    fun submitList(data : List<Contact>){
        list.clear()
        list.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(ItemContactBinding.inflate(LayoutInflater.from(parent.context) , parent , false))
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class ViewHolder(val binding : ItemContactBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(item : Contact){
            binding.name.text = item.name
            binding.number.text = item.number
        }
    }
}