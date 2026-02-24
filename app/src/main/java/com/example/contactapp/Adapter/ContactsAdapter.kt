package com.example.contactapp.Adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.contactapp.Model.ContactEnhanced
import com.example.contactapp.Model.ContactListItemEnhanced
import com.example.contactapp.R
import com.example.contactapp.databinding.ItemContactBinding
import com.example.contactapp.databinding.ItemContactHeaderBinding

class ContactsAdapter : ListAdapter<ContactListItemEnhanced, RecyclerView.ViewHolder>(ContactDiffCallback()) {

    private var onCallClick: ((ContactEnhanced) -> Unit)? = null
    private var onSmsClick: ((ContactEnhanced) -> Unit)? = null
    private var onVideoCallClick: ((ContactEnhanced) -> Unit)? = null
    private var onInformationClick: ((ContactEnhanced) -> Unit)? = null
    private var onEditClick: ((ContactEnhanced) -> Unit)? = null

    private var expandedPosition = -1

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_CONTACT = 1
        private const val TYPE_GROUPS = 2
    }

    fun setOnCallClickListener(listener: (ContactEnhanced) -> Unit) { onCallClick = listener }
    fun setOnSmsClickListener(listener: (ContactEnhanced) -> Unit) { onSmsClick = listener }
    fun setOnVideoCallClickListener(listener: (ContactEnhanced) -> Unit) { onVideoCallClick = listener }
    fun setOnInformationClickListener(listener: (ContactEnhanced) -> Unit) { onInformationClick = listener }
    fun setOnEditClickListener(listener: (ContactEnhanced) -> Unit) { onEditClick = listener }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ContactListItemEnhanced.Header -> TYPE_HEADER
            is ContactListItemEnhanced.ContactItem -> TYPE_CONTACT
            is ContactListItemEnhanced.GroupsItem -> TYPE_GROUPS
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(ItemContactHeaderBinding.inflate(inflater, parent, false))
            TYPE_GROUPS -> GroupsViewHolder(ItemContactBinding.inflate(inflater, parent, false))
            else -> ContactViewHolder(ItemContactBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ContactListItemEnhanced.Header -> (holder as HeaderViewHolder).bind(item)
            is ContactListItemEnhanced.GroupsItem -> (holder as GroupsViewHolder).bind()
            is ContactListItemEnhanced.ContactItem -> (holder as ContactViewHolder).bind(item.contact, position)
        }
    }

    // ─── Header ViewHolder ───────────────────────────────────────────────────────

    inner class HeaderViewHolder(private val binding: ItemContactHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(header: ContactListItemEnhanced.Header) {
            binding.tvSectionHeader.text = header.title
            when {
                header.isFavorite -> {
                    binding.ivHeaderIcon.visibility = View.VISIBLE
                    binding.ivHeaderIcon.setImageResource(android.R.drawable.btn_star_big_on)
                }
                header.title == "Groups" -> {
                    binding.ivHeaderIcon.visibility = View.VISIBLE
                    binding.ivHeaderIcon.setImageResource(R.drawable.ic_groups)
                }
                else -> binding.ivHeaderIcon.visibility = View.GONE
            }
        }
    }

    // ─── Groups ViewHolder ───────────────────────────────────────────────────────

    inner class GroupsViewHolder(private val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.tvContactName.text = "Groups"

            // Show initial placeholder with the groups icon
            binding.tvContactInitial.visibility = View.VISIBLE
            binding.tvContactInitial.text = ""
            binding.tvContactInitial.setBackgroundResource(R.drawable.ic_groups)

            // Clear Glide and hide the photo ImageView
            Glide.with(binding.root.context).clear(binding.ivContactPhoto)
            binding.ivContactPhoto.visibility = View.GONE

            binding.ivFavorite.visibility = View.GONE
            binding.subLy.visibility = View.GONE

            // FIX: Keep the XML's selectableItemBackground ripple — don't override with a solid drawable
            // Just clear any programmatic background override so the XML default applies
            binding.root.background = null
            binding.root.isClickable = true
        }
    }

    // ─── Contact ViewHolder ──────────────────────────────────────────────────────

    inner class ContactViewHolder(private val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: ContactEnhanced, position: Int) {
            binding.tvContactName.text = contact.name

            // FIX: Always clear Glide on recycle first, then decide what to show.
            // Prevents stale photos appearing on recycled ViewHolders.
            Glide.with(binding.root.context).clear(binding.ivContactPhoto)

            if (contact.photoUri != null) {
                binding.tvContactInitial.visibility = View.GONE
                binding.ivContactPhoto.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(Uri.parse(contact.photoUri))
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .circleCrop()
                    .into(binding.ivContactPhoto)
            } else {
                binding.ivContactPhoto.visibility = View.GONE
                binding.tvContactInitial.visibility = View.VISIBLE
                binding.tvContactInitial.text = contact.name.firstOrNull()?.uppercase() ?: "?"
                // FIX: Reset background to the teal_200 color defined in XML
                // (avoids ic_groups icon bleeding in from recycled GroupsViewHolder)
                binding.tvContactInitial.setBackgroundColor(
                    itemView.context.getColor(R.color.teal_200)
                )
            }

            binding.ivFavorite.visibility = if (contact.isFavorite) View.VISIBLE else View.GONE

            // tvPhoneNumber is inside subLy, so it only needs to be set — visibility
            // is handled by the expand/collapse logic below.
            binding.tvPhoneNumber.text = if (contact.additionalPhones.isNotEmpty()) {
                "${contact.phoneNumber}  +${contact.additionalPhones.size} more"
            } else {
                contact.phoneNumber
            }

            // Expand / Collapse the sub-actions panel on click
            val isExpanded = position == expandedPosition
            binding.subLy.visibility = if (isExpanded) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                val pos = adapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                val prev = expandedPosition
                expandedPosition = if (prev == pos) -1 else pos
                if (prev != -1) notifyItemChanged(prev)
                notifyItemChanged(pos)
            }

            // Sub-action buttons — all present in the XML
            binding.phoneImg.setOnClickListener { onCallClick?.invoke(contact) }
            binding.smsImg.setOnClickListener { onSmsClick?.invoke(contact) }
            binding.videoCallImg.setOnClickListener { onVideoCallClick?.invoke(contact) }
            binding.informationImg.setOnClickListener { onInformationClick?.invoke(contact) }

            // FIX: editImg does NOT exist in item_contact.xml.
            // Removed the try/catch hack entirely — onEditClick can be triggered
            // via the informationImg (info screen) or long-press instead.
            binding.root.setOnLongClickListener {
                onEditClick?.invoke(contact)
                true
            }
        }

        // FIX: Removed applySectionBackground() entirely.
        //
        // Reason: The XML root is a RelativeLayout with android:padding="@dimen/_10sdp"
        // and android:background="?attr/selectableItemBackground". Calling
        // setBackgroundResource() from the adapter overwrites the ripple effect,
        // and calling setPadding() on top of the XML padding causes double-padding.
        //
        // If you want Samsung-style grouped rounded corners, the correct approach is to
        // create a dedicated item_contact_top.xml / _middle.xml / _bottom.xml / _single.xml
        // layout files and inflate the correct one in onCreateViewHolder based on position.
        // For now, the single XML layout with the default background is clean and correct.
    }

    // ─── DiffCallback ────────────────────────────────────────────────────────────

    class ContactDiffCallback : DiffUtil.ItemCallback<ContactListItemEnhanced>() {
        override fun areItemsTheSame(
            oldItem: ContactListItemEnhanced,
            newItem: ContactListItemEnhanced
        ): Boolean = when {
            oldItem is ContactListItemEnhanced.ContactItem && newItem is ContactListItemEnhanced.ContactItem ->
                oldItem.contact.contactId == newItem.contact.contactId
            oldItem is ContactListItemEnhanced.Header && newItem is ContactListItemEnhanced.Header ->
                oldItem.title == newItem.title
            else -> oldItem == newItem
        }

        override fun areContentsTheSame(
            oldItem: ContactListItemEnhanced,
            newItem: ContactListItemEnhanced
        ): Boolean = oldItem == newItem
    }
}