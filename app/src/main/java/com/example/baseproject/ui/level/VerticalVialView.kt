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

/**
 * View hiển thị ống thủy dọc (Vertical Vial) với chất lỏng huỳnh quang và bọt khí di động.
 */
class VerticalVialView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentAngle: Float = 0f
    private val maxDisplayAngle: Float = 30f

    // Vùng vẽ và kích thước tính toán
    private val outerRect = RectF()
    private val tubeRect = RectF()
    private val topCapRect = RectF()
    private val bottomCapRect = RectF()
    private val bubbleRect = RectF()
    private val bubbleGlareRect = RectF()

    // Cọ vẽ vỏ ngoài và ống thủy
    private val casingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E293B")
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

    // Cập nhật góc nghiêng trục dọc (độ) và vẽ lại giao diện.
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

        val capHeight = w * 0.42f
        topCapRect.set(outerRect.left, outerRect.top, outerRect.right, outerRect.top + capHeight)
        bottomCapRect.set(outerRect.left, outerRect.bottom - capHeight, outerRect.right, outerRect.bottom)

        tubeRect.set(outerRect.left + 3f, outerRect.top + capHeight * 0.75f, outerRect.right - 3f, outerRect.bottom - capHeight * 0.75f)

        // Gradient 3D dạng xi lanh thủy tinh thẳng đứng chứa chất lỏng vàng chanh neon
        tubeLiquidPaint.shader = LinearGradient(
            tubeRect.left, 0f, tubeRect.right, 0f,
            intArrayOf(
                Color.parseColor("#F7FFE5"),
                Color.parseColor("#D9FC38"),
                Color.parseColor("#A3E635"),
                Color.parseColor("#65A30D")
            ),
            floatArrayOf(0.0f, 0.25f, 0.65f, 1.0f),
            Shader.TileMode.CLAMP
        )
    }

    // Thực hiện vẽ toàn bộ ống thủy dọc bao gồm nắp đệm, chất lỏng, vạch cân bằng và bọt khí.
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val cornerRadius = w / 2f

        // 1. Vẽ khung vỏ ống thủy (Casing kim loại)
        canvas.drawRoundRect(outerRect, cornerRadius, cornerRadius, casingPaint)
        canvas.drawRoundRect(outerRect, cornerRadius, cornerRadius, casingStrokePaint)

        // 2. Vẽ thân chất lỏng huỳnh quang
        val tubeCornerRadius = tubeRect.width() / 2.2f
        canvas.drawRoundRect(tubeRect, tubeCornerRadius, tubeCornerRadius, tubeLiquidPaint)

        // Vệt sáng phản quang trên mặt kính bên trái
        val glareX = tubeRect.left + tubeRect.width() * 0.16f
        canvas.drawLine(glareX, tubeRect.top + 12f, glareX, tubeRect.bottom - 12f, tubeGlassGlarePaint)

        // 3. Vẽ 2 nắp đệm trên và dưới
        canvas.drawRoundRect(topCapRect, cornerRadius, cornerRadius, endCapPaint)
        canvas.drawRoundRect(bottomCapRect, cornerRadius, cornerRadius, endCapPaint)

        // 4. Vẽ vạch chia cân bằng trung tâm
        val cy = h / 2f
        val bubbleWidth = w * 0.58f
        val bubbleHeight = w * 0.76f
        val targetGap = bubbleHeight + 8f

        val line1Y = cy - targetGap / 2f
        val line2Y = cy + targetGap / 2f
        val lineLeft = tubeRect.left + 2f
        val lineRight = tubeRect.right - 2f

        canvas.drawLine(lineLeft, line1Y, lineRight, line1Y, graduationLinePaint)
        canvas.drawLine(lineLeft, line2Y, lineRight, line2Y, graduationLinePaint)
        canvas.drawLine(lineLeft, line1Y + 1f, lineRight, line1Y + 1f, graduationLineGlowPaint)
        canvas.drawLine(lineLeft, line2Y - 1f, lineRight, line2Y - 1f, graduationLineGlowPaint)

        // 5. Tính toán vị trí bọt nước
        // Khi đuôi máy chúc xuống, bọt khí nổi về mép trên cao hơn
        val maxTravel = (tubeRect.height() - bubbleHeight) / 2f - 4f
        val normalizedAngle = (-currentAngle / maxDisplayAngle).coerceIn(-1f, 1f)
        val bx = w / 2f
        val by = cy + normalizedAngle * maxTravel

        bubbleRect.set(
            bx - bubbleWidth / 2f,
            by - bubbleHeight / 2f,
            bx + bubbleWidth / 2f,
            by + bubbleHeight / 2f
        )
        val bubbleCorner = bubbleWidth / 2f

        // Vẽ thân bọt khí và viền khúc xạ
        canvas.drawRoundRect(bubbleRect, bubbleCorner, bubbleCorner, bubbleFillPaint)
        canvas.drawRoundRect(bubbleRect, bubbleCorner, bubbleCorner, bubbleBorderPaint)

        // Đốm sáng phản chiếu bề mặt bọt khí
        bubbleGlareRect.set(
            bx - bubbleWidth * 0.34f,
            by - bubbleHeight * 0.32f,
            bx - bubbleWidth * 0.08f,
            by + bubbleHeight * 0.32f
        )
        canvas.drawRoundRect(bubbleGlareRect, bubbleWidth * 0.15f, bubbleWidth * 0.15f, bubbleGlarePaint)
    }
}
