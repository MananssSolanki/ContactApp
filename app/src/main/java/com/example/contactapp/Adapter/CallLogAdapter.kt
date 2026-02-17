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
    private val onCallClick: (String) -> Unit,
    private val onItemClick: (CallLog) -> Unit = {},
    private val style: Int = 0 // 0 = New/History, 1 = Old/Recent
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
                // Determine background based on position (Only relevant for Style 0)
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

            // --- COMMON SETUP (Icons & Colors) ---
            val (statusText, iconRes, color) = when (callLog.callType) {
                CallLog.CallType.INCOMING -> Triple("Incoming call", android.R.drawable.sym_call_incoming, Color.parseColor("#4CAF50"))
                CallLog.CallType.OUTGOING -> Triple("Outgoing call", android.R.drawable.sym_call_outgoing, Color.parseColor("#ff9800"))
                CallLog.CallType.MISSED -> Triple("Missed call", android.R.drawable.sym_call_missed, Color.parseColor("#F44336"))
            }
            binding.ivCallType.setImageResource(iconRes)
            
            // Icon Tint Logic
            val tintColor = if (callLog.callType == CallLog.CallType.MISSED) Color.RED else Color.parseColor("#2196F3")
            binding.ivCallType.setColorFilter(tintColor)


            if (style == 0) {
                // --- STYLE 0 (New / History / Grouped) ---
                
                // Visible Views
                binding.tvTime.visibility = View.VISIBLE
                binding.tvStatusDuration.visibility = View.VISIBLE
                binding.divider.visibility = if (isLast) View.GONE else View.VISIBLE // Divider logic for groups

                // Hidden Views
                binding.tvContactName.visibility = View.GONE
                binding.tvPhoneNumber.visibility = View.GONE
                binding.tvTimeAndDuration.visibility = View.GONE
                binding.btnCall.visibility = View.GONE
                
                // Data Population
                binding.tvTime.text = callLog.getFormattedTime().lowercase()
                val durationText = formatDuration(callLog.duration)
                binding.tvStatusDuration.text = "$statusText, $durationText"

                // Background Logic (Grouped Rounded Corners)
                val bgRes = when {
                    isFirst && isLast -> R.drawable.bg_group_item_single
                    isFirst -> R.drawable.bg_group_item_top
                    isLast -> R.drawable.bg_group_item_bottom
                    else -> R.drawable.bg_group_item_middle
                }
                binding.rootLayout.setBackgroundResource(bgRes)

                // Margin Logic (Space between groups)
                val params = binding.root.layoutParams as RecyclerView.LayoutParams
                if (isLast) {
                    params.bottomMargin = binding.root.context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp)
                } else {
                    params.bottomMargin = 0
                }
                binding.root.layoutParams = params

            } else {
                // --- STYLE 1 (Old / Recent / Flat) ---

                // Visible Views
                binding.tvContactName.visibility = View.VISIBLE
                binding.tvTimeAndDuration.visibility = View.VISIBLE
                binding.btnCall.visibility = View.VISIBLE
                // Divider always gone or handled differently? Original design didn't seem to have explicit divider view in item, maybe RecyclerView item decoration?
                // For now, hide internal divider to match 'flat' look, or show it if desired. Let's hide it for cleaner look or match previous.
                binding.divider.visibility = View.GONE 

                // Hidden Views
                binding.tvTime.visibility = View.GONE
                binding.tvStatusDuration.visibility = View.GONE

                // Data Population
                if (callLog.contactName != null) {
                    binding.tvContactName.text = callLog.contactName
                    binding.tvPhoneNumber.text = callLog.phoneNumber
                    binding.tvPhoneNumber.visibility = View.VISIBLE
                } else {
                    binding.tvContactName.text = callLog.phoneNumber
                    binding.tvPhoneNumber.visibility = View.GONE
                }

                // Color for Missed Calls on Name
                if (callLog.callType == CallLog.CallType.MISSED) {
                    binding.tvContactName.setTextColor(Color.RED)
                } else {
                    binding.tvContactName.setTextColor(Color.BLACK)
                }

                // Time & Duration string
                val durationStr = if (callLog.duration > 0) " • ${callLog.getFormattedDuration()}" else ""
                binding.tvTimeAndDuration.text = "${callLog.getFormattedTime()}$durationStr"

                // Background Logic (Selectable Flat)
                val outValue = android.util.TypedValue()
                binding.root.context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                binding.rootLayout.setBackgroundResource(outValue.resourceId)

                // Reset Margins
                val params = binding.root.layoutParams as RecyclerView.LayoutParams
                params.bottomMargin = 0
                binding.root.layoutParams = params
            }

            // Click listeners
            binding.root.setOnClickListener { onItemClick(callLog) }
            binding.btnCall.setOnClickListener { onCallClick(callLog.phoneNumber) }
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
