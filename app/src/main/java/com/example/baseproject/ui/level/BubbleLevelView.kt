package com.example.baseproject.ui.level

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * View vẽ mặt thước thủy tròn với bọt nước cân bằng theo thời gian thực.
 */
class BubbleLevelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var pitch: Float = 0f
    private var roll: Float = 0f

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F1F5F9")
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CBD5E1")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val targetRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#94A3B8")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#94A3B8")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val bubbleNormalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3B82F6") // Xanh dương khi lệch
        style = Paint.Style.FILL
    }

    private val bubbleBalancedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#10B981") // Xanh lá khi thăng bằng
        style = Paint.Style.FILL
    }

    // Cập nhật góc nghiêng pitch và roll để tính toán vị trí bọt nước.
    fun setAngles(pitchAngle: Float, rollAngle: Float) {
        this.pitch = pitchAngle
        this.roll = rollAngle
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = min(cx, cy) - 16f
        val bubbleRadius = maxRadius * 0.22f

        // Vẽ nền tròn
        canvas.drawCircle(cx, cy, maxRadius, bgPaint)
        canvas.drawCircle(cx, cy, maxRadius, borderPaint)

        // Vẽ các vòng tròn mốc chuẩn
        canvas.drawCircle(cx, cy, bubbleRadius, targetRingPaint)
        canvas.drawCircle(cx, cy, maxRadius * 0.55f, targetRingPaint)

        // Vẽ dấu chữ thập tâm
        canvas.drawLine(cx - maxRadius + 10f, cy, cx + maxRadius - 10f, cy, crosshairPaint)
        canvas.drawLine(cx, cy - maxRadius + 10f, cx, cy + maxRadius - 10f, crosshairPaint)

        // Tính độ lệch của bọt nước (giới hạn trong vòng tròn đo)
        val maxOffset = maxRadius - bubbleRadius - 6f
        val maxAngle = 45f // 45 độ lệch tối đa chạm viền

        val dx = (roll / maxAngle).coerceIn(-1f, 1f) * maxOffset
        val dy = (-pitch / maxAngle).coerceIn(-1f, 1f) * maxOffset

        val bx = cx + dx
        val by = cy + dy

        val isBalanced = kotlin.math.abs(pitch) < 1.0f && kotlin.math.abs(roll) < 1.0f
        val paintToUse = if (isBalanced) bubbleBalancedPaint else bubbleNormalPaint

        canvas.drawCircle(bx, by, bubbleRadius, paintToUse)
    }
}
