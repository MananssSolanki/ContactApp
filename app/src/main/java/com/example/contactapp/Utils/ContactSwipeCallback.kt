package com.example.contactapp.Utils

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.contactapp.R

/**
 * ItemTouchHelper callback for swipe gestures on contacts
 * Swipe right: Call
 * Swipe left: SMS
 */
class ContactSwipeCallback(
    private val onSwipeRight: (Int) -> Unit,
    private val onSwipeLeft: (Int) -> Unit
) : ItemTouchHelper.SimpleCallback(
    0,
    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
) {
    
    private val paint = Paint()
    private var callIcon: Drawable? = null
    private var smsIcon: Drawable? = null
    
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false
    
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        when (direction) {
            ItemTouchHelper.RIGHT -> onSwipeRight(position)
            ItemTouchHelper.LEFT -> onSwipeLeft(position)
        }
    }
    
    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val context = recyclerView.context
        
        // Initialize icons if needed
        if (callIcon == null) {
            callIcon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_call)
            smsIcon = ContextCompat.getDrawable(context, android.R.drawable.sym_action_chat)
        }
        
        if (dX > 0) {
            // Swipe right - Call
            drawSwipeBackground(
                c,
                itemView.left.toFloat(),
                itemView.top.toFloat(),
                itemView.left + dX,
                itemView.bottom.toFloat(),
                ContextCompat.getColor(context, R.color.teal_700)
            )
            
            // Draw call icon
            callIcon?.let { icon ->
                val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
                val iconTop = itemView.top + iconMargin
                val iconBottom = iconTop + icon.intrinsicHeight
                val iconLeft = itemView.left + iconMargin
                val iconRight = iconLeft + icon.intrinsicWidth
                
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                icon.setTint(0xFFFFFFFF.toInt())
                icon.draw(c)
            }
        } else if (dX < 0) {
            // Swipe left - SMS
            drawSwipeBackground(
                c,
                itemView.right + dX,
                itemView.top.toFloat(),
                itemView.right.toFloat(),
                itemView.bottom.toFloat(),
                ContextCompat.getColor(context, R.color.teal_200)
            )
            
            // Draw SMS icon
            smsIcon?.let { icon ->
                val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
                val iconTop = itemView.top + iconMargin
                val iconBottom = iconTop + icon.intrinsicHeight
                val iconRight = itemView.right - iconMargin
                val iconLeft = iconRight - icon.intrinsicWidth
                
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                icon.setTint(0xFFFFFFFF.toInt())
                icon.draw(c)
            }
        }
        
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
    
    /**
     * Draw swipe background with rounded corners
     */
    private fun drawSwipeBackground(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        color: Int
    ) {
        paint.color = color
        val rect = RectF(left, top, right, bottom)
        canvas.drawRect(rect, paint)
    }
    
    /**
     * Control swipe threshold
     */
    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return 0.3f // 30% swipe to trigger action
    }
    
    /**
     * Control swipe escape velocity
     */
    override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
        return defaultValue * 1.5f
    }
}
