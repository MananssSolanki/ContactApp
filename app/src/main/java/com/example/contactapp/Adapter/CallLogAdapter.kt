//package com.example.contactapp.Adapter
//
//import android.graphics.Color
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.recyclerview.widget.DiffUtil
//import androidx.recyclerview.widget.RecyclerView
//import com.example.contactapp.Model.CallLog
//import com.example.contactapp.Model.CallLogListItem
//import com.example.contactapp.R
//import com.example.contactapp.databinding.ItemCallHistoryRowBinding
//import com.example.contactapp.databinding.ItemCallLogHeaderBinding
//
//class CallLogAdapter(
//    private val onCallClick: (String) -> Unit,
//    private val onItemClick: (CallLog) -> Unit = {},
//    private val onSelectionChanged: (Int) -> Unit = {},
//    private val style: Int = 0 // 0 = New/History, 1 = Old/Recent
//) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
//
//    private val list = ArrayList<CallLogListItem>()
//
//    // Selection state
//    var isSelectionMode = false
//        set(value) {
//            field = value
//            if (!value) selectedIds.clear()
//            notifyDataSetChanged()
//        }
//    val selectedIds = HashSet<String>()
//
//    companion object {
//        private const val VIEW_TYPE_HEADER = 0
//        private const val VIEW_TYPE_CALL = 1
//    }
//
//    fun submitList(data: List<CallLogListItem>) {
//        val diffCallback = CallLogDiffCallback(list, data)
//        val diffResult = DiffUtil.calculateDiff(diffCallback)
//
//        list.clear()
//        list.addAll(data)
//        diffResult.dispatchUpdatesTo(this)
//    }
//
//    fun selectAll() {
//        isSelectionMode = true
//        list.forEach { if (it is CallLogListItem.CallItem) selectedIds.add(it.callLog.id) }
//        notifyDataSetChanged()
//        onSelectionChanged(selectedIds.size)
//    }
//
//    fun clearSelection() {
//        selectedIds.clear()
//        isSelectionMode = false
//        onSelectionChanged(0)
//    }
//
//    /**
//     * Remove items by ids from the current list (reactive update), then exit selection mode.
//     * Use after deleting from database to refresh without reloading.
//     */
//    fun removeItemsByIdAndExitSelection(ids: Set<String>) {
//        val newList = list.filter { item ->
//            when (item) {
//                is CallLogListItem.Header -> true
//                is CallLogListItem.CallItem -> item.callLog.id !in ids
//            }
//        }
//        submitList(newList)
//        selectedIds.clear()
//        isSelectionMode = false
//        onSelectionChanged(0)
//    }
//
//    override fun getItemViewType(position: Int): Int {
//        return when (list[position]) {
//            is CallLogListItem.Header -> VIEW_TYPE_HEADER
//            is CallLogListItem.CallItem -> VIEW_TYPE_CALL
//        }
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
//        return when (viewType) {
//            VIEW_TYPE_HEADER -> {
//                HeaderViewHolder(
//                    ItemCallLogHeaderBinding.inflate(
//                        LayoutInflater.from(parent.context),
//                        parent,
//                        false
//                    )
//                )
//            }
//            else -> {
//                CallLogViewHolder(
//                    ItemCallHistoryRowBinding.inflate(
//                        LayoutInflater.from(parent.context),
//                        parent,
//                        false
//                    )
//                )
//            }
//        }
//    }
//
//    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
//        when (val item = list[position]) {
//            is CallLogListItem.Header -> (holder as HeaderViewHolder).bind(item)
//            is CallLogListItem.CallItem -> {
//                val isFirstInGroup = position == 0 || list[position - 1] is CallLogListItem.Header
//                val isLastInGroup = position == list.size - 1 || list[position + 1] is CallLogListItem.Header
//                (holder as CallLogViewHolder).bind(item, isFirstInGroup, isLastInGroup)
//            }
//        }
//    }
//
//    override fun getItemCount(): Int = list.size
//
//    inner class HeaderViewHolder(private val binding: ItemCallLogHeaderBinding) :
//        RecyclerView.ViewHolder(binding.root) {
//        fun bind(item: CallLogListItem.Header) {
//            binding.tvDateHeader.text = item.dateSection
//        }
//    }
//
//    inner class CallLogViewHolder(private val binding: ItemCallHistoryRowBinding) :
//        RecyclerView.ViewHolder(binding.root) {
//
//        fun bind(item: CallLogListItem.CallItem, isFirst: Boolean, isLast: Boolean) {
//            val callLog = item.callLog
//
//            // Selection Logic
//            binding.cbSelect.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
//            binding.cbSelect.isChecked = selectedIds.contains(callLog.id)
//
//            if (isSelectionMode && selectedIds.contains(callLog.id)) {
//                binding.rootLayout.setBackgroundColor(Color.parseColor("#1A009688"))
//            } else {
//                applyBackground(isFirst, isLast)
//            }
//
//            // Contact Photo
//            if (callLog.photoUri != null) {
//                com.bumptech.glide.Glide.with(itemView.context)
//                    .load(android.net.Uri.parse(callLog.photoUri))
//                    .placeholder(R.drawable.ic_launcher_foreground)
//                    .circleCrop()
//                    .into(binding.ivContactPhoto)
//            } else {
//                binding.ivContactPhoto.setImageResource(R.drawable.ic_launcher_foreground)
//            }
//
//            // Icons & Colors
//            val (statusText, iconRes) = when (callLog.callType) {
//                CallLog.CallType.INCOMING -> "Incoming call" to android.R.drawable.sym_call_incoming
//                CallLog.CallType.OUTGOING -> "Outgoing call" to android.R.drawable.sym_call_outgoing
//                CallLog.CallType.MISSED -> "Missed call" to android.R.drawable.sym_call_missed
//            }
//            binding.ivCallType.setImageResource(iconRes)
//            val tintColor = if (callLog.callType == CallLog.CallType.MISSED) Color.RED else Color.parseColor("#2196F3")
//            binding.ivCallType.setColorFilter(tintColor)
//
//            if (style == 0) {
//                binding.tvTime.visibility = View.VISIBLE
//                binding.tvStatusDuration.visibility = View.VISIBLE
//                binding.divider.visibility = if (isLast) View.GONE else View.VISIBLE
//
//                binding.tvContactName.visibility = View.GONE
//                binding.tvPhoneNumber.visibility = View.GONE
//                binding.tvTimeAndDuration.visibility = View.GONE
//                binding.btnCall.visibility = View.GONE
//
//                binding.tvTime.text = callLog.getFormattedTime().lowercase()
//                binding.tvStatusDuration.text = "$statusText, ${formatDuration(callLog.duration)}"
//            } else {
//                binding.tvContactName.visibility = View.VISIBLE
//                binding.tvTimeAndDuration.visibility = View.VISIBLE
//                binding.btnCall.visibility = if (isSelectionMode) View.GONE else View.VISIBLE
//                binding.divider.visibility = View.GONE
//
//                binding.tvTime.visibility = View.GONE
//                binding.tvStatusDuration.visibility = View.GONE
//
//                if (callLog.contactName != null) {
//                    binding.tvContactName.text = callLog.contactName
//                    binding.tvPhoneNumber.text = callLog.phoneNumber
//                    binding.tvPhoneNumber.visibility = View.VISIBLE
//                } else {
//                    binding.tvContactName.text = callLog.phoneNumber
//                    binding.tvPhoneNumber.visibility = View.GONE
//                }
//
//                binding.tvContactName.setTextColor(if (callLog.callType == CallLog.CallType.MISSED) Color.RED else Color.BLACK)
//                val durationStr = if (callLog.duration > 0) " • ${callLog.getFormattedDuration()}" else ""
//                binding.tvTimeAndDuration.text = "${callLog.getFormattedTime()}$durationStr"
//            }
//
//            // Click Handlers
//            binding.root.setOnClickListener {
//                if (isSelectionMode) {
//                    toggleSelection(callLog.id)
//                } else {
//                    onItemClick(callLog)
//                }
//            }
//
//            binding.root.setOnLongClickListener {
//                if (!isSelectionMode) {
//                    isSelectionMode = true
//                    toggleSelection(callLog.id)
//                    true
//                } else false
//            }
//
//            binding.btnCall.setOnClickListener { onCallClick(callLog.phoneNumber) }
//        }
//
//        private fun toggleSelection(id: String) {
//            if (selectedIds.contains(id)) selectedIds.remove(id) else selectedIds.add(id)
//            notifyItemChanged(adapterPosition)
//            onSelectionChanged(selectedIds.size)
//        }
//
//        private fun applyBackground(isFirst: Boolean, isLast: Boolean) {
//            if (style == 0) {
//                val bgRes = when {
//                    isFirst && isLast -> R.drawable.bg_group_item_single
//                    isFirst -> R.drawable.bg_group_item_top
//                    isLast -> R.drawable.bg_group_item_bottom
//                    else -> R.drawable.bg_group_item_middle
//                }
//                binding.rootLayout.setBackgroundResource(bgRes)
//            } else {
//                val outValue = android.util.TypedValue()
//                binding.root.context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
//                binding.rootLayout.setBackgroundResource(outValue.resourceId)
//            }
//        }
//
//        private fun formatDuration(seconds: Long): String {
//            val mins = seconds / 60
//            val secs = seconds % 60
//            return "$mins mins $secs secs"
//        }
//    }
//
//    class CallLogDiffCallback(
//        private val oldList: List<CallLogListItem>,
//        private val newList: List<CallLogListItem>
//    ) : DiffUtil.Callback() {
//        override fun getOldListSize(): Int = oldList.size
//        override fun getNewListSize(): Int = newList.size
//        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
//            val oldItem = oldList[oldItemPosition]
//            val newItem = newList[newItemPosition]
//            return when {
//                oldItem is CallLogListItem.Header && newItem is CallLogListItem.Header ->
//                    oldItem.dateSection == newItem.dateSection
//                oldItem is CallLogListItem.CallItem && newItem is CallLogListItem.CallItem ->
//                    oldItem.callLog.id == newItem.callLog.id
//                else -> false
//            }
//        }
//        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
//            return oldList[oldItemPosition] == newList[newItemPosition]
//        }
//    }
//}

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
    private val onSelectionChanged: (Int) -> Unit = {},
    private val style: Int = 0 // 0 = New/History, 1 = Old/Recent
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val list = ArrayList<CallLogListItem>()

    // FIX: Private backing field prevents setter side-effects inside selectAll/clearSelection/longPress
    private var _isSelectionMode = false
    var isSelectionMode: Boolean
        get() = _isSelectionMode
        set(value) {
            _isSelectionMode = value
            if (!value) selectedIds.clear()
            notifyDataSetChanged()
        }

    val selectedIds = HashSet<String>()

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

    // FIX: _isSelectionMode backing field so selectedIds is NOT cleared before IDs are populated
    fun selectAll() {
        _isSelectionMode = true
        list.forEach { if (it is CallLogListItem.CallItem) selectedIds.add(it.callLog.id) }
        notifyDataSetChanged()
        onSelectionChanged(selectedIds.size)
    }

    // FIX: _isSelectionMode backing field + explicit single notifyDataSetChanged
    fun clearSelection() {
        selectedIds.clear()
        _isSelectionMode = false
        notifyDataSetChanged()
        onSelectionChanged(0)
    }

    // FIX: No longer uses submitList/DiffUtil (caused double-notify conflict).
    // Mutates list directly with single notifyDataSetChanged. Also strips orphan headers.
    fun removeItemsByIdAndExitSelection(ids: Set<String>) {
        // Step 1: Remove deleted call items (keep all headers for now)
        val filtered = list.filter { item ->
            when (item) {
                is CallLogListItem.Header  -> true
                is CallLogListItem.CallItem -> item.callLog.id !in ids
            }
        }

        // Step 2: Remove orphan headers (header not followed by a CallItem)
        val cleaned = mutableListOf<CallLogListItem>()
        for (i in filtered.indices) {
            val item = filtered[i]
            if (item is CallLogListItem.Header) {
                if (filtered.getOrNull(i + 1) is CallLogListItem.CallItem) {
                    cleaned.add(item)
                }
            } else {
                cleaned.add(item)
            }
        }

        // Step 3: Reset selection state via backing field (no setter side-effects)
        selectedIds.clear()
        _isSelectionMode = false

        // Step 4: Single atomic RecyclerView update
        list.clear()
        list.addAll(cleaned)
        notifyDataSetChanged()

        onSelectionChanged(0)
    }

    override fun getItemViewType(position: Int): Int {
        return when (list[position]) {
            is CallLogListItem.Header  -> VIEW_TYPE_HEADER
            is CallLogListItem.CallItem -> VIEW_TYPE_CALL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(
                ItemCallLogHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            else -> CallLogViewHolder(
                ItemCallHistoryRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = list[position]) {
            is CallLogListItem.Header  -> (holder as HeaderViewHolder).bind(item)
            is CallLogListItem.CallItem -> {
                val isFirstInGroup = position == 0 || list[position - 1] is CallLogListItem.Header
                val isLastInGroup  = position == list.size - 1 || list[position + 1] is CallLogListItem.Header
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

            // Selection UI
            binding.cbSelect.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
            binding.cbSelect.isChecked = selectedIds.contains(callLog.id)

            if (isSelectionMode && selectedIds.contains(callLog.id)) {
                binding.rootLayout.setBackgroundColor(Color.parseColor("#1A009688"))
            } else {
                applyBackground(isFirst, isLast)
            }

            // Contact Photo
            if (callLog.photoUri != null) {
                com.bumptech.glide.Glide.with(itemView.context)
                    .load(android.net.Uri.parse(callLog.photoUri))
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .circleCrop()
                    .into(binding.ivContactPhoto)
            } else {
                binding.ivContactPhoto.setImageResource(R.drawable.ic_launcher_foreground)
            }

            // Call type icon & color
            val (statusText, iconRes) = when (callLog.callType) {
                CallLog.CallType.INCOMING -> "Incoming call" to android.R.drawable.sym_call_incoming
                CallLog.CallType.OUTGOING -> "Outgoing call" to android.R.drawable.sym_call_outgoing
                CallLog.CallType.MISSED   -> "Missed call"   to android.R.drawable.sym_call_missed
            }
            binding.ivCallType.setImageResource(iconRes)
            val tintColor = if (callLog.callType == CallLog.CallType.MISSED)
                Color.RED else Color.parseColor("#2196F3")
            binding.ivCallType.setColorFilter(tintColor)

            if (style == 0) {
                // History style
                binding.tvTime.visibility = View.VISIBLE
                binding.tvStatusDuration.visibility = View.VISIBLE
                binding.divider.visibility = if (isLast) View.GONE else View.VISIBLE
                binding.tvContactName.visibility = View.GONE
                binding.tvPhoneNumber.visibility = View.GONE
                binding.tvTimeAndDuration.visibility = View.GONE
                binding.btnCall.visibility = View.GONE

                binding.tvTime.text = callLog.getFormattedTime().lowercase()
                binding.tvStatusDuration.text = "$statusText, ${formatDuration(callLog.duration)}"
            } else {
                // Recent style
                binding.tvContactName.visibility = View.VISIBLE
                binding.tvTimeAndDuration.visibility = View.VISIBLE
                binding.btnCall.visibility = if (isSelectionMode) View.GONE else View.VISIBLE
                binding.divider.visibility = View.GONE
                binding.tvTime.visibility = View.GONE
                binding.tvStatusDuration.visibility = View.GONE

                if (callLog.contactName != null) {
                    binding.tvContactName.text = callLog.contactName
                    binding.tvPhoneNumber.text = callLog.phoneNumber
                    binding.tvPhoneNumber.visibility = View.VISIBLE
                } else {
                    binding.tvContactName.text = callLog.phoneNumber
                    binding.tvPhoneNumber.visibility = View.GONE
                }

                binding.tvContactName.setTextColor(
                    if (callLog.callType == CallLog.CallType.MISSED) Color.RED else Color.BLACK
                )
                val durationStr = if (callLog.duration > 0) " • ${callLog.getFormattedDuration()}" else ""
                binding.tvTimeAndDuration.text = "${callLog.getFormattedTime()}$durationStr"
            }

            // Click: toggle selection or open detail
            binding.root.setOnClickListener {
                if (isSelectionMode) toggleSelection(callLog.id)
                else onItemClick(callLog)
            }

            // FIX: Use _isSelectionMode so toggleSelection fires AFTER mode is set
            // but BEFORE notifyDataSetChanged re-binds all items
            binding.root.setOnLongClickListener {
                if (!isSelectionMode) {
                    _isSelectionMode = true
                    notifyDataSetChanged()
                    toggleSelection(callLog.id)
                    true
                } else false
            }

            binding.btnCall.setOnClickListener { onCallClick(callLog.phoneNumber) }
        }

        private fun toggleSelection(id: String) {
            if (selectedIds.contains(id)) selectedIds.remove(id) else selectedIds.add(id)
            notifyItemChanged(adapterPosition)
            onSelectionChanged(selectedIds.size)
        }

        private fun applyBackground(isFirst: Boolean, isLast: Boolean) {
            if (style == 0) {
                val bgRes = when {
                    isFirst && isLast -> R.drawable.bg_group_item_single
                    isFirst           -> R.drawable.bg_group_item_top
                    isLast            -> R.drawable.bg_group_item_bottom
                    else              -> R.drawable.bg_group_item_middle
                }
                binding.rootLayout.setBackgroundResource(bgRes)
            } else {
                val outValue = android.util.TypedValue()
                binding.root.context.theme.resolveAttribute(
                    android.R.attr.selectableItemBackground, outValue, true
                )
                binding.rootLayout.setBackgroundResource(outValue.resourceId)
            }
        }

        private fun formatDuration(seconds: Long): String {
            val mins = seconds / 60
            val secs = seconds % 60
            return "$mins mins $secs secs"
        }
    }

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
