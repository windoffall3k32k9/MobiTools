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
 * Lớp phủ đồ họa quét mã QR và mã vạch với 4 góc định vị xanh phát quang và vệt laser quét động.
 */
class ScannerOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    // Trạng thái chế độ quét: true = QR (hình vuông), false = Mã vạch 1D (hình chữ nhật rộng)
    private var isQrMode: Boolean = true

    // Kích thước và thông số khung ngắm
    private val cornerRadius = 22f * density
    private val cornerLength = 36f * density
    private val cornerStroke = 5f * density
    private val glowStroke = 8f * density
    private val laserStroke = 2.5f * density

    // Vùng chữ nhật khung ngắm
    private val boxRect = RectF()
    private val targetRect = RectF()
    private val cornerPath = Path()

    // Cọ vẽ 4 góc định vị xanh dương phát quang
    private val cornerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = glowStroke
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.parseColor("#4D0066FF") // Quầng sáng xanh mờ bao ngoài
    }

    private val cornerCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = cornerStroke
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.parseColor("#0066FF") // Viền xanh electric chính
    }

    private val cornerHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.8f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.parseColor("#E0F2FE") // Lõi sáng trắng xanh ở trung tâm góc
    }

    // Cọ vẽ vệt sáng và tia laser
    private val laserPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = laserStroke
        strokeCap = Paint.Cap.ROUND
    }

    private val laserGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Hiệu ứng quét laser
    private var laserAnimator: ValueAnimator? = null
    private var boxAnimator: ValueAnimator? = null
    private var laserProgress: Float = 0.08f
    private var isLaserScanning: Boolean = true

    // Cập nhật chế độ quét (QR hoặc Mã vạch) và tạo chuyển động co giãn khung mượt mà.
    fun setScanMode(isQr: Boolean) {
        if (this.isQrMode == isQr) return
        this.isQrMode = isQr
        calculateTargetRect(width, height)
        animateBoxTransition()
    }

    // Tính toán kích thước khung ngắm theo chế độ quét hiện hành.
    private fun calculateTargetRect(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        val cx = w / 2f
        val cy = h / 2f

        if (isQrMode) {
            val squareSize = min(w * 0.72f, h * 0.60f)
            val half = squareSize / 2f
            targetRect.set(cx - half, cy - half, cx + half, cy + half)
        } else {
            val rectWidth = w * 0.82f
            val rectHeight = rectWidth * 0.52f
            val halfW = rectWidth / 2f
            val halfH = rectHeight / 2f
            targetRect.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH)
        }
    }

    // Hiệu ứng chuyển tiếp kích thước khung ngắm giữa QR và Mã vạch.
    private fun animateBoxTransition() {
        boxAnimator?.cancel()
        val startLeft = boxRect.left
        val startTop = boxRect.top
        val startRight = boxRect.right
        val startBottom = boxRect.bottom

        boxAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300L
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val f = animator.animatedValue as Float
                boxRect.set(
                    startLeft + (targetRect.left - startLeft) * f,
                    startTop + (targetRect.top - startTop) * f,
                    startRight + (targetRect.right - startRight) * f,
                    startBottom + (targetRect.bottom - startBottom) * f
                )
                invalidate()
            }
            start()
        }
    }

    // Tái cấu hình vùng vẽ khung ngắm khi kích thước view thay đổi.
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateTargetRect(w, h)
        boxRect.set(targetRect)
    }

    // Tự động kích hoạt hoạt ảnh laser khi gắn view vào phân cấp hiển thị.
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isLaserScanning) {
            startLaserAnimation()
        }
    }

    // Hủy hoạt ảnh khi view tháo khỏi phân cấp để giải phóng tài nguyên.
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopLaserAnimation()
    }

    // Bắt đầu chu kỳ quét tia laser chuyển động lên xuống liên tục.
    fun startLaserAnimation() {
        isLaserScanning = true
        if (laserAnimator?.isRunning == true) return
        laserAnimator = ValueAnimator.ofFloat(0.08f, 0.92f).apply {
            duration = 2400L
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

    // Tạm dừng tia laser quét khi đã hoàn tất giải mã mã vạch.
    fun stopLaserAnimation() {
        isLaserScanning = false
        laserAnimator?.cancel()
        laserAnimator = null
        invalidate()
    }

    // Thực hiện vẽ 4 góc định vị xanh phát quang và vệt sáng laser quét thời gian thực.
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (boxRect.isEmpty) return

        // 1. Vẽ 4 góc định vị phát quang phong cách hiện đại
        drawGlowingCorners(canvas)

        // 2. Vẽ tia laser quét chuyển động và quầng phát sáng
        if (isLaserScanning) {
            drawLaser(canvas)
        }
    }

    // Vẽ 4 góc định vị bo tròn chữ L với hiệu ứng phát quang 3 lớp.
    private fun drawGlowingCorners(canvas: Canvas) {
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

        // Vẽ lớp phát quang mờ bao ngoài
        canvas.drawPath(cornerPath, cornerGlowPaint)
        // Vẽ viền chính màu xanh electric
        canvas.drawPath(cornerPath, cornerCorePaint)
        // Vẽ tim sáng trắng bóng ở giữa
        canvas.drawPath(cornerPath, cornerHighlightPaint)
    }

    // Vẽ tia laser quét ngang và dải ánh sáng xanh tỏa hai phía.
    private fun drawLaser(canvas: Canvas) {
        val laserY = boxRect.top + laserProgress * boxRect.height()
        val startX = boxRect.left + 12f * density
        val endX = boxRect.right - 12f * density
        val glowHeight = 18f * density

        // 1. Dải hào quang phát sáng mờ phía sau tia laser
        val glowShader = LinearGradient(
            0f, laserY - glowHeight, 0f, laserY + glowHeight,
            intArrayOf(0x000066FF, 0x6600C2FF.toInt(), 0x000066FF),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        laserGlowPaint.shader = glowShader
        canvas.drawRect(startX, laserY - glowHeight, endX, laserY + glowHeight, laserGlowPaint)

        // 2. Tia laser chính màu xanh sáng với đuôi nhạt dần về 2 bên mép
        val lineShader = LinearGradient(
            startX, laserY, endX, laserY,
            intArrayOf(0x220066FF, 0xFF38BDF8.toInt(), 0x220066FF),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        laserPaint.shader = lineShader
        canvas.drawLine(startX, laserY, endX, laserY, laserPaint)
    }

    // Trả về vùng tọa độ của khung ngắm quét mã để căn chỉnh hoặc cắt ảnh nếu cần.
    fun getBoxRect(): RectF {
        return RectF(boxRect)
    }
}
