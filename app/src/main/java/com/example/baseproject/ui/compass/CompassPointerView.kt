package com.example.baseproject.ui.compass

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/**
 * View vẽ mũi tên chỉ hướng cố định góc 12 giờ gồm 2 vạch trắng song song và tam giác đỏ.
 */
class CompassPointerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        strokeWidth = 2.5f * density
        strokeCap = Paint.Cap.ROUND
    }

    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF4D4F.toInt()
        style = Paint.Style.FILL
    }

    private val arrowPath = Path()

    // Vẽ 2 vạch song song cố định và mũi tên tam giác đỏ chỉ thẳng vào tâm la bàn.
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val gap = 4f * density
        val lineHeight = 16f * density

        // Vẽ 2 vạch trắng song song
        canvas.drawLine(cx - gap, 2f * density, cx - gap, lineHeight, linePaint)
        canvas.drawLine(cx + gap, 2f * density, cx + gap, lineHeight, linePaint)

        // Vẽ tam giác đỏ chỉ hướng vào mặt số
        val triangleTop = lineHeight + 2f * density
        val triangleBottom = triangleTop + (8f * density)
        val triangleHalfWidth = 5f * density

        arrowPath.reset()
        arrowPath.moveTo(cx, triangleBottom)
        arrowPath.lineTo(cx - triangleHalfWidth, triangleTop)
        arrowPath.lineTo(cx + triangleHalfWidth, triangleTop)
        arrowPath.close()

        canvas.drawPath(arrowPath, arrowPaint)
    }
}
