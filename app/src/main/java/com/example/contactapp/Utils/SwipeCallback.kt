package com.example.contactapp.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

abstract class SwipeCallback(context: Context) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    private val background = ColorDrawable()
    private val callIcon: Drawable? = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_call)
    private val messageIcon: Drawable? = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_send)
    private val callColor = Color.parseColor("#4CAF50") // Green
    private val messageColor = Color.parseColor("#2196F3") // Blue

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
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

        if (dX > 0) { // Swiping Right (Message)
            background.color = messageColor
            background.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
            background.draw(c)

            messageIcon?.let {
                val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                val iconTop = itemView.top + (itemView.height - it.intrinsicHeight) / 2
                val iconBottom = iconTop + it.intrinsicHeight
                val iconLeft = itemView.left + iconMargin
                val iconRight = itemView.left + iconMargin + it.intrinsicWidth
                it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                it.draw(c)
            }
        } else if (dX < 0) { // Swiping Left (Call)
            background.color = callColor
            background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
            background.draw(c)

            callIcon?.let {
                val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                val iconTop = itemView.top + (itemView.height - it.intrinsicHeight) / 2
                val iconBottom = iconTop + it.intrinsicHeight
                val iconRight = itemView.right - iconMargin
                val iconLeft = itemView.right - iconMargin - it.intrinsicWidth
                it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                it.draw(c)
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}
