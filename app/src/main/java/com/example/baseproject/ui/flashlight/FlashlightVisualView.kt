package com.example.baseproject.ui.flashlight

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import com.example.baseproject.R
import kotlin.math.hypot

/**
 * Custom View vẽ đèn pin phần cứng chân thực với chùm sáng ấm hướng lên,
 * vòng xanh kim loại và nút nguồn phát sáng tích hợp trực tiếp trên thân đèn.
 */
class FlashlightVisualView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var isTorchOn: Boolean = false
    private var brightnessPercent: Int = 100
    private var beamAlphaAnim: Float = 0f
    private var buttonPressScale: Float = 1f
    private var onPowerClickListener: (() -> Unit)? = null

    // Giá trị kích thước hình học
    private var centerX: Float = 0f
    private var headTopY: Float = 0f
    private var headWidth: Float = 0f
    private var headHeight: Float = 0f
    private var neckBottomY: Float = 0f
    private var bodyWidth: Float = 0f
    private var powerButtonCenterY: Float = 0f
    private var powerButtonRadius: Float = 0f

    // Đường path tái sử dụng
    private val beamPath = Path()
    private val headPath = Path()
    private val reflectorOval = RectF()
    private val blueRingRect = RectF()
    private val bodyRect = RectF()
    private val powerButtonBounds = RectF()

    // Các cọ vẽ tái sử dụng
    private val beamPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val radialGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val reflectorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val headBezelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val blueRingPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val knurlLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val powerButtonHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val powerButtonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val powerIconPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var alphaAnimator: ValueAnimator? = null

    init {
        knurlLinePaint.apply {
            color = Color.parseColor("#3B4556")
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
            alpha = 70
        }

        powerIconPaint.apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 6f
            strokeCap = Paint.Cap.ROUND
        }

        powerButtonHaloPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
    }

    // Thiết lập callback khi người dùng bấm vào nút nguồn của đèn pin.
    fun setOnPowerClickListener(listener: () -> Unit) {
        this.onPowerClickListener = listener
    }

    // Cập nhật trạng thái bật tắt của đèn pin kèm hiệu ứng chùm sáng.
    fun setTorchState(isOn: Boolean, animated: Boolean = true) {
        if (isTorchOn == isOn) return
        isTorchOn = isOn
        val targetAlpha = if (isOn) 1f else 0f

        if (animated) {
            alphaAnimator?.cancel()
            alphaAnimator = ValueAnimator.ofFloat(beamAlphaAnim, targetAlpha).apply {
                duration = 240
                addUpdateListener {
                    beamAlphaAnim = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            alphaAnimator?.cancel()
            beamAlphaAnim = targetAlpha
            invalidate()
        }
    }

    // Cập nhật cường độ sáng (1 đến 100) để thay đổi độ trong suốt của chùm tia sáng.
    fun setBeamBrightness(percent: Int) {
        this.brightnessPercent = percent.coerceIn(1, 100)
        invalidate()
    }

    // Trả về trạng thái bật tắt hiện tại của đèn pin trên giao diện.
    fun isTorchActive(): Boolean = isTorchOn

    // Tính toán lại tọa độ, kích thước và gradient khi khung nhìn thay đổi kích thước.
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return

        val density = resources.displayMetrics.density
        centerX = w / 2f

        // Đèn pin nằm ở nửa dưới, chùm sáng tỏa lên trên
        headTopY = h * 0.28f
        headWidth = (148f * density).coerceAtMost(w * 0.46f)
        headHeight = 36f * density
        val neckHeight = 52f * density
        neckBottomY = headTopY + headHeight + neckHeight
        bodyWidth = headWidth * 0.64f
        val bodyBottomY = h.toFloat()

        // Vị trí và bán kính nút nguồn trên thân đèn
        powerButtonRadius = 38f * density
        powerButtonCenterY = neckBottomY + 54f * density
        powerButtonBounds.set(
            centerX - powerButtonRadius,
            powerButtonCenterY - powerButtonRadius,
            centerX + powerButtonRadius,
            powerButtonCenterY + powerButtonRadius
        )

        // Khung gương phản xạ trên đầu đèn
        reflectorOval.set(
            centerX - headWidth / 2f,
            headTopY,
            centerX + headWidth / 2f,
            headTopY + headHeight
        )

        // Đường chùm sáng hình nón tỏa lên phía trên
        beamPath.reset()
        val beamTopSpread = w * 0.48f
        beamPath.moveTo(centerX - headWidth * 0.44f, headTopY + headHeight * 0.35f)
        beamPath.lineTo(centerX - beamTopSpread, 0f)
        beamPath.lineTo(centerX + beamTopSpread, 0f)
        beamPath.lineTo(centerX + headWidth * 0.44f, headTopY + headHeight * 0.35f)
        beamPath.close()

        // Gradient cho chùm tia sáng
        beamPaint.shader = LinearGradient(
            centerX, headTopY,
            centerX, 0f,
            intArrayOf(
                Color.argb(205, 255, 213, 79),
                Color.argb(120, 255, 243, 176),
                Color.argb(0, 255, 248, 225)
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )

        // Quầng sáng tỏa tròn mềm mại phía sau đầu đèn
        radialGlowPaint.shader = RadialGradient(
            centerX, headTopY,
            w * 0.48f,
            intArrayOf(
                Color.argb(90, 255, 224, 130),
                Color.argb(35, 255, 238, 170),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )

        // Path hình chóp cụt của cổ đèn pin
        headPath.reset()
        headPath.moveTo(centerX - headWidth / 2f, headTopY + headHeight * 0.5f)
        headPath.lineTo(centerX + headWidth / 2f, headTopY + headHeight * 0.5f)
        headPath.lineTo(centerX + bodyWidth / 2f, neckBottomY)
        headPath.lineTo(centerX - bodyWidth / 2f, neckBottomY)
        headPath.close()

        // Gradient kim loại cho đầu và cổ đèn pin
        headBezelPaint.shader = LinearGradient(
            centerX - headWidth / 2f, 0f,
            centerX + headWidth / 2f, 0f,
            intArrayOf(
                Color.parseColor("#1B232F"),
                Color.parseColor("#384457"),
                Color.parseColor("#53627A"),
                Color.parseColor("#384457"),
                Color.parseColor("#19212D")
            ),
            floatArrayOf(0f, 0.28f, 0.52f, 0.78f, 1f),
            Shader.TileMode.CLAMP
        )

        // Vòng nhấn màu xanh điện nổi bật ngay cổ đèn
        val ringHeight = 9f * density
        blueRingRect.set(
            centerX - bodyWidth / 2f,
            neckBottomY,
            centerX + bodyWidth / 2f,
            neckBottomY + ringHeight
        )
        blueRingPaint.shader = LinearGradient(
            blueRingRect.left, 0f,
            blueRingRect.right, 0f,
            intArrayOf(
                Color.parseColor("#0040A8"),
                Color.parseColor("#0070F3"),
                Color.parseColor("#38BDF8"),
                Color.parseColor("#0070F3"),
                Color.parseColor("#003B99")
            ),
            floatArrayOf(0f, 0.3f, 0.5f, 0.72f, 1f),
            Shader.TileMode.CLAMP
        )

        // Thân hình trụ đèn pin
        bodyRect.set(
            centerX - bodyWidth / 2f,
            neckBottomY + ringHeight,
            centerX + bodyWidth / 2f,
            bodyBottomY
        )
        bodyPaint.shader = LinearGradient(
            bodyRect.left, 0f,
            bodyRect.right, 0f,
            intArrayOf(
                Color.parseColor("#161D27"),
                Color.parseColor("#283344"),
                Color.parseColor("#3E4D65"),
                Color.parseColor("#283344"),
                Color.parseColor("#131923")
            ),
            floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    // Vẽ toàn bộ các thành phần trực quan của đèn pin lên canvas.
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        // 1. Vẽ chùm sáng và quầng sáng nếu đèn đang bật
        val effectiveAlpha = (beamAlphaAnim * (brightnessPercent / 100f)).coerceIn(0f, 1f)
        if (effectiveAlpha > 0.01f) {
            val saveCount = canvas.save()
            beamPaint.alpha = (255 * effectiveAlpha).toInt()
            radialGlowPaint.alpha = (255 * effectiveAlpha).toInt()

            canvas.drawCircle(centerX, headTopY, width * 0.46f, radialGlowPaint)
            canvas.drawPath(beamPath, beamPaint)
            canvas.restoreToCount(saveCount)
        }

        // 2. Vẽ thân đèn pin và vân nổi kim loại
        canvas.drawRect(bodyRect, bodyPaint)
        drawKnurlingTexture(canvas)

        // 3. Vẽ vòng cổ nhấn màu xanh
        canvas.drawRect(blueRingRect, blueRingPaint)

        // 4. Vẽ cổ đèn và đầu đèn
        canvas.drawPath(headPath, headBezelPaint)

        // 5. Vẽ thấu kính gương phản xạ
        drawReflectorLens(canvas, effectiveAlpha)

        // 6. Vẽ nút nguồn tròn tích hợp trên thân đèn
        drawPowerButton(canvas)
    }

    // Vẽ họa tiết vân kim cương tạo độ bám thực tế cho tay cầm đèn pin.
    private fun drawKnurlingTexture(canvas: Canvas) {
        val density = resources.displayMetrics.density
        val step = 10f * density
        val top = bodyRect.top + 6f * density
        val bottom = bodyRect.bottom

        var y = top
        while (y < bottom) {
            canvas.drawLine(bodyRect.left + 4f, y, bodyRect.right - 4f, y, knurlLinePaint)
            y += step
        }
    }

    // Vẽ thấu kính phản xạ ánh sáng vàng khi bật hoặc xám kim loại khi tắt.
    private fun drawReflectorLens(canvas: Canvas, lightAlpha: Float) {
        if (lightAlpha > 0.05f) {
            reflectorPaint.shader = RadialGradient(
                centerX, reflectorOval.centerY(),
                headWidth * 0.5f,
                intArrayOf(
                    Color.argb((255 * lightAlpha).toInt(), 255, 255, 235),
                    Color.argb((230 * lightAlpha).toInt(), 255, 224, 130),
                    Color.argb((210 * lightAlpha).toInt(), 255, 179, 0)
                ),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP
            )
        } else {
            reflectorPaint.shader = RadialGradient(
                centerX, reflectorOval.centerY(),
                headWidth * 0.5f,
                intArrayOf(
                    Color.parseColor("#424E60"),
                    Color.parseColor("#28313E"),
                    Color.parseColor("#171D26")
                ),
                floatArrayOf(0f, 0.7f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawOval(reflectorOval, reflectorPaint)

        // Vành bezel bảo vệ mặt trước
        headBezelPaint.style = Paint.Style.STROKE
        headBezelPaint.strokeWidth = 3f
        canvas.drawOval(reflectorOval, headBezelPaint)
        headBezelPaint.style = Paint.Style.FILL
    }

    // Vẽ nút nguồn tròn với vòng hào quang phát sáng và biểu tượng công tắc nguồn.
    private fun drawPowerButton(canvas: Canvas) {
        val save = canvas.save()
        canvas.scale(buttonPressScale, buttonPressScale, centerX, powerButtonCenterY)

        val density = resources.displayMetrics.density
        val haloRadius = powerButtonRadius + 7f * density

        // Hào quang xanh xung quanh nút bấm
        if (isTorchOn) {
            powerButtonHaloPaint.color = Color.parseColor("#0066FF")
            powerButtonHaloPaint.alpha = 240
            powerButtonHaloPaint.strokeWidth = 3.5f * density
            canvas.drawCircle(centerX, powerButtonCenterY, haloRadius, powerButtonHaloPaint)

            // Vòng phát sáng mờ phía ngoài
            powerButtonHaloPaint.strokeWidth = 7f * density
            powerButtonHaloPaint.alpha = 75
            canvas.drawCircle(centerX, powerButtonCenterY, haloRadius + 3f * density, powerButtonHaloPaint)
        } else {
            powerButtonHaloPaint.color = Color.parseColor("#64748B")
            powerButtonHaloPaint.alpha = 90
            powerButtonHaloPaint.strokeWidth = 2f * density
            canvas.drawCircle(centerX, powerButtonCenterY, haloRadius, powerButtonHaloPaint)
        }

        // Mặt nút bấm
        if (isTorchOn) {
            powerButtonPaint.shader = RadialGradient(
                centerX, powerButtonCenterY,
                powerButtonRadius,
                intArrayOf(
                    Color.parseColor("#2F8AF5"),
                    Color.parseColor("#0066FF"),
                    Color.parseColor("#004CBD")
                ),
                floatArrayOf(0f, 0.65f, 1f),
                Shader.TileMode.CLAMP
            )
        } else {
            powerButtonPaint.shader = RadialGradient(
                centerX, powerButtonCenterY,
                powerButtonRadius,
                intArrayOf(
                    Color.parseColor("#334155"),
                    Color.parseColor("#1E293B"),
                    Color.parseColor("#0F172A")
                ),
                floatArrayOf(0f, 0.7f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(centerX, powerButtonCenterY, powerButtonRadius, powerButtonPaint)

        // Biểu tượng nút nguồn trắng
        drawPowerSymbol(canvas)

        canvas.restoreToCount(save)
    }

    // Vẽ biểu tượng thanh đứng và vòng tròn hở đặc trưng của nút nguồn.
    private fun drawPowerSymbol(canvas: Canvas) {
        val density = resources.displayMetrics.density
        val symbolRadius = powerButtonRadius * 0.44f
        val strokeW = 3.2f * density
        powerIconPaint.strokeWidth = strokeW
        powerIconPaint.color = Color.WHITE

        // Vòng tròn khuyết trên đầu
        val arcRect = RectF(
            centerX - symbolRadius,
            powerButtonCenterY - symbolRadius,
            centerX + symbolRadius,
            powerButtonCenterY + symbolRadius
        )
        canvas.drawArc(arcRect, 130f, 280f, false, powerIconPaint)

        // Vạch thẳng chính giữa
        val lineTop = powerButtonCenterY - symbolRadius * 1.05f
        val lineBottom = powerButtonCenterY + symbolRadius * 0.15f
        canvas.drawLine(centerX, lineTop, centerX, lineBottom, powerIconPaint)
    }

    // Xử lý cử chỉ chạm trên nút bấm nguồn để tạo hoạt ảnh nhấn và kích hoạt sự kiện.
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val distance = hypot(event.x - centerX, event.y - powerButtonCenterY)
                val isInsideButton = distance <= powerButtonRadius * 1.5f
                val isInsideFlashlight = event.x in (centerX - headWidth / 2f)..(centerX + headWidth / 2f) &&
                        event.y >= headTopY && event.y <= height

                if (isInsideButton || isInsideFlashlight) {
                    animateButtonScale(0.92f)
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                animateButtonScale(1f)
                val distance = hypot(event.x - centerX, event.y - powerButtonCenterY)
                val isInsideButton = distance <= powerButtonRadius * 1.6f
                val isInsideFlashlight = event.x in (centerX - headWidth / 2f)..(centerX + headWidth / 2f) &&
                        event.y >= headTopY && event.y <= height

                if (isInsideButton || isInsideFlashlight) {
                    performClick()
                    onPowerClickListener?.invoke()
                    return true
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                animateButtonScale(1f)
            }
        }
        return super.onTouchEvent(event)
    }

    // Hỗ trợ accessibility khi click vào custom view.
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    // Tạo hiệu ứng co giãn tự nhiên dạng lò xo khi nhấn nút bấm nguồn.
    private fun animateButtonScale(targetScale: Float) {
        ValueAnimator.ofFloat(buttonPressScale, targetScale).apply {
            duration = 150
            interpolator = OvershootInterpolator(2.5f)
            addUpdateListener {
                buttonPressScale = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }
}
