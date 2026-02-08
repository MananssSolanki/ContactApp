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
import com.example.contactapp.databinding.ItemCallLogBinding
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
                    ItemCallLogBinding.inflate(
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
            is CallLogListItem.CallItem -> (holder as CallLogViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = list.size

    inner class HeaderViewHolder(private val binding: ItemCallLogHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CallLogListItem.Header) {
            binding.tvDateHeader.text = item.dateSection
        }
    }

    inner class CallLogViewHolder(private val binding: ItemCallLogBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: CallLogListItem.CallItem) {
            val callLog = item.callLog
            
            // Set contact name or phone number
            if (callLog.contactName != null) {
                binding.tvContactName.text = callLog.contactName
                binding.tvPhoneNumber.text = callLog.phoneNumber
                binding.tvPhoneNumber.visibility = View.VISIBLE
            } else {
                binding.tvContactName.text = callLog.phoneNumber
                binding.tvPhoneNumber.visibility = View.GONE
            }

            // Set time and duration
            val durationText = if (callLog.duration > 0) {
                " • ${callLog.getFormattedDuration()}"
            } else {
                ""
            }
            binding.tvTimeAndDuration.text = "${callLog.getFormattedTime()}$durationText"

            // Set call type icon and color
            when (callLog.callType) {
                CallLog.CallType.INCOMING -> {
                    binding.ivCallType.setImageResource(android.R.drawable.sym_call_incoming)
                    binding.ivCallType.setColorFilter(Color.parseColor("#4CAF50")) // Green
                }
                CallLog.CallType.OUTGOING -> {
                    binding.ivCallType.setImageResource(android.R.drawable.sym_call_outgoing)
                    binding.ivCallType.setColorFilter(Color.parseColor("#2196F3")) // Blue
                }
                CallLog.CallType.MISSED -> {
                    binding.ivCallType.setImageResource(android.R.drawable.sym_call_missed)
                    binding.ivCallType.setColorFilter(Color.parseColor("#F44336")) // Red
                    // Make text red for missed calls
                    binding.tvContactName.setTextColor(Color.parseColor("#F44336"))
                }
            }

            // Reset text color for non-missed calls
            if (callLog.callType != CallLog.CallType.MISSED) {
                binding.tvContactName.setTextColor(Color.parseColor("#000000"))
            }

            // Click listeners
            binding.root.setOnClickListener {
                onItemClick(callLog)
            }

            binding.btnCall.setOnClickListener {
                onCallClick(callLog.phoneNumber)
            }
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
