package com.example.baseproject.ui.level

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs

/**
 * View hiển thị ống thủy ngang (Horizontal Vial) với chất lỏng huỳnh quang và bọt khí di động.
 */
class HorizontalVialView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentAngle: Float = 0f
    private val maxDisplayAngle: Float = 30f

    // Vùng vẽ và kích thước tính toán
    private val outerRect = RectF()
    private val tubeRect = RectF()
    private val leftCapRect = RectF()
    private val rightCapRect = RectF()
    private val bubbleRect = RectF()
    private val bubbleGlareRect = RectF()

    // Cọ vẽ khung và viền
    private val casingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E293B") // Kim loại slate đậm
        style = Paint.Style.FILL
    }

    private val casingStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#334155")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val endCapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        style = Paint.Style.FILL
    }

    private val tubeLiquidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val tubeGlassGlarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }

    private val graduationLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
    }

    private val graduationLineGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4D7C0F")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    // Cọ vẽ bọt khí
    private val bubbleFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(210, 247, 255, 230)
        style = Paint.Style.FILL
    }

    private val bubbleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(110, 45, 74, 8)
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val bubbleGlarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 255, 255, 255)
        style = Paint.Style.FILL
    }

    // Cập nhật góc nghiêng trục ngang (độ) và vẽ lại giao diện.
    fun setAngle(angle: Float) {
        this.currentAngle = angle
        invalidate()
    }

    // Tái cấu hình gradient màu và vùng vẽ khi kích thước view thay đổi.
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return

        val padding = 6f
        outerRect.set(padding, padding, w.toFloat() - padding, h.toFloat() - padding)

        val capWidth = h * 0.42f
        leftCapRect.set(outerRect.left, outerRect.top, outerRect.left + capWidth, outerRect.bottom)
        rightCapRect.set(outerRect.right - capWidth, outerRect.top, outerRect.right, outerRect.bottom)

        tubeRect.set(outerRect.left + capWidth * 0.75f, outerRect.top + 3f, outerRect.right - capWidth * 0.75f, outerRect.bottom - 3f)

        // Gradient 3D dạng xi lanh thủy tinh chứa chất lỏng vàng chanh neon
        tubeLiquidPaint.shader = LinearGradient(
            0f, tubeRect.top, 0f, tubeRect.bottom,
            intArrayOf(
                Color.parseColor("#F7FFE5"), // Ánh sáng khúc xạ mép trên
                Color.parseColor("#D9FC38"), // Vàng chanh sáng
                Color.parseColor("#A3E635"), // Lục chuối neon trung tâm
                Color.parseColor("#65A30D")  // Đổ bóng đáy xi lanh
            ),
            floatArrayOf(0.0f, 0.25f, 0.65f, 1.0f),
            Shader.TileMode.CLAMP
        )
    }

    // Thực hiện vẽ toàn bộ ống thủy ngang bao gồm nắp đệm, chất lỏng, vạch cân bằng và bọt khí.
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val cornerRadius = h / 2f

        // 1. Vẽ khung vỏ ống thủy (Casing kim loại)
        canvas.drawRoundRect(outerRect, cornerRadius, cornerRadius, casingPaint)
        canvas.drawRoundRect(outerRect, cornerRadius, cornerRadius, casingStrokePaint)

        // 2. Vẽ thân chất lỏng huỳnh quang
        val tubeCornerRadius = tubeRect.height() / 2.2f
        canvas.drawRoundRect(tubeRect, tubeCornerRadius, tubeCornerRadius, tubeLiquidPaint)

        // Vệt sáng phản quang trên mặt kính
        val glareY = tubeRect.top + tubeRect.height() * 0.16f
        canvas.drawLine(tubeRect.left + 12f, glareY, tubeRect.right - 12f, glareY, tubeGlassGlarePaint)

        // 3. Vẽ 2 đầu nắp kim loại bịt ống
        canvas.drawRoundRect(leftCapRect, cornerRadius, cornerRadius, endCapPaint)
        canvas.drawRoundRect(rightCapRect, cornerRadius, cornerRadius, endCapPaint)

        // 4. Vẽ vạch chia cân bằng trung tâm
        val cx = w / 2f
        val bubbleWidth = h * 0.76f
        val bubbleHeight = h * 0.58f
        val targetGap = bubbleWidth + 8f

        val line1X = cx - targetGap / 2f
        val line2X = cx + targetGap / 2f
        val lineTop = tubeRect.top + 2f
        val lineBottom = tubeRect.bottom - 2f

        canvas.drawLine(line1X, lineTop, line1X, lineBottom, graduationLinePaint)
        canvas.drawLine(line2X, lineTop, line2X, lineBottom, graduationLinePaint)
        canvas.drawLine(line1X + 1f, lineTop, line1X + 1f, lineBottom, graduationLineGlowPaint)
        canvas.drawLine(line2X - 1f, lineTop, line2X - 1f, lineBottom, graduationLineGlowPaint)

        // 5. Tính toán vị trí bọt nước
        // Khi đầu phải chúc xuống (nghiêng phải), bọt khí nổi về mép trái cao hơn
        val capWidth = h * 0.42f
        val maxTravel = (tubeRect.width() - bubbleWidth) / 2f - 4f
        val normalizedAngle = (-currentAngle / maxDisplayAngle).coerceIn(-1f, 1f)
        val bx = cx + normalizedAngle * maxTravel
        val by = h / 2f

        bubbleRect.set(
            bx - bubbleWidth / 2f,
            by - bubbleHeight / 2f,
            bx + bubbleWidth / 2f,
            by + bubbleHeight / 2f
        )
        val bubbleCorner = bubbleHeight / 2f

        // Vẽ thân bọt khí và viền khúc xạ
        canvas.drawRoundRect(bubbleRect, bubbleCorner, bubbleCorner, bubbleFillPaint)
        canvas.drawRoundRect(bubbleRect, bubbleCorner, bubbleCorner, bubbleBorderPaint)

        // Đốm sáng phản chiếu bề mặt bọt khí tạo hiệu ứng thủy tinh 3D
        bubbleGlareRect.set(
            bx - bubbleWidth * 0.32f,
            by - bubbleHeight * 0.34f,
            bx + bubbleWidth * 0.32f,
            by - bubbleHeight * 0.08f
        )
        canvas.drawRoundRect(bubbleGlareRect, bubbleHeight * 0.15f, bubbleHeight * 0.15f, bubbleGlarePaint)
    }
}
