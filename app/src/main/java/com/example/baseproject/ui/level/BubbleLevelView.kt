package com.example.baseproject.ui.level

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.hypot
import kotlin.math.min

/**
 * View vẽ mặt thước thủy tròn mắt bò (Bullseye Level) với đĩa chất lỏng huỳnh quang và bọt nước 2 chiều.
 */
class BubbleLevelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var pitch: Float = 0f
    private var roll: Float = 0f
    private val maxAngle: Float = 30f

    // Cọ vẽ khung viền kim loại
    private val bezelOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#334155")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val bezelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E293B")
        style = Paint.Style.STROKE
        strokeWidth = 14f
    }

    private val bezelInnerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    // Cọ vẽ đĩa chất lỏng huỳnh quang
    private val liquidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Cọ vẽ tâm chuẩn và đường chữ thập
    private val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }

    private val targetRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val outerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(90, 45, 74, 8)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val balancedGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 196, 240, 58)
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    // Cọ vẽ bọt khí
    private val bubbleFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(215, 247, 255, 230)
        style = Paint.Style.FILL
    }

    private val bubbleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 45, 74, 8)
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
    }

    private val bubbleSpecularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(230, 255, 255, 255)
        style = Paint.Style.FILL
    }

    // Cập nhật góc nghiêng pitch (trước sau) và roll (trái phải) để tính toán vị trí bọt nước.
    fun setAngles(pitchAngle: Float, rollAngle: Float) {
        this.pitch = pitchAngle
        this.roll = rollAngle
        invalidate()
    }

    // Tái cấu hình gradient hình tròn khi kích thước thay đổi.
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return

        val cx = w / 2f
        val cy = h / 2f
        val maxRadius = min(cx, cy) - 10f

        liquidPaint.shader = RadialGradient(
            cx, cy, maxRadius,
            intArrayOf(
                Color.parseColor("#E6FF85"), // Vùng tâm sáng
                Color.parseColor("#C4F03A"), // Vàng chanh chuối neon
                Color.parseColor("#95DE14"), // Lục neon
                Color.parseColor("#5A960B")  // Đổ bóng viền đĩa
            ),
            floatArrayOf(0f, 0.4f, 0.75f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    // Thực hiện vẽ mặt thước tròn, các vòng đồng tâm, dấu chữ thập và bọt khí hình cầu 3D.
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val cx = w / 2f
        val cy = h / 2f
        val maxRadius = min(cx, cy) - 12f
        val bubbleRadius = maxRadius * 0.22f

        // 1. Vẽ đĩa chất lỏng huỳnh quang
        canvas.drawCircle(cx, cy, maxRadius, liquidPaint)

        // 2. Vẽ các vòng cữ chia độ và chữ thập tâm
        val targetRingRadius = bubbleRadius + 6f
        val isBalanced = kotlin.math.abs(pitch) < 0.8f && kotlin.math.abs(roll) < 0.8f
        if (isBalanced) {
            canvas.drawCircle(cx, cy, targetRingRadius + 2f, balancedGlowPaint)
        }

        canvas.drawCircle(cx, cy, targetRingRadius, targetRingPaint)
        canvas.drawCircle(cx, cy, maxRadius * 0.58f, outerRingPaint)

        // Đường chữ thập
        canvas.drawLine(cx - maxRadius + 6f, cy, cx + maxRadius - 6f, cy, crosshairPaint)
        canvas.drawLine(cx, cy - maxRadius + 6f, cx, cy + maxRadius - 6f, crosshairPaint)

        // 3. Tính toán vị trí bọt khí 2 chiều (phản xạ ngược chiều vector gia tốc trọng trường)
        val maxTravel = maxRadius - bubbleRadius - 6f
        var dx = (-roll / maxAngle).coerceIn(-1f, 1f) * maxTravel
        var dy = (-pitch / maxAngle).coerceIn(-1f, 1f) * maxTravel

        val distance = hypot(dx, dy)
        if (distance > maxTravel) {
            val ratio = maxTravel / distance
            dx *= ratio
            dy *= ratio
        }

        val bx = cx + dx
        val by = cy + dy

        // 4. Vẽ bọt khí hình cầu và đốm sáng phản chiếu
        canvas.drawCircle(bx, by, bubbleRadius, bubbleFillPaint)
        canvas.drawCircle(bx, by, bubbleRadius, bubbleBorderPaint)

        val specularRadius = bubbleRadius * 0.28f
        val specularX = bx - bubbleRadius * 0.32f
        val specularY = by - bubbleRadius * 0.34f
        canvas.drawCircle(specularX, specularY, specularRadius, bubbleSpecularPaint)

        // 5. Vẽ viền kim loại bảo vệ bên ngoài
        canvas.drawCircle(cx, cy, maxRadius, bezelPaint)
        canvas.drawCircle(cx, cy, maxRadius + 7f, bezelOuterPaint)
        canvas.drawCircle(cx, cy, maxRadius - 7f, bezelInnerBorderPaint)
    }
}
