package com.example.contactapp.Utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

abstract class SwipeCallback(context: Context) :
    ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    private val background = ColorDrawable()

    private val callIcon: Drawable? =
        ContextCompat.getDrawable(context, android.R.drawable.ic_menu_call)

    private val messageIcon: Drawable? =
        ContextCompat.getDrawable(context, android.R.drawable.ic_menu_send)

    private val callColor = Color.parseColor("#4CAF50")
    private val messageColor = Color.parseColor("#2196F3")

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

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

        if (dX > 0) { // RIGHT → SMS
            background.color = messageColor
            background.setBounds(
                itemView.left,
                itemView.top,
                itemView.left + dX.toInt(),
                itemView.bottom
            )
            background.draw(c)

            messageIcon?.let {
                val margin = (itemView.height - it.intrinsicHeight) / 2
                val top = itemView.top + margin
                val bottom = top + it.intrinsicHeight
                val left = itemView.left + margin
                val right = left + it.intrinsicWidth
                it.setBounds(left, top, right, bottom)
                it.draw(c)
            }

        } else if (dX < 0) { // LEFT → Call
            background.color = callColor
            background.setBounds(
                itemView.right + dX.toInt(),
                itemView.top,
                itemView.right,
                itemView.bottom
            )
            background.draw(c)

            callIcon?.let {
                val margin = (itemView.height - it.intrinsicHeight) / 2
                val top = itemView.top + margin
                val bottom = top + it.intrinsicHeight
                val right = itemView.right - margin
                val left = right - it.intrinsicWidth
                it.setBounds(left, top, right, bottom)
                it.draw(c)
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}
