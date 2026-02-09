package com.example.contactapp.Views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.contactapp.R
import kotlin.math.roundToInt

/**
 * Custom A-Z Fast Scroller View with floating bubble overlay
 * Premium design with smooth animations and touch feedback
 */
class FastScrollerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Alphabet sections
    private val sections = ('A'..'Z').toList() + '#'
    
    // Paint objects
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
        textAlign = Paint.Align.CENTER
        color = ContextCompat.getColor(context, R.color.teal_700)
    }
    
    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 32f
        textAlign = Paint.Align.CENTER
        color = ContextCompat.getColor(context, R.color.teal_700)
        isFakeBoldText = true
    }
    
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.teal_700)
        style = Paint.Style.FILL
        setShadowLayer(12f, 0f, 4f, 0x40000000)
    }
    
    private val bubbleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 48f
        textAlign = Paint.Align.CENTER
        color = 0xFFFFFFFF.toInt()
        isFakeBoldText = true
    }
    
    // State
    private var activeIndex = -1
    private var bubbleAlpha = 0f
    private var bubbleAnimator: ValueAnimator? = null
    
    // Callbacks
    var onSectionSelected: ((String) -> Unit)? = null
    
    // Dimensions
    private val sectionHeight: Float
        get() = height.toFloat() / sections.size
    
    private val bubbleSize = 120f
    private val bubbleMargin = 80f
    
    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null) // Enable shadow
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw all section letters
        sections.forEachIndexed { index, letter ->
            val y = sectionHeight * index + sectionHeight / 2
            val textY = y + (textPaint.textSize / 3)
            
            val paint = if (index == activeIndex) activePaint else textPaint
            canvas.drawText(letter.toString(), width / 2f, textY, paint)
        }
        
        // Draw floating bubble if active
        if (activeIndex >= 0 && bubbleAlpha > 0) {
            val bubbleY = sectionHeight * activeIndex + sectionHeight / 2
            val bubbleX = width - bubbleMargin - bubbleSize / 2
            
            // Apply alpha to bubble
            val alpha = (bubbleAlpha * 255).roundToInt()
            bubblePaint.alpha = alpha
            bubbleTextPaint.alpha = alpha
            
            // Draw bubble background (rounded rectangle)
            val rect = RectF(
                bubbleX - bubbleSize / 2,
                bubbleY - bubbleSize / 2,
                bubbleX + bubbleSize / 2,
                bubbleY + bubbleSize / 2
            )
            canvas.drawRoundRect(rect, 16f, 16f, bubblePaint)
            
            // Draw letter in bubble
            val letter = sections[activeIndex].toString()
            val textY = bubbleY + (bubbleTextPaint.textSize / 3)
            canvas.drawText(letter, bubbleX, textY, bubbleTextPaint)
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val index = (event.y / sectionHeight).toInt().coerceIn(0, sections.size - 1)
                if (index != activeIndex) {
                    activeIndex = index
                    onSectionSelected?.invoke(sections[index].toString())
                    performHapticFeedback()
                    showBubble()
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                hideBubble()
                activeIndex = -1
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    /**
     * Show bubble with animation
     */
    private fun showBubble() {
        bubbleAnimator?.cancel()
        bubbleAnimator = ValueAnimator.ofFloat(bubbleAlpha, 1f).apply {
            duration = 150
            addUpdateListener { animator ->
                bubbleAlpha = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }
    
    /**
     * Hide bubble with animation
     */
    private fun hideBubble() {
        bubbleAnimator?.cancel()
        bubbleAnimator = ValueAnimator.ofFloat(bubbleAlpha, 0f).apply {
            duration = 200
            addUpdateListener { animator ->
                bubbleAlpha = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }
    
    /**
     * Perform haptic feedback
     */
    private fun performHapticFeedback() {
        performHapticFeedback(
            android.view.HapticFeedbackConstants.CLOCK_TICK,
            android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Fixed width for the scroller
        val desiredWidth = 80
        val width = resolveSize(desiredWidth, widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
    }
}
