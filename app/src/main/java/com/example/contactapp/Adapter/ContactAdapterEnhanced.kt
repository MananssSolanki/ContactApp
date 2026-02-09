package com.example.contactapp.Adapter

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.contactapp.Model.ContactEnhanced
import com.example.contactapp.Model.ContactListItemEnhanced
import com.example.contactapp.R
import com.example.contactapp.databinding.ItemContactEnhancedBinding
import com.example.contactapp.databinding.ItemContactHeaderBinding

/**
 * Enhanced Contact Adapter with swipe support and advanced features
 */
class ContactAdapterEnhanced(
    private val onContactClick: (ContactEnhanced) -> Unit = {},
    private val onCallClick: (ContactEnhanced) -> Unit = {},
    private val onSmsClick: (ContactEnhanced) -> Unit = {},
    private val onFavoriteClick: (ContactEnhanced) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    private val list = ArrayList<ContactListItemEnhanced>()
    
    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_CONTACT = 1
    }
    
    fun submitList(data: List<ContactListItemEnhanced>) {
        val diffCallback = ContactDiffCallback(list, data)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        list.clear()
        list.addAll(data)
        diffResult.dispatchUpdatesTo(this)
    }
    
    fun getItemAt(position: Int): ContactListItemEnhanced? {
        return if (position in list.indices) list[position] else null
    }
    
    override fun getItemViewType(position: Int): Int {
        return when (list[position]) {
            is ContactListItemEnhanced.Header -> VIEW_TYPE_HEADER
            is ContactListItemEnhanced.ContactItem -> VIEW_TYPE_CONTACT
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
                    ItemContactEnhancedBinding.inflate(
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
            is ContactListItemEnhanced.Header -> (holder as HeaderViewHolder).bind(item)
            is ContactListItemEnhanced.ContactItem -> (holder as ContactViewHolder).bind(item)
        }
    }
    
    override fun getItemCount(): Int = list.size
    
    inner class HeaderViewHolder(private val binding: ItemContactHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ContactListItemEnhanced.Header) {
            binding.tvSectionHeader.text = item.letter
        }
    }
    
    inner class ContactViewHolder(private val binding: ItemContactEnhancedBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: ContactListItemEnhanced.ContactItem) {
            val contact = item.contact
            
            binding.tvContactName.text = contact.name
            binding.tvPhoneNumber.text = contact.phoneNumber
            
            // Show account type badge
            binding.tvAccountBadge.text = when (contact.accountType) {
                com.example.contactapp.Model.AccountType.GOOGLE -> "G"
                com.example.contactapp.Model.AccountType.SIM -> "SIM"
                com.example.contactapp.Model.AccountType.EXCHANGE -> "E"
                else -> ""
            }
            binding.tvAccountBadge.visibility = if (contact.accountType != com.example.contactapp.Model.AccountType.PHONE) {
                View.VISIBLE
            } else {
                View.GONE
            }
            
            // Show favorite icon
            binding.ivFavorite.setImageResource(
                if (contact.isFavorite) android.R.drawable.star_big_on
                else android.R.drawable.star_big_off
            )
            
            // Show initial avatar
            val initial = if (contact.name.isNotEmpty()) {
                contact.name.first().uppercaseChar().toString()
            } else {
                "?"
            }
            binding.tvContactInitial.text = initial
            
            // Load photo if available using Glide (fallback to simple approach if Glide not available)
            if (!contact.photoUri.isNullOrEmpty()) {
                try {
                    // Try using Glide if available
                    Glide.with(binding.root.context)
                        .load(contact.photoUri)
                        .circleCrop()
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .into(binding.ivContactPhoto)
                    binding.ivContactPhoto.visibility = View.VISIBLE
                    binding.tvContactInitial.visibility = View.GONE
                } catch (e: Exception) {
                    // Fallback to URI parsing
                    try {
                        binding.ivContactPhoto.setImageURI(Uri.parse(contact.photoUri))
                        binding.ivContactPhoto.visibility = View.VISIBLE
                        binding.tvContactInitial.visibility = View.GONE
                    } catch (e2: Exception) {
                        binding.ivContactPhoto.visibility = View.GONE
                        binding.tvContactInitial.visibility = View.VISIBLE
                    }
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
                onContactClick(contact)
            }
            
            binding.btnCall.setOnClickListener {
                onCallClick(contact)
            }
            
            binding.ivFavorite.setOnClickListener {
                onFavoriteClick(contact)
            }
            
            // Expose foreground for swipe animation
            binding.foregroundView.setOnClickListener {
                onContactClick(contact)
            }
        }
    }
    
    /**
     * DiffUtil callback for efficient list updates
     */
    class ContactDiffCallback(
        private val oldList: List<ContactListItemEnhanced>,
        private val newList: List<ContactListItemEnhanced>
    ) : DiffUtil.Callback() {
        
        override fun getOldListSize(): Int = oldList.size
        
        override fun getNewListSize(): Int = newList.size
        
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            
            return when {
                oldItem is ContactListItemEnhanced.Header && newItem is ContactListItemEnhanced.Header ->
                    oldItem.letter == newItem.letter
                oldItem is ContactListItemEnhanced.ContactItem && newItem is ContactListItemEnhanced.ContactItem ->
                    oldItem.contact.contactId == newItem.contact.contactId
                else -> false
            }
        }
        
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
