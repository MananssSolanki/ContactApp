package com.example.contactapp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.contactapp.Model.DeletedContact
import com.example.contactapp.R
import com.bumptech.glide.Glide
import java.util.concurrent.TimeUnit

class RecycleBinAdapter(
    private val onRestoreClick: (DeletedContact) -> Unit,
    private val onDeleteClick: (DeletedContact) -> Unit
) : ListAdapter<DeletedContact, RecycleBinAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAvatar: ImageView = itemView.findViewById(R.id.imgAvatar)
        val tvLetter: TextView = itemView.findViewById(R.id.tvAvatarLetter)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvPhone: TextView = itemView.findViewById(R.id.tvPhone)
        val tvDaysLeft: TextView = itemView.findViewById(R.id.tvDaysLeft)
        val btnRestore: TextView = itemView.findViewById(R.id.btnRestore)
        val btnDelete: TextView = itemView.findViewById(R.id.btnPermanentDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recycle_bin, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = getItem(position)

        holder.tvName.text = contact.name
        holder.tvPhone.text = contact.phoneNumber.ifEmpty { "No phone number" }

        // Days remaining (30-day retention)
        val diffMs = System.currentTimeMillis() - contact.deletedAt
        val daysPassed = TimeUnit.MILLISECONDS.toDays(diffMs)
        val daysLeft = (30 - daysPassed).coerceAtLeast(0)
        
        holder.tvDaysLeft.text = when {
            daysLeft > 1 -> "$daysLeft days left until permanently deleted"
            daysLeft == 1L -> "1 day left until permanently deleted"
            else -> "Permanently deleted soon"
        }

        // Avatar
        if (!contact.photoUri.isNullOrEmpty()) {
            holder.imgAvatar.visibility = View.VISIBLE
            holder.tvLetter.visibility = View.GONE
            Glide.with(holder.itemView.context)
                .load(contact.photoUri)
                .circleCrop()
                .into(holder.imgAvatar)
        } else {
            showLetterAvatar(holder, contact.name)
        }

        holder.btnRestore.setOnClickListener { onRestoreClick(contact) }
        holder.btnDelete.setOnClickListener { onDeleteClick(contact) }
    }

    private fun showLetterAvatar(holder: ViewHolder, name: String) {
        holder.imgAvatar.visibility = View.GONE
        holder.tvLetter.visibility = View.VISIBLE
        holder.tvLetter.text = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    }

    class DiffCallback : DiffUtil.ItemCallback<DeletedContact>() {
        override fun areItemsTheSame(a: DeletedContact, b: DeletedContact) = a.id == b.id
        override fun areContentsTheSame(a: DeletedContact, b: DeletedContact) = a == b
    }
}
