package com.example.contactapp.Adapter

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.contactapp.Model.ContactListItem
import com.example.contactapp.databinding.ItemContactBinding
import com.example.contactapp.databinding.ItemContactHeaderBinding

class PhoneContactAdapter(
    private val onContactClick: (ContactListItem.ContactItem) -> Unit = {},
    private val onCallClick: (String) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val list = ArrayList<ContactListItem>()

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_CONTACT = 1
    }

    fun submitList(data: List<ContactListItem>) {
        val diffCallback = ContactDiffCallback(list, data)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        list.clear()
        list.addAll(data)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemViewType(position: Int): Int {
        return when (list[position]) {
            is ContactListItem.Header -> VIEW_TYPE_HEADER
            is ContactListItem.ContactItem -> VIEW_TYPE_CONTACT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                HeaderViewHolder(
                    ItemContactHeaderBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            else -> {
                ContactViewHolder(
                    ItemContactBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = list[position]) {
            is ContactListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is ContactListItem.ContactItem -> (holder as ContactViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = list.size

    inner class HeaderViewHolder(private val binding: ItemContactHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ContactListItem.Header) {
            binding.tvSectionHeader.text = item.letter
        }
    }

    inner class ContactViewHolder(private val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: ContactListItem.ContactItem) {
            val contact = item.contact
            
            binding.tvContactName.text = contact.name
            binding.tvPhoneNumber.text = contact.phoneNumber

            // Show initial avatar
            val initial = if (contact.name.isNotEmpty()) {
                contact.name.first().uppercaseChar().toString()
            } else {
                "?"
            }
            binding.tvContactInitial.text = initial
            
            // Load photo if available (using simple approach, can be enhanced with Glide/Coil)
            if (!contact.photoUri.isNullOrEmpty()) {
                try {
                    binding.ivContactPhoto.setImageURI(Uri.parse(contact.photoUri))
                    binding.ivContactPhoto.visibility = View.VISIBLE
                    binding.tvContactInitial.visibility = View.GONE
                } catch (e: Exception) {
                    // If photo loading fails, show initial
                    binding.ivContactPhoto.visibility = View.GONE
                    binding.tvContactInitial.visibility = View.VISIBLE
                }
            } else {
                binding.ivContactPhoto.visibility = View.GONE
                binding.tvContactInitial.visibility = View.VISIBLE
            }

            // Set background color based on initial
            val colors = listOf(
                0xFF6200EA.toInt(), // Purple
                0xFF03DAC5.toInt(), // Teal
                0xFFFF6F00.toInt(), // Orange
                0xFF2196F3.toInt(), // Blue
                0xFF4CAF50.toInt(), // Green
                0xFFE91E63.toInt(), // Pink
                0xFF9C27B0.toInt(), // Deep Purple
                0xFF00BCD4.toInt()  // Cyan
            )
            val colorIndex = initial.hashCode() % colors.size
            binding.tvContactInitial.setBackgroundColor(colors[colorIndex.coerceAtLeast(0)])

            // Click listeners
            binding.root.setOnClickListener {
                onContactClick(item)
            }

            binding.btnCall.setOnClickListener {
                onCallClick(contact.phoneNumber)
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates
     */
    class ContactDiffCallback(
        private val oldList: List<ContactListItem>,
        private val newList: List<ContactListItem>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            
            return when {
                oldItem is ContactListItem.Header && newItem is ContactListItem.Header ->
                    oldItem.letter == newItem.letter
                oldItem is ContactListItem.ContactItem && newItem is ContactListItem.ContactItem ->
                    oldItem.contact.contactId == newItem.contact.contactId
                else -> false
            }
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
