package com.example.baseproject.ui.compass

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/**
 * View vẽ mặt số la bàn chi tiết 360 độ gồm các vạch chia nhỏ, số độ và các hướng B, Đ, N, T.
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

    private val degreeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF94A3B8.toInt()
        textSize = 10f * density
        textAlign = Paint.Align.CENTER
    }

    private val cardinalTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 14f * density
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val northCardinalTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF4D4F.toInt()
        textSize = 15f * density
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val textBounds = Rect()

    // Vẽ toàn bộ mặt đồng hồ la bàn 360 độ bao gồm vạch chia, chữ số và hướng địa lý.
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = (minOf(width, height) / 2f) - (6f * density)

        if (radius <= 30f * density) return

        // Vòng tròn bao ngoài cùng
        canvas.drawCircle(cx, cy, radius, outerRingPaint)
        // Vòng tròn mờ bên trong
        canvas.drawCircle(cx, cy, radius - (32f * density), outerRingPaint)

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
                    // Hướng Bắc (B)
                    val startX = cx + (radius - majorLen) * cosA
                    val startY = cy + (radius - majorLen) * sinA
                    val endX = cx + radius * cosA
                    val endY = cy + radius * sinA
                    canvas.drawLine(startX, startY, endX, endY, northTickPaint)

                    // Chữ B và số 0
                    drawRotatedText(canvas, "B", cx, cy, radius - (20f * density), degree.toFloat(), northCardinalTextPaint)
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
                    drawRotatedText(canvas, label, cx, cy, radius - (20f * density), degree.toFloat(), cardinalTextPaint)
                }
                degree % 30 == 0 -> {
                    val startX = cx + (radius - majorLen) * cosA
                    val startY = cy + (radius - majorLen) * sinA
                    val endX = cx + radius * cosA
                    val endY = cy + radius * sinA
                    canvas.drawLine(startX, startY, endX, endY, majorTickPaint)

                    val label = when (degree) {
                        45 -> "ĐB"
                        135 -> "ĐN"
                        225 -> "TN"
                        315 -> "TB"
                        else -> "$degree"
                    }
                    drawRotatedText(canvas, label, cx, cy, radius - (20f * density), degree.toFloat(), degreeTextPaint)
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
        }
    }

    // Vẽ nhãn chữ xoay theo hướng tâm để đảm bảo chữ số luôn hướng ra viền la bàn.
    private fun drawRotatedText(canvas: Canvas, text: String, cx: Float, cy: Float, distFromCenter: Float, degree: Float, paint: Paint) {
        val angleRad = Math.toRadians((degree - 90).toDouble())
        val tx = cx + distFromCenter * cos(angleRad).toFloat()
        val ty = cy + distFromCenter * sin(angleRad).toFloat()

        paint.getTextBounds(text, 0, text.size(), textBounds)
        val yOffset = textBounds.height() / 2f

        canvas.save()
        canvas.rotate(degree, tx, ty)
        canvas.drawText(text, tx, ty + yOffset, paint)
        canvas.restore()
    }

    private fun String.size() = length
}
