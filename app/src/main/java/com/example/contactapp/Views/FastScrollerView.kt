package com.example.contactapp.Views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.contactapp.R
import kotlin.math.roundToInt

/**
 * Custom Samsung-style Fast Scroller View with floating bubble overlay
 * Supports Star icon for Favorites section
 */
class FastScrollerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Alphabet sections with Star for favorites
    private val sections = listOf("★") + ('A'..'Z').map { it.toString() } + "#"
    
    // Paint objects
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 24f
        textAlign = Paint.Align.CENTER
        color = Color.GRAY
    }
    
    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
        textAlign = Paint.Align.CENTER
        color = ContextCompat.getColor(context, R.color.teal_700)
        isFakeBoldText = true
    }
    
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.teal_700)
        style = Paint.Style.FILL
        setShadowLayer(16f, 0f, 6f, 0x40000000)
    }
    
    private val bubbleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 56f
        textAlign = Paint.Align.CENTER
        color = Color.WHITE
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
    
    private val bubbleSize = 140f
    private val bubbleMargin = 100f
    
    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw all section letters
        sections.forEachIndexed { index, letter ->
            val y = sectionHeight * index + sectionHeight / 2
            val textY = y + (textPaint.textSize / 3)
            
            val paint = if (index == activeIndex) activePaint else textPaint
            canvas.drawText(letter, width / 2f, textY, paint)
        }
        
        // Draw floating bubble if active
        if (activeIndex >= 0 && bubbleAlpha > 0) {
            val bubbleY = (sectionHeight * activeIndex + sectionHeight / 2).coerceIn(bubbleSize/2, height - bubbleSize/2)
            val bubbleX = width - bubbleMargin - bubbleSize / 2
            
            val alpha = (bubbleAlpha * 255).roundToInt()
            bubblePaint.alpha = alpha
            bubbleTextPaint.alpha = alpha
            
            // Draw circular bubble background
            canvas.drawCircle(bubbleX, bubbleY, bubbleSize / 2, bubblePaint)
            
            val letter = sections[activeIndex]
            if (letter == "★") {
                // Draw Star Icon instead of text
                drawStar(canvas, bubbleX, bubbleY, bubbleSize * 0.4f, alpha)
            } else {
                // Draw letter in bubble
                val textY = bubbleY + (bubbleTextPaint.textSize / 3)
                canvas.drawText(letter, bubbleX, textY, bubbleTextPaint)
            }
        }
    }

    private fun drawStar(canvas: Canvas, x: Float, y: Float, radius: Float, alpha: Int) {
        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            this.alpha = alpha
            style = Paint.Style.FILL
        }
        val path = Path()
        val innerRadius = radius / 2.5f
        val spikes = 5
        var angle = -Math.PI / 2
        val step = Math.PI / spikes

        path.moveTo(x + (radius * Math.cos(angle)).toFloat(), y + (radius * Math.sin(angle)).toFloat())
        for (i in 0 until spikes) {
            angle += step
            path.lineTo(x + (innerRadius * Math.cos(angle)).toFloat(), y + (innerRadius * Math.sin(angle)).toFloat())
            angle += step
            path.lineTo(x + (radius * Math.cos(angle)).toFloat(), y + (radius * Math.sin(angle)).toFloat())
        }
        path.close()
        canvas.drawPath(path, starPaint)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val index = (event.y / sectionHeight).toInt().coerceIn(0, sections.size - 1)
                if (index != activeIndex) {
                    activeIndex = index
                    onSectionSelected?.invoke(sections[index])
                    showBubble()
                    invalidate()
                    performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
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
    
    private fun showBubble() {
        bubbleAnimator?.cancel()
        bubbleAnimator = ValueAnimator.ofFloat(bubbleAlpha, 1f).apply {
            duration = 100
            addUpdateListener { animator ->
                bubbleAlpha = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }
    
    private fun hideBubble() {
        bubbleAnimator?.cancel()
        bubbleAnimator = ValueAnimator.ofFloat(bubbleAlpha, 0f).apply {
            duration = 300
            addUpdateListener { animator ->
                bubbleAlpha = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = 60
        val width = resolveSize(desiredWidth, widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
    }
}
