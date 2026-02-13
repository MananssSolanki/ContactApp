package com.example.contactapp.Adapter

import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.contactapp.Model.ContactEnhanced
import com.example.contactapp.R
import com.example.contactapp.databinding.ItemContactBinding
import java.util.Random

class ContactsAdapter(
) : ListAdapter<ContactEnhanced, ContactsAdapter.ContactViewHolder>(ContactDiffCallback()) {

    private var expandedPosition = -1
    private var onCallItemClickListener : ((ContactEnhanced) -> Unit) ?= null
    private var onSmsItemClickListener : ((ContactEnhanced) -> Unit) ?= null
    private var onVideoCallItemClickListener : ((ContactEnhanced) -> Unit) ?= null
    private var onInformationItemClickListener : ((ContactEnhanced) -> Unit) ?= null

    fun setOnCallClickListener(onCallItemClickListener : (ContactEnhanced) -> Unit){
        this.onCallItemClickListener = onCallItemClickListener
    }

    fun setOnInformationClickListener(onInformationItemClickListener : (ContactEnhanced) -> Unit){
        this.onInformationItemClickListener = onInformationItemClickListener
    }

    fun setOnSmsClickListener(onSmsItemClickListener : (ContactEnhanced) -> Unit){
        this.onSmsItemClickListener = onSmsItemClickListener
    }

    fun setOnVideoCallClickListener(onVideoCallItemClickListener : (ContactEnhanced) -> Unit){
        this.onVideoCallItemClickListener = onVideoCallItemClickListener
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ContactViewHolder(private val binding: ItemContactBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(contact: ContactEnhanced) {
            binding.tvContactName.text = contact.name
            binding.tvPhoneNumber.text = contact.phoneNumber

            val isExpanded = position == expandedPosition
            binding.subLy.visibility = if (isExpanded) View.VISIBLE else View.GONE

            if (contact.photoUri != null) {
                binding.ivContactPhoto.visibility = View.VISIBLE
                binding.tvContactInitial.visibility = View.GONE
                Glide.with(binding.root.context)
                    .load(Uri.parse(contact.photoUri))
                    .circleCrop()
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(binding.ivContactPhoto)
            } else {
                binding.ivContactPhoto.visibility = View.GONE
                binding.tvContactInitial.visibility = View.VISIBLE
                val initial = contact.name.firstOrNull()?.toString()?.uppercase() ?: "?"
                binding.tvContactInitial.text = initial

                // Random background color for initial (or could be deterministic based on name)
                val random = Random(contact.contactId.hashCode().toLong())
                val color = Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256))
                binding.tvContactInitial.background.setColorFilter(color, PorterDuff.Mode.SRC_IN)
            }

            binding.phoneImg.setOnClickListener {
                onCallItemClickListener?.invoke(contact)
            }

            binding.smsImg.setOnClickListener {
                onSmsItemClickListener?.invoke(contact)
            }

            binding.videoCallImg.setOnClickListener {
                onVideoCallItemClickListener?.invoke(contact)
            }

            binding.informationImg.setOnClickListener {
                onInformationItemClickListener?.invoke(contact)
            }

            binding.root.setOnClickListener {
                val previousPosition = expandedPosition
                expandedPosition = if (isExpanded) -1 else position

                // refresh old and new item
                if (previousPosition != -1) notifyItemChanged(previousPosition)
                notifyItemChanged(position)
                // binding.subLy.visibility = View.VISIBLE // Logic handled by notifyItemChanged re-binding
            }
        }
    }

    class ContactDiffCallback : DiffUtil.ItemCallback<ContactEnhanced>() {
        override fun areItemsTheSame(oldItem: ContactEnhanced, newItem: ContactEnhanced): Boolean {
            return oldItem.contactId == newItem.contactId
        }

        override fun areContentsTheSame(oldItem: ContactEnhanced, newItem: ContactEnhanced): Boolean {
            return oldItem == newItem
        }
    }
}