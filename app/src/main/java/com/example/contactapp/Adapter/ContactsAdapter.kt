package com.example.contactapp.Adapter

import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.contactapp.Model.Contact
import com.example.contactapp.databinding.ItemContactBinding
import java.util.Random

class ContactsAdapter(
    private val onContactClick: (Contact) -> Unit,
    private val onCallClick: (Contact) -> Unit
) : ListAdapter<Contact, ContactsAdapter.ContactViewHolder>(ContactDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ContactViewHolder(private val binding: ItemContactBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(contact: Contact) {
            binding.tvContactName.text = contact.name
            binding.tvPhoneNumber.text = contact.phoneNumber

            if (contact.photoUri != null) {
                binding.ivContactPhoto.visibility = View.VISIBLE
                binding.tvContactInitial.visibility = View.GONE
                Glide.with(binding.root.context)
                    .load(contact.photoUri)
                    .circleCrop()
                    .into(binding.ivContactPhoto)
            } else {
                binding.ivContactPhoto.visibility = View.GONE
                binding.tvContactInitial.visibility = View.VISIBLE
                val initial = contact.name.firstOrNull()?.toString()?.uppercase() ?: "?"
                binding.tvContactInitial.text = initial

                // Random background color for initial (or could be deterministic based on name)
                val random = Random(contact.id.hashCode().toLong())
                val color = Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256))
                binding.tvContactInitial.background.setColorFilter(color, PorterDuff.Mode.SRC_IN)
            }

            binding.root.setOnClickListener {
                onContactClick(contact)
            }
        }
    }

    class ContactDiffCallback : DiffUtil.ItemCallback<Contact>() {
        override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean {
            return oldItem == newItem
        }
    }
}