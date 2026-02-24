package com.example.contactapp.Adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.contactapp.Model.ContactEnhanced
import com.example.contactapp.R
import com.example.contactapp.ViewModel.SearchResultItem
import com.example.contactapp.databinding.ItemCallLogBinding
import com.example.contactapp.databinding.ItemContactBinding
import com.example.contactapp.databinding.ItemContactHeaderBinding
import com.example.contactapp.databinding.ItemSearchHistoryBinding

class SearchAdapter : ListAdapter<SearchResultItem, RecyclerView.ViewHolder>(SearchDiffCallback()) {

    private var onHistoryClick: ((String) -> Unit)? = null
    private var onHistoryRemoveClick: ((String) -> Unit)? = null
    private var onContactClick: ((ContactEnhanced) -> Unit)? = null
    private var onCallLogClick: ((com.example.contactapp.Model.CallLog) -> Unit)? = null
    
    // New sub-action listeners
    private var onCallClick: ((ContactEnhanced) -> Unit)? = null
    private var onSmsClick: ((ContactEnhanced) -> Unit)? = null
    private var onVideoCallClick: ((ContactEnhanced) -> Unit)? = null
    private var onInformationClick: ((ContactEnhanced) -> Unit)? = null

    private var expandedPosition = -1

    fun setOnHistoryClickListener(l: (String) -> Unit) { onHistoryClick = l }
    fun setOnHistoryRemoveClickListener(l: (String) -> Unit) { onHistoryRemoveClick = l }
    fun setOnContactClickListener(l: (ContactEnhanced) -> Unit) { onContactClick = l }
    fun setOnCallLogClickListener(l: (com.example.contactapp.Model.CallLog) -> Unit) { onCallLogClick = l }
    
    fun setOnCallClickListener(l: (ContactEnhanced) -> Unit) { onCallClick = l }
    fun setOnSmsClickListener(l: (ContactEnhanced) -> Unit) { onSmsClick = l }
    fun setOnVideoCallClickListener(l: (ContactEnhanced) -> Unit) { onVideoCallClick = l }
    fun setOnInformationClickListener(l: (ContactEnhanced) -> Unit) { onInformationClick = l }

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_HISTORY = 1
        const val TYPE_CONTACT = 2
        const val TYPE_CALL = 3
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is SearchResultItem.Header -> TYPE_HEADER
        is SearchResultItem.HistoryItem -> TYPE_HISTORY
        is SearchResultItem.ContactItem -> TYPE_CONTACT
        is SearchResultItem.CallItem -> TYPE_CALL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderVH(ItemContactHeaderBinding.inflate(inflater, parent, false))
            TYPE_HISTORY -> HistoryVH(ItemSearchHistoryBinding.inflate(inflater, parent, false))
            TYPE_CONTACT -> ContactVH(ItemContactBinding.inflate(inflater, parent, false))
            TYPE_CALL -> CallVH(ItemCallLogBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is SearchResultItem.Header -> (holder as HeaderVH).bind(item)
            is SearchResultItem.HistoryItem -> (holder as HistoryVH).bind(item)
            is SearchResultItem.ContactItem -> (holder as ContactVH).bind(item.contact, position)
            is SearchResultItem.CallItem -> (holder as CallVH).bind(item.callLog)
        }
    }

    inner class HeaderVH(val b: ItemContactHeaderBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: SearchResultItem.Header) {
            b.tvSectionHeader.text = item.title
            b.ivHeaderIcon.visibility = View.GONE
        }
    }

    inner class HistoryVH(val b: ItemSearchHistoryBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(historyItem: SearchResultItem.HistoryItem) {
            b.tvSearchQuery.text = historyItem.query
            b.root.setOnClickListener { onHistoryClick?.invoke(historyItem.query) }
            b.btnRemove.setOnClickListener { onHistoryRemoveClick?.invoke(historyItem.query) }
        }
    }

    inner class ContactVH(val b: ItemContactBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(c: ContactEnhanced, position: Int) {
            b.tvContactName.text = c.name
            b.tvPhoneNumber.text = c.phoneNumber
            
            // Expansion logic
            val isExpanded = position == expandedPosition
            b.subLy.visibility = if (isExpanded) View.VISIBLE else View.GONE
            
            Glide.with(b.root.context).clear(b.ivContactPhoto)
            if (c.photoUri != null) {
                b.tvContactInitial.visibility = View.GONE
                b.ivContactPhoto.visibility = View.VISIBLE
                Glide.with(itemView.context).load(Uri.parse(c.photoUri)).circleCrop().into(b.ivContactPhoto)
            } else {
                b.ivContactPhoto.visibility = View.GONE
                b.tvContactInitial.visibility = View.VISIBLE
                b.tvContactInitial.text = c.name.firstOrNull()?.uppercase() ?: "?"
                b.tvContactInitial.setBackgroundColor(itemView.context.getColor(R.color.teal_200))
            }

            b.root.setOnClickListener {
                val pos = adapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                val prev = expandedPosition
                expandedPosition = if (prev == pos) -1 else pos
                if (prev != -1) notifyItemChanged(prev)
                notifyItemChanged(pos)
                onContactClick?.invoke(c)
            }

            // Bind action listeners
            b.phoneImg.setOnClickListener { onCallClick?.invoke(c) }
            b.smsImg.setOnClickListener { onSmsClick?.invoke(c) }
            b.videoCallImg.setOnClickListener { onVideoCallClick?.invoke(c) }
            b.informationImg.setOnClickListener { onInformationClick?.invoke(c) }
        }
    }

    inner class CallVH(val b: ItemCallLogBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(cl: com.example.contactapp.Model.CallLog) {
            b.tvContactName.text = cl.contactName ?: cl.phoneNumber
            b.tvPhoneNumber.text = if (cl.contactName != null) cl.phoneNumber else null
            b.tvPhoneNumber.visibility = if (cl.contactName != null) View.VISIBLE else View.GONE
            b.tvTimeAndDuration.text = "${cl.getFormattedTime()} \u2022 ${cl.getFormattedDuration()}"
            
            val iconAttr = when (cl.callType) {
                com.example.contactapp.Model.CallLog.CallType.INCOMING -> android.R.drawable.sym_call_incoming
                com.example.contactapp.Model.CallLog.CallType.OUTGOING -> android.R.drawable.sym_call_outgoing
                com.example.contactapp.Model.CallLog.CallType.MISSED -> android.R.drawable.sym_call_missed
            }
            b.ivCallType.setImageResource(iconAttr)
            b.root.setOnClickListener { onCallLogClick?.invoke(cl) }
            b.btnCall.setOnClickListener { /* could invoke call click */ }
        }
    }

    class SearchDiffCallback : DiffUtil.ItemCallback<SearchResultItem>() {
        override fun areItemsTheSame(old: SearchResultItem, new: SearchResultItem): Boolean = old == new
        override fun areContentsTheSame(old: SearchResultItem, new: SearchResultItem): Boolean = old == new
    }
}
