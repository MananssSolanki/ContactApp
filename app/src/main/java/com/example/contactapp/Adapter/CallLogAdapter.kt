package com.example.contactapp.Adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.contactapp.Model.CallLog
import com.example.contactapp.Model.CallLogListItem
import com.example.contactapp.R
import com.example.contactapp.databinding.ItemCallHistoryRowBinding
import com.example.contactapp.databinding.ItemCallLogHeaderBinding

class CallLogAdapter(
    private val onCallClick: (String) -> Unit = {},
    private val onItemClick: (CallLog) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val list = ArrayList<CallLogListItem>()

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_CALL = 1
    }

    fun submitList(data: List<CallLogListItem>) {
        val diffCallback = CallLogDiffCallback(list, data)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        list.clear()
        list.addAll(data)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemViewType(position: Int): Int {
        return when (list[position]) {
            is CallLogListItem.Header -> VIEW_TYPE_HEADER
            is CallLogListItem.CallItem -> VIEW_TYPE_CALL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                HeaderViewHolder(
                    ItemCallLogHeaderBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            else -> {
                CallLogViewHolder(
                    ItemCallHistoryRowBinding.inflate(
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
            is CallLogListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is CallLogListItem.CallItem -> {
                // Determine background based on position
                val isFirstInGroup = position == 0 || list[position - 1] is CallLogListItem.Header
                val isLastInGroup = position == list.size - 1 || list[position + 1] is CallLogListItem.Header
                
                (holder as CallLogViewHolder).bind(item, isFirstInGroup, isLastInGroup)
            }
        }
    }

    override fun getItemCount(): Int = list.size

    inner class HeaderViewHolder(private val binding: ItemCallLogHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CallLogListItem.Header) {
            binding.tvDateHeader.text = item.dateSection
        }
    }

    inner class CallLogViewHolder(private val binding: ItemCallHistoryRowBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: CallLogListItem.CallItem, isFirst: Boolean, isLast: Boolean) {
            val callLog = item.callLog

            // Set layout params for margin
            val params = binding.root.layoutParams as RecyclerView.LayoutParams
            if (isLast) {
                params.bottomMargin = binding.root.context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp)
            } else {
                params.bottomMargin = 0
            }
            binding.root.layoutParams = params
            
            // Set Time
            binding.tvTime.text = callLog.getFormattedTime().lowercase()

            // Set Call Type, Icon, and Duration
            val (statusText, iconRes, color) = when (callLog.callType) {
                CallLog.CallType.INCOMING -> Triple("Incoming call", android.R.drawable.sym_call_incoming, Color.parseColor("#4CAF50"))
                CallLog.CallType.OUTGOING -> Triple("Outgoing call", android.R.drawable.sym_call_outgoing, Color.parseColor("#ff9800")) // Orange for outgoing match samsung usually? or Blue. User screenshot shows blue/green. Stick to standard or user request.
                CallLog.CallType.MISSED -> Triple("Missed call", android.R.drawable.sym_call_missed, Color.parseColor("#F44336"))
            }

            // Update call type text for Outgoing to match user screenshot style if needed, or keep standard.
            // Screenshot: "Outgoing call, 0 mins 8 secs"
            // Duration formatting: "0 mins 8 secs"
            val durationText = formatDuration(callLog.duration)
            binding.tvStatusDuration.text = "$statusText, $durationText"

            binding.ivCallType.setImageResource(iconRes)
            
            // User screenshot has colored icons.
            if (callLog.callType == CallLog.CallType.INCOMING) {
                binding.ivCallType.setColorFilter(Color.parseColor("#2196F3")) // Blue for incoming in screenshot? No, typically green or blue. Screenshot 8:53pm is outgoing (blue icon). 9:53am Incoming (blue icon). Wait, screenshot shows blue for both? Just arrow direction changes. 
                // Let's stick to standard colors but apply tint.
                 binding.ivCallType.setColorFilter(Color.parseColor("#2196F3")) 
            } else if (callLog.callType == CallLog.CallType.OUTGOING) {
                 binding.ivCallType.setColorFilter(Color.parseColor("#4CAF50")) // Greenish? Or just Blue as well?
                 // Let's make them all blueish except missed.
                 binding.ivCallType.setColorFilter(Color.parseColor("#2196F3"))
            } else {
                binding.ivCallType.setColorFilter(Color.RED)
            }

            // Background Logic
            val bgRes = when {
                isFirst && isLast -> R.drawable.bg_group_item_single
                isFirst -> R.drawable.bg_group_item_top
                isLast -> R.drawable.bg_group_item_bottom
                else -> R.drawable.bg_group_item_middle
            }
            binding.root.setBackgroundResource(bgRes)

            // Divider Logic
            binding.divider.visibility = if (isLast) View.GONE else View.VISIBLE

            // Click listeners
            binding.root.setOnClickListener {
                onItemClick(callLog)
            }
            // No btnCall in new layout, whole row clickable? Or maybe add if needed.
            // Layout doesn't have btnCall anymore.
            // onCallClick(callLog.phoneNumber) can be triggered by whole row or long press? 
            // Standard behavior: click to call or expand. User just said "click on viewmore". 
            // For now, click -> toggle expanded or do nothing? 
            // Let's make click -> call for now or detail. Using passed callback.
            binding.root.setOnClickListener { onCallClick(callLog.phoneNumber) }
        }

        private fun formatDuration(seconds: Long): String {
            val mins = seconds / 60
            val secs = seconds % 60
            return "$mins mins $secs secs"
        }
    }

    /**
     * DiffUtil callback for efficient list updates
     */
    class CallLogDiffCallback(
        private val oldList: List<CallLogListItem>,
        private val newList: List<CallLogListItem>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            
            return when {
                oldItem is CallLogListItem.Header && newItem is CallLogListItem.Header ->
                    oldItem.dateSection == newItem.dateSection
                oldItem is CallLogListItem.CallItem && newItem is CallLogListItem.CallItem ->
                    oldItem.callLog.id == newItem.callLog.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
