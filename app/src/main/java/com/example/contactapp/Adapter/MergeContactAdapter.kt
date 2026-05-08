package com.example.contactapp.Adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.contactapp.Model.MergeDuplicateGroup
import com.example.contactapp.R

class MergeContactsAdapter(
    private val groups: List<MergeDuplicateGroup>,
    private val selectedGroups: MutableSet<Int>,
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<MergeContactsAdapter.GroupViewHolder>() {

    inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkboxGroup: CheckBox = itemView.findViewById(R.id.checkboxGroup)
        val textGroupKey: TextView = itemView.findViewById(R.id.textGroupKey)
        val layoutContacts: LinearLayout = itemView.findViewById(R.id.layoutContacts)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_merge_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]

        // Set checkbox without triggering listener
        holder.checkboxGroup.setOnCheckedChangeListener(null)
        holder.checkboxGroup.isChecked = selectedGroups.contains(position)
        holder.textGroupKey.text = group.displayKey

        // Set up checkbox toggle
        holder.checkboxGroup.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectedGroups.add(position)
            else selectedGroups.remove(position)
            onSelectionChanged()
        }

        // Also allow tapping the whole header row to toggle
        holder.itemView.setOnClickListener {
            holder.checkboxGroup.isChecked = !holder.checkboxGroup.isChecked
        }

        // Build contact sub-rows inside the card
        holder.layoutContacts.removeAllViews()
        group.contacts.forEachIndexed { index, contact ->
            val row = LayoutInflater.from(holder.itemView.context)
                .inflate(R.layout.item_merge_contact_row, holder.layoutContacts, false)

            val tvAvatarLetter = row.findViewById<TextView>(R.id.tvAvatarLetter)
            val imgAvatar = row.findViewById<ImageView>(R.id.imgContactAvatar)
            val tvName = row.findViewById<TextView>(R.id.tvContactName)
            val tvPhone = row.findViewById<TextView>(R.id.tvContactPhone)
            val layoutDefault = row.findViewById<LinearLayout>(R.id.layoutDefault)

            tvName.text = contact.name
            tvPhone.text = contact.phoneNumber.ifEmpty { contact.email ?: "" }

            // Show "Default" badge only on first contact row
            layoutDefault.visibility = if (index == 0) View.VISIBLE else View.GONE

            // Load photo or fallback to letter avatar
            val photoUri = contact.photoUri
            if (!photoUri.isNullOrEmpty()) {
                try {
                    imgAvatar.setImageURI(Uri.parse(photoUri))
                    imgAvatar.visibility = View.VISIBLE
                    tvAvatarLetter.visibility = View.GONE
                } catch (e: Exception) {
                    showLetterAvatar(imgAvatar, tvAvatarLetter, contact.name)
                }
            } else {
                showLetterAvatar(imgAvatar, tvAvatarLetter, contact.name)
            }

            holder.layoutContacts.addView(row)
        }
    }

    private fun showLetterAvatar(imgView: ImageView, letterView: TextView, name: String) {
        imgView.visibility = View.GONE
        letterView.visibility = View.VISIBLE
        letterView.text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    }

    override fun getItemCount(): Int = groups.size
}