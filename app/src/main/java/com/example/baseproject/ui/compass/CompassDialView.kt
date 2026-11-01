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
 * View vẽ mặt số la bàn 360 độ tích hợp liền khối với kim nam châm 2 hướng N - S (Bắc - Nam).
 * Toàn bộ mặt la bàn và kim nam châm tạo thành 1 khối thống nhất cùng xoay theo góc phương vị.
 */
class CompassDialView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    // Paint cho vòng tròn viền ngoài
    private val outerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
        color = 0xFF334155.toInt()
    }

    // Paint cho vạch chia chính 30 độ
    private val majorTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
        color = 0xFFFFFFFF.toInt()
        strokeCap = Paint.Cap.ROUND
    }

    // Paint cho vạch 0 độ hướng Bắc
    private val northTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        color = 0xFFFF4D4F.toInt()
        strokeCap = Paint.Cap.ROUND
    }

    // Paint cho vạch chia trung bình 10 độ
    private val mediumTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
        color = 0xFFCBD5E1.toInt()
        strokeCap = Paint.Cap.ROUND
    }

    // Paint cho vạch chia nhỏ 2 độ
    private val minorTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
        color = 0xFF475569.toInt()
        strokeCap = Paint.Cap.ROUND
    }

    // Paint cho chữ số độ bên ngoài (0, 30, 60... 330)
    private val degreeNumberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE2E8F0.toInt()
        textSize = 11f * density
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    // Paint cho các hướng chính B, Đ, N, T bên trong
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

    // Paint cho mũi tên đỏ nhỏ tại mốc 0 độ
    private val northSmallArrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF4D4F.toInt()
        style = Paint.Style.FILL
    }

    // Paint cho kim nam châm 2 đầu N - S dạng khối 3D vát cạnh
    private val needleNorthLeftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFEF4444.toInt()
        style = Paint.Style.FILL
    }

    private val needleNorthRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFDC2626.toInt()
        style = Paint.Style.FILL
    }

    private val needleSouthLeftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF1F5F9.toInt()
        style = Paint.Style.FILL
    }

    private val needleSouthRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF94A3B8.toInt()
        style = Paint.Style.FILL
    }

    // Trục tròn ở tâm bao quanh chữ số hiển thị
    private val hubFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF0B132B.toInt()
        style = Paint.Style.FILL
    }

    private val hubStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF334155.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }

    private val textBounds = Rect()
    private val northTrianglePath = Path()
    private val needlePath = Path()

    // Vẽ toàn bộ mặt đồng hồ la bàn liền khối với kim nam châm 2 đầu N - S.
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = (minOf(width, height) / 2f) - (20f * density)

        if (radius <= 40f * density) return

        // 1. Vẽ kim nam châm 2 hướng N - S gắn chặt vào mặt la bàn
        drawMagneticNeedle(canvas, cx, cy, radius)

        // 2. Vẽ vòng tròn bao ngoài của vạch chia
        canvas.drawCircle(cx, cy, radius, outerRingPaint)
        canvas.drawCircle(cx, cy, radius - (16f * density), outerRingPaint)

        val majorLen = 14f * density
        val medLen = 9f * density
        val minorLen = 5f * density

        // 3. Vẽ 180 vạch chia (bước nhảy 2 độ) xung quanh chu vi
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

                    // Mũi tên tam giác đỏ nhỏ chỉ thẳng vào vạch 0 độ
                    drawNorthTriangle(canvas, cx, cy, radius)

                    // Chữ B màu đỏ nằm phía trong
                    drawRotatedText(canvas, "B", cx, cy, radius - (26f * density), degree.toFloat(), northCardinalTextPaint)
                    // Số 0 nằm phía ngoài vạch
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
        }

        // 4. Vẽ 4 hướng phụ ĐB, ĐN, TN, TB
        drawRotatedText(canvas, "ĐB", cx, cy, radius - (26f * density), 45f, subCardinalTextPaint)
        drawRotatedText(canvas, "ĐN", cx, cy, radius - (26f * density), 135f, subCardinalTextPaint)
        drawRotatedText(canvas, "TN", cx, cy, radius - (26f * density), 225f, subCardinalTextPaint)
        drawRotatedText(canvas, "TB", cx, cy, radius - (26f * density), 315f, subCardinalTextPaint)
    }

    // Vẽ mũi tên nam châm 2 đầu cố định gắn chặt theo trục N (Bắc) và S (Nam) của mặt la bàn.
    private fun drawMagneticNeedle(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val hubRadius = 46f * density
        val halfNeedleWidth = 14f * density
        // Đỉnh mũi tên chạm sát vào vạch chia của hướng B và N
        val needleTipRadius = radius - (16f * density)

        // --- ĐẦU KIM BẮC (N) MÀU ĐỎ VUỐT NHỌN VỀ MỐC 0 ĐỘ TRÊN LA BÀN ---
        // Nửa trái kim Bắc (đỏ tươi)
        needlePath.reset()
        needlePath.moveTo(cx, cy - needleTipRadius)
        needlePath.lineTo(cx - halfNeedleWidth, cy - hubRadius + (6f * density))
        needlePath.lineTo(cx, cy - hubRadius + (6f * density))
        needlePath.close()
        canvas.drawPath(needlePath, needleNorthLeftPaint)

        // Nửa phải kim Bắc (đỏ sẫm 3D)
        needlePath.reset()
        needlePath.moveTo(cx, cy - needleTipRadius)
        needlePath.lineTo(cx, cy - hubRadius + (6f * density))
        needlePath.lineTo(cx + halfNeedleWidth, cy - hubRadius + (6f * density))
        needlePath.close()
        canvas.drawPath(needlePath, needleNorthRightPaint)

        // --- ĐẦU KIM NAM (S) MÀU BẠC VUỐT NHỌN VỀ MỐC 180 ĐỘ TRÊN LA BÀN ---
        // Nửa trái kim Nam (bạc sáng)
        needlePath.reset()
        needlePath.moveTo(cx, cy + needleTipRadius)
        needlePath.lineTo(cx, cy + hubRadius - (6f * density))
        needlePath.lineTo(cx - halfNeedleWidth, cy + hubRadius - (6f * density))
        needlePath.close()
        canvas.drawPath(needlePath, needleSouthLeftPaint)

        // Nửa phải kim Nam (xám đậm 3D)
        needlePath.reset()
        needlePath.moveTo(cx, cy + needleTipRadius)
        needlePath.lineTo(cx + halfNeedleWidth, cy + hubRadius - (6f * density))
        needlePath.lineTo(cx, cy + hubRadius - (6f * density))
        needlePath.close()
        canvas.drawPath(needlePath, needleSouthRightPaint)

        // --- TRỤC TRÒN TÂM LA BÀN NẰM ĐÈ LÊN GỐC KIM NAM CHÂM ---
        canvas.drawCircle(cx, cy, hubRadius, hubFillPaint)
        canvas.drawCircle(cx, cy, hubRadius, hubStrokePaint)
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

        canvas.drawPath(northTrianglePath, northSmallArrowPaint)
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
