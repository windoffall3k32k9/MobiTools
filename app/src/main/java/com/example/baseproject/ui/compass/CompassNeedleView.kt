package com.example.baseproject.ui.compass

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/**
 * View vẽ mũi tên nam châm 2 hướng cố định (đầu Bắc đỏ, đầu Nam bạc) và vạch chuẩn góc 12 giờ.
 */
class CompassNeedleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    // Nửa kim Bắc đỏ tươi (bên trái) tạo hiệu ứng khối 3D
    private val northLeftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFEF4444.toInt()
        style = Paint.Style.FILL
    }

    // Nửa kim Bắc đỏ sẫm (bên phải)
    private val northRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFDC2626.toInt()
        style = Paint.Style.FILL
    }

    // Nửa kim Nam bạc sáng (bên trái)
    private val southLeftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF1F5F9.toInt()
        style = Paint.Style.FILL
    }

    // Nửa kim Nam xám đậm (bên phải)
    private val southRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF94A3B8.toInt()
        style = Paint.Style.FILL
    }

    // Vòng tròn tâm kim la bàn
    private val hubFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF0B132B.toInt()
        style = Paint.Style.FILL
    }

    private val hubStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF334155.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }

    // Vạch chuẩn chỉ mục màu trắng tại góc 12 giờ
    private val topIndexLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        strokeWidth = 3f * density
        strokeCap = Paint.Cap.ROUND
    }

    private val northLeftPath = Path()
    private val northRightPath = Path()
    private val southLeftPath = Path()
    private val southRightPath = Path()

    // Vẽ mũi tên nam châm 2 hướng gồm đầu Bắc đỏ, đầu Nam bạc và trục tròn ở giữa.
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = (minOf(width, height) / 2f) - (20f * density)

        if (radius <= 40f * density) return

        // 1. Vạch chuẩn màu trắng tại góc 12 giờ đỉnh trên cùng
        val indexTop = 2f * density
        val indexBottom = indexTop + (26f * density)
        canvas.drawLine(cx, indexTop, cx, indexBottom, topIndexLinePaint)

        // Bán kính trục tròn ở tâm
        val hubRadius = 46f * density
        val halfNeedleWidth = 14f * density
        val needleTipRadius = radius - (12f * density)

        // 2. Nửa kim Bắc (Mũi tên đỏ chỉ lên 12 giờ)
        northLeftPath.reset()
        northLeftPath.moveTo(cx, cy - needleTipRadius)
        northLeftPath.lineTo(cx - halfNeedleWidth, cy - hubRadius + (8f * density))
        northLeftPath.lineTo(cx, cy - hubRadius + (8f * density))
        northLeftPath.close()
        canvas.drawPath(northLeftPath, northLeftPaint)

        northRightPath.reset()
        northRightPath.moveTo(cx, cy - needleTipRadius)
        northRightPath.lineTo(cx, cy - hubRadius + (8f * density))
        northRightPath.lineTo(cx + halfNeedleWidth, cy - hubRadius + (8f * density))
        northRightPath.close()
        canvas.drawPath(northRightPath, northRightPaint)

        // 3. Nửa kim Nam (Mũi tên bạc chỉ xuống 6 giờ)
        southLeftPath.reset()
        southLeftPath.moveTo(cx, cy + needleTipRadius)
        southLeftPath.lineTo(cx, cy + hubRadius - (8f * density))
        southLeftPath.lineTo(cx - halfNeedleWidth, cy + hubRadius - (8f * density))
        southLeftPath.close()
        canvas.drawPath(southLeftPath, southLeftPaint)

        southRightPath.reset()
        southRightPath.moveTo(cx, cy + needleTipRadius)
        southRightPath.lineTo(cx + halfNeedleWidth, cy + hubRadius - (8f * density))
        southRightPath.lineTo(cx, cy + hubRadius - (8f * density))
        southRightPath.close()
        canvas.drawPath(southRightPath, southRightPaint)

        // 4. Trục tròn ở tâm bao quanh chữ số phương vị
        canvas.drawCircle(cx, cy, hubRadius, hubFillPaint)
        canvas.drawCircle(cx, cy, hubRadius, hubStrokePaint)
    }
}
