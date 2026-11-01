package com.example.baseproject.ui.scanner

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.min

/**
 * Lớp phủ khung ngắm quét mã QR và mã vạch với vùng nhìn xuyên suốt camera,
 * 4 góc bo định vị và hiệu ứng tia laser quét động.
 */
class ScannerOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    // Kích thước và thông số khung ngắm
    private val boxSize = 260f * density
    private val cornerRadius = 18f * density
    private val cornerLength = 26f * density
    private val cornerStroke = 3.5f * density
    private val borderStroke = 1.2f * density
    private val laserStroke = 2.5f * density

    // Vùng chữ nhật khung ngắm
    private val boxRect = RectF()
    private val maskPath = Path()
    private val cornerPath = Path()

    // Cọ vẽ
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#59000000")
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = borderStroke
        color = Color.parseColor("#4DFFFFFF")
    }

    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = cornerStroke
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.parseColor("#38BDF8")
    }

    private val laserPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = laserStroke
        strokeCap = Paint.Cap.ROUND
    }

    // Trạng thái hiệu ứng quét
    private var laserAnimator: ValueAnimator? = null
    private var laserProgress: Float = 0.08f
    private var isLaserScanning: Boolean = true

    // Cập nhật tọa độ và kích thước khung ngắm khi kích thước view thay đổi.
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val actualBoxSize = min(boxSize, min(w * 0.72f, h * 0.55f))
        val halfSize = actualBoxSize / 2f
        val centerX = w / 2f
        val centerY = (h / 2f) - (20f * density)

        boxRect.set(
            centerX - halfSize,
            centerY - halfSize,
            centerX + halfSize,
            centerY + halfSize
        )

        updateMaskPath(w, h)
    }

    // Tạo đường dẫn mặt nạ bán trong suốt bao ngoài, để trống khung quét ở giữa.
    private fun updateMaskPath(w: Int, h: Int) {
        maskPath.reset()
        maskPath.fillType = Path.FillType.EVEN_ODD
        maskPath.addRect(0f, 0f, w.toFloat(), h.toFloat(), Path.Direction.CW)
        maskPath.addRoundRect(boxRect, cornerRadius, cornerRadius, Path.Direction.CW)
    }

    // Bắt đầu chạy hiệu ứng khi gắn view vào cửa sổ.
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isLaserScanning) {
            startLaserAnimation()
        }
    }

    // Dừng hiệu ứng khi tháo view khỏi cửa sổ.
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopLaserAnimation()
    }

    // Bắt đầu hiệu ứng quét tia laser lên xuống trong khung ngắm.
    fun startLaserAnimation() {
        isLaserScanning = true
        if (laserAnimator?.isRunning == true) return
        laserAnimator = ValueAnimator.ofFloat(0.08f, 0.92f).apply {
            duration = 2200L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                laserProgress = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    // Tạm dừng hiệu ứng tia laser khi đã giải mã xong mã vạch.
    fun stopLaserAnimation() {
        isLaserScanning = false
        laserAnimator?.cancel()
        laserAnimator = null
        invalidate()
    }

    // Vẽ mặt nạ mờ, viền khung, 4 góc định vị và tia laser.
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (boxRect.isEmpty) return

        // 1. Mặt nạ tối bên ngoài, vùng trong suốt ở giữa cho camera chiếu qua
        canvas.drawPath(maskPath, maskPaint)

        // 2. Viền bo tròn thanh mảnh
        canvas.drawRoundRect(boxRect, cornerRadius, cornerRadius, borderPaint)

        // 3. 4 góc định vị L-shape màu xanh sáng
        drawCorners(canvas)

        // 4. Tia laser quét chuyển động
        if (isLaserScanning) {
            drawLaser(canvas)
        }
    }

    // Vẽ 4 góc định vị của khung ngắm quét mã.
    private fun drawCorners(canvas: Canvas) {
        val r = cornerRadius
        val len = cornerLength
        val left = boxRect.left
        val top = boxRect.top
        val right = boxRect.right
        val bottom = boxRect.bottom

        cornerPath.reset()

        // Góc trên bên trái
        cornerPath.moveTo(left, top + len)
        cornerPath.lineTo(left, top + r)
        cornerPath.quadTo(left, top, left + r, top)
        cornerPath.lineTo(left + len, top)

        // Góc trên bên phải
        cornerPath.moveTo(right - len, top)
        cornerPath.lineTo(right - r, top)
        cornerPath.quadTo(right, top, right, top + r)
        cornerPath.lineTo(right, top + len)

        // Góc dưới bên phải
        cornerPath.moveTo(right, bottom - len)
        cornerPath.lineTo(right, bottom - r)
        cornerPath.quadTo(right, bottom, right - r, bottom)
        cornerPath.lineTo(right - len, bottom)

        // Góc dưới bên trái
        cornerPath.moveTo(left + len, bottom)
        cornerPath.lineTo(left + r, bottom)
        cornerPath.quadTo(left, bottom, left, bottom - r)
        cornerPath.lineTo(left, bottom - len)

        canvas.drawPath(cornerPath, cornerPaint)
    }

    // Vẽ tia laser quét ngang chuyển động từ trên xuống dưới.
    private fun drawLaser(canvas: Canvas) {
        val laserY = boxRect.top + laserProgress * (boxRect.height())
        val startX = boxRect.left + 16f * density
        val endX = boxRect.right - 16f * density

        val laserShader = LinearGradient(
            startX, laserY,
            endX, laserY,
            intArrayOf(0x0038BDF8, 0xFF38BDF8.toInt(), 0x0038BDF8),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        laserPaint.shader = laserShader

        canvas.drawLine(startX, laserY, endX, laserY, laserPaint)
    }

    // Lấy tọa độ khung ngắm để căn chỉnh các thành phần giao diện khác nếu cần.
    fun getBoxRect(): RectF {
        return RectF(boxRect)
    }
}
