package com.example.baseproject.ui.timer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/**
 * View vẽ vòng tròn chứa các vạch tỏa tròn xung quanh đồng hồ đếm ngược theo phong cách đồng hồ kim.
 */
class TimerTickDialView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progress: Float = 1.0f

    private val totalTicks = 120
    private val density = resources.displayMetrics.density

    private val activeTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF7A00.toInt()
        strokeWidth = 2.5f * density
        strokeCap = Paint.Cap.ROUND
    }

    private val inactiveTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2A2E39.toInt()
        strokeWidth = 2f * density
        strokeCap = Paint.Cap.ROUND
    }

    // Cập nhật tỷ lệ tiến trình còn lại từ 0f (hết giờ) đến 1f (đầy đủ) và yêu cầu vẽ lại.
    fun setProgress(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        if (progress != clamped) {
            progress = clamped
            invalidate()
        }
    }

    // Vẽ toàn bộ các vạch chia xung quanh tâm vòng tròn đồng hồ.
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val outerRadius = (minOf(width, height) / 2f) - (8f * density)
        val tickLength = 10f * density

        if (outerRadius <= tickLength) return

        val activeCount = (totalTicks * progress).toInt()

        for (i in 0 until totalTicks) {
            val angleDeg = (i * 360f / totalTicks) - 90f
            val angleRad = Math.toRadians(angleDeg.toDouble())

            val cosA = cos(angleRad).toFloat()
            val sinA = sin(angleRad).toFloat()

            val startX = cx + (outerRadius - tickLength) * cosA
            val startY = cy + (outerRadius - tickLength) * sinA
            val endX = cx + outerRadius * cosA
            val endY = cy + outerRadius * sinA

            val paint = if (i <= activeCount) activeTickPaint else inactiveTickPaint
            canvas.drawLine(startX, startY, endX, endY, paint)
        }
    }
}
