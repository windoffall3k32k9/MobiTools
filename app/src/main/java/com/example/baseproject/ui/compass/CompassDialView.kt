package com.example.baseproject.ui.compass

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/**
 * View vẽ mặt số la bàn chi tiết 360 độ gồm các vạch chia nhỏ, số độ bên ngoài và các hướng B, Đ, N, T bên trong.
 */
class CompassDialView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    private val outerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
        color = 0xFF334155.toInt()
    }

    private val majorTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
        color = 0xFFFFFFFF.toInt()
        strokeCap = Paint.Cap.ROUND
    }

    private val northTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        color = 0xFFFF4D4F.toInt()
        strokeCap = Paint.Cap.ROUND
    }

    private val mediumTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
        color = 0xFFCBD5E1.toInt()
        strokeCap = Paint.Cap.ROUND
    }

    private val minorTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
        color = 0xFF475569.toInt()
        strokeCap = Paint.Cap.ROUND
    }

    private val degreeNumberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE2E8F0.toInt()
        textSize = 11f * density
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val cardinalTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 15f * density
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val northCardinalTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF4D4F.toInt()
        textSize = 16f * density
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val subCardinalTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF94A3B8.toInt()
        textSize = 11f * density
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val northArrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF4D4F.toInt()
        style = Paint.Style.FILL
    }

    private val textBounds = Rect()
    private val northTrianglePath = Path()

    // Vẽ toàn bộ mặt đồng hồ la bàn 360 độ gồm vạch chia, số độ bên ngoài và chữ hướng bên trong.
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        // Chừa khoảng trống bên ngoài để vẽ các chữ số 0, 30, 60... 330
        val radius = (minOf(width, height) / 2f) - (20f * density)

        if (radius <= 40f * density) return

        // Vòng tròn bao ngoài cùng của vạch chia
        canvas.drawCircle(cx, cy, radius, outerRingPaint)
        // Vòng tròn mờ bên trong
        canvas.drawCircle(cx, cy, radius - (16f * density), outerRingPaint)

        val majorLen = 14f * density
        val medLen = 9f * density
        val minorLen = 5f * density

        // Duyệt từng góc 2 độ một để vẽ 180 vạch chia xung quanh chu vi
        for (degree in 0 until 360 step 2) {
            val angleRad = Math.toRadians((degree - 90).toDouble())
            val cosA = cos(angleRad).toFloat()
            val sinA = sin(angleRad).toFloat()

            when {
                degree == 0 -> {
                    // Vạch hướng Bắc (đỏ)
                    val startX = cx + (radius - majorLen) * cosA
                    val startY = cy + (radius - majorLen) * sinA
                    val endX = cx + radius * cosA
                    val endY = cy + radius * sinA
                    canvas.drawLine(startX, startY, endX, endY, northTickPaint)

                    // Tam giác đỏ chỉ hướng Bắc ngay sát vạch 0 độ
                    drawNorthTriangle(canvas, cx, cy, radius)

                    // Chữ B màu đỏ nằm phía trong
                    drawRotatedText(canvas, "B", cx, cy, radius - (26f * density), degree.toFloat(), northCardinalTextPaint)
                    // Số 0 nằm phía ngoài
                    drawRotatedText(canvas, "0", cx, cy, radius + (12f * density), degree.toFloat(), degreeNumberPaint)
                }
                degree % 90 == 0 -> {
                    val startX = cx + (radius - majorLen) * cosA
                    val startY = cy + (radius - majorLen) * sinA
                    val endX = cx + radius * cosA
                    val endY = cy + radius * sinA
                    canvas.drawLine(startX, startY, endX, endY, majorTickPaint)

                    val label = when (degree) {
                        90 -> "Đ"
                        180 -> "N"
                        270 -> "T"
                        else -> ""
                    }
                    drawRotatedText(canvas, label, cx, cy, radius - (26f * density), degree.toFloat(), cardinalTextPaint)
                    drawRotatedText(canvas, "$degree", cx, cy, radius + (12f * density), degree.toFloat(), degreeNumberPaint)
                }
                degree % 30 == 0 -> {
                    val startX = cx + (radius - majorLen) * cosA
                    val startY = cy + (radius - majorLen) * sinA
                    val endX = cx + radius * cosA
                    val endY = cy + radius * sinA
                    canvas.drawLine(startX, startY, endX, endY, majorTickPaint)

                    drawRotatedText(canvas, "$degree", cx, cy, radius + (12f * density), degree.toFloat(), degreeNumberPaint)
                }
                degree % 10 == 0 -> {
                    val startX = cx + (radius - medLen) * cosA
                    val startY = cy + (radius - medLen) * sinA
                    val endX = cx + radius * cosA
                    val endY = cy + radius * sinA
                    canvas.drawLine(startX, startY, endX, endY, mediumTickPaint)
                }
                else -> {
                    val startX = cx + (radius - minorLen) * cosA
                    val startY = cy + (radius - minorLen) * sinA
                    val endX = cx + radius * cosA
                    val endY = cy + radius * sinA
                    canvas.drawLine(startX, startY, endX, endY, minorTickPaint)
                }
            }

            // Vẽ các hướng phụ ĐB (45°), ĐN (135°), TN (225°), TB (315°)
            if (degree % 90 == 45 || degree == 44 || degree == 46) {
                if (degree == 44 || degree == 46 || degree == 134 || degree == 136 || degree == 224 || degree == 226 || degree == 314 || degree == 316) {
                    // Bỏ qua tránh trùng
                }
            }
        }

        // Vẽ 4 hướng phụ ở góc 45, 135, 225, 315 độ
        drawRotatedText(canvas, "ĐB", cx, cy, radius - (26f * density), 45f, subCardinalTextPaint)
        drawRotatedText(canvas, "ĐN", cx, cy, radius - (26f * density), 135f, subCardinalTextPaint)
        drawRotatedText(canvas, "TN", cx, cy, radius - (26f * density), 225f, subCardinalTextPaint)
        drawRotatedText(canvas, "TB", cx, cy, radius - (26f * density), 315f, subCardinalTextPaint)
    }

    // Vẽ tam giác đỏ nhỏ hướng vào tâm tại mốc 0 độ trên mặt xoay.
    private fun drawNorthTriangle(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val triWidth = 8f * density
        val triHeight = 7f * density
        val triY = cy - radius + (2f * density)

        northTrianglePath.reset()
        northTrianglePath.moveTo(cx, triY)
        northTrianglePath.lineTo(cx - triWidth / 2f, triY - triHeight)
        northTrianglePath.lineTo(cx + triWidth / 2f, triY - triHeight)
        northTrianglePath.close()

        canvas.drawPath(northTrianglePath, northArrowPaint)
    }

    // Vẽ nhãn chữ xoay theo hướng tâm để đảm bảo chữ số luôn hướng ra viền la bàn.
    private fun drawRotatedText(canvas: Canvas, text: String, cx: Float, cy: Float, distFromCenter: Float, degree: Float, paint: Paint) {
        val angleRad = Math.toRadians((degree - 90).toDouble())
        val tx = cx + distFromCenter * cos(angleRad).toFloat()
        val ty = cy + distFromCenter * sin(angleRad).toFloat()

        paint.getTextBounds(text, 0, text.length, textBounds)
        val yOffset = textBounds.height() / 2f

        canvas.save()
        canvas.rotate(degree, tx, ty)
        canvas.drawText(text, tx, ty + yOffset, paint)
        canvas.restore()
    }
}
