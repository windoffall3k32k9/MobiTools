package com.example.baseproject.ui.level

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import com.example.baseproject.R
import com.example.baseproject.base.BaseToolFragment
import com.example.baseproject.databinding.FragmentLevelBinding
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Công cụ thước thủy bọt nước chuẩn xác đa chế độ: Tất cả, Ngang, Dọc và Tròn theo phong cách công nghiệp sáng màu.
 */
class BubbleLevelFragment : BaseToolFragment<FragmentLevelBinding>(), SensorEventListener {

    override val toolId: String = "bubble_level"
    override val toolTitleResId: Int = R.string.tool_bubble_level_title

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    // Mốc hiệu chuẩn cân bằng
    private var pitchOffset = 0f
    private var rollOffset = 0f
    private var filteredPitch = 0f
    private var filteredRoll = 0f
    private var wasBalanced = false

    // Trạng thái giữ kết quả đo và toàn màn hình
    private var isHeld = false
    private var isFullscreen = false

    // Chế độ hiển thị thước đo
    enum class LevelMode {
        ALL, HORIZONTAL, VERTICAL, CIRCULAR
    }

    private var currentMode = LevelMode.ALL

    private val prefs by lazy {
        requireContext().getSharedPreferences("bubble_level_prefs", Context.MODE_PRIVATE)
    }

    // Khởi tạo đối tượng ViewBinding cho giao diện FragmentLevel.
    override fun provideViewBinding(container: ViewGroup?): FragmentLevelBinding {
        return FragmentLevelBinding.inflate(layoutInflater, container, false)
    }

    // Cấu hình ban đầu cho các view, tải mốc chuẩn và đăng ký cảm biến.
    override fun initViews() {
        pitchOffset = prefs.getFloat("pitch_offset", 0f)
        rollOffset = prefs.getFloat("roll_offset", 0f)

        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            viewBinding.tvLevelSensorWarning.visibility = View.VISIBLE
        }

        setupToolbarMoreMenu()
        setupTabButtons()
        setupActionButtons()
        updateModeUI(LevelMode.ALL)
    }

    // Kích hoạt lắng nghe dữ liệu từ cảm biến gia tốc khi màn hình hoạt động.
    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    // Hủy đăng ký lắng nghe cảm biến khi màn hình tạm dừng để tiết kiệm pin.
    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
        if (isFullscreen) {
            exitFullscreen()
        }
    }

    // Cấu hình nút 3 chấm trên thanh công cụ để mở menu hiệu chuẩn và đặt lại mốc 0.
    private fun setupToolbarMoreMenu() {
        val btnMore = view?.findViewById<ImageView>(R.id.btnToolbarMore) ?: return
        btnMore.visibility = View.VISIBLE
        btnMore.setOnClickListener { anchorView ->
            val context = requireContext()
            val popup = PopupMenu(context, anchorView)
            popup.menu.add(0, 1, 0, R.string.level_calibrate_menu)
            popup.menu.add(0, 2, 1, R.string.level_reset_menu)

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    1 -> {
                        calibrateZero()
                        true
                    }
                    2 -> {
                        resetCalibration()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    // Thiết lập sự kiện chuyển tab 4 chế độ: Tất cả, Ngang, Dọc và Tròn.
    private fun setupTabButtons() {
        viewBinding.tabModeAll.setOnClickListener {
            updateModeUI(LevelMode.ALL)
            view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
        viewBinding.tabModeHorizontal.setOnClickListener {
            updateModeUI(LevelMode.HORIZONTAL)
            view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
        viewBinding.tabModeVertical.setOnClickListener {
            updateModeUI(LevelMode.VERTICAL)
            view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
        viewBinding.tabModeCircular.setOnClickListener {
            updateModeUI(LevelMode.CIRCULAR)
            view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    // Thiết lập sự kiện cho hai nút hành động: Toàn màn hình và Giữ kết quả.
    private fun setupActionButtons() {
        viewBinding.btnFullscreenCard.setOnClickListener {
            toggleFullscreen()
            view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }

        viewBinding.btnHoldCard.setOnClickListener {
            toggleHoldResult()
            view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
    }

    // Cập nhật giao diện tab được chọn và hiển thị container bố cục tương ứng.
    private fun updateModeUI(mode: LevelMode) {
        currentMode = mode

        val colorActiveStroke = Color.parseColor("#84CC16")
        val colorActiveBg = Color.parseColor("#F7FEE7")
        val colorActiveTint = Color.parseColor("#4D7C0F")

        val colorInactiveStroke = ContextCompat.getColor(requireContext(), R.color.colorBorder)
        val colorInactiveBg = ContextCompat.getColor(requireContext(), R.color.colorCardBackground)
        val colorInactiveTint = ContextCompat.getColor(requireContext(), R.color.textSecondary)

        // Cập nhật phong cách Tab Tất cả
        val isAll = (mode == LevelMode.ALL)
        viewBinding.tabModeAll.strokeColor = if (isAll) colorActiveStroke else colorInactiveStroke
        viewBinding.tabModeAll.strokeWidth = if (isAll) dpToPx(2) else dpToPx(1)
        viewBinding.tabModeAll.setCardBackgroundColor(if (isAll) colorActiveBg else colorInactiveBg)
        viewBinding.ivTabAll.imageTintList = ColorStateList.valueOf(if (isAll) colorActiveTint else colorInactiveTint)
        viewBinding.tvTabAll.setTextColor(if (isAll) colorActiveTint else colorInactiveTint)

        // Cập nhật phong cách Tab Ngang
        val isHorizontal = (mode == LevelMode.HORIZONTAL)
        viewBinding.tabModeHorizontal.strokeColor = if (isHorizontal) colorActiveStroke else colorInactiveStroke
        viewBinding.tabModeHorizontal.strokeWidth = if (isHorizontal) dpToPx(2) else dpToPx(1)
        viewBinding.tabModeHorizontal.setCardBackgroundColor(if (isHorizontal) colorActiveBg else colorInactiveBg)
        viewBinding.ivTabHorizontal.imageTintList = ColorStateList.valueOf(if (isHorizontal) colorActiveTint else colorInactiveTint)
        viewBinding.tvTabHorizontal.setTextColor(if (isHorizontal) colorActiveTint else colorInactiveTint)

        // Cập nhật phong cách Tab Dọc
        val isVertical = (mode == LevelMode.VERTICAL)
        viewBinding.tabModeVertical.strokeColor = if (isVertical) colorActiveStroke else colorInactiveStroke
        viewBinding.tabModeVertical.strokeWidth = if (isVertical) dpToPx(2) else dpToPx(1)
        viewBinding.tabModeVertical.setCardBackgroundColor(if (isVertical) colorActiveBg else colorInactiveBg)
        viewBinding.ivTabVertical.imageTintList = ColorStateList.valueOf(if (isVertical) colorActiveTint else colorInactiveTint)
        viewBinding.tvTabVertical.setTextColor(if (isVertical) colorActiveTint else colorInactiveTint)

        // Cập nhật phong cách Tab Tròn
        val isCircular = (mode == LevelMode.CIRCULAR)
        viewBinding.tabModeCircular.strokeColor = if (isCircular) colorActiveStroke else colorInactiveStroke
        viewBinding.tabModeCircular.strokeWidth = if (isCircular) dpToPx(2) else dpToPx(1)
        viewBinding.tabModeCircular.setCardBackgroundColor(if (isCircular) colorActiveBg else colorInactiveBg)
        viewBinding.ivTabCircular.imageTintList = ColorStateList.valueOf(if (isCircular) colorActiveTint else colorInactiveTint)
        viewBinding.tvTabCircular.setTextColor(if (isCircular) colorActiveTint else colorInactiveTint)

        // Ẩn hiện các container chức năng
        viewBinding.containerModeAll.isVisible = isAll
        viewBinding.containerModeHorizontal.isVisible = isHorizontal
        viewBinding.containerModeVertical.isVisible = isVertical
        viewBinding.containerModeCircular.isVisible = isCircular

        // Cập nhật số liệu tức thời cho chế độ vừa chọn
        renderReadings(filteredPitch, filteredRoll)
    }

    // Bật tắt tính năng giữ kết quả góc đo (khóa / mở khóa bọt nước và số đo).
    private fun toggleHoldResult() {
        isHeld = !isHeld
        if (isHeld) {
            viewBinding.btnHoldCard.setCardBackgroundColor(Color.parseColor("#DCFCE7"))
            viewBinding.btnHoldCard.strokeColor = Color.parseColor("#16A34A")
            viewBinding.btnHoldCard.strokeWidth = dpToPx(2)
            viewBinding.ivHoldIcon.setImageResource(R.drawable.ic_lock)
            viewBinding.ivHoldIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#157335"))
            viewBinding.tvHoldLabel.text = getString(R.string.level_holding_result)
            viewBinding.tvHoldLabel.setTextColor(Color.parseColor("#157335"))
        } else {
            val normalBg = ContextCompat.getColor(requireContext(), R.color.colorCardBackground)
            val normalBorder = ContextCompat.getColor(requireContext(), R.color.colorBorder)
            val normalText = ContextCompat.getColor(requireContext(), R.color.textSecondary)

            viewBinding.btnHoldCard.setCardBackgroundColor(normalBg)
            viewBinding.btnHoldCard.strokeColor = normalBorder
            viewBinding.btnHoldCard.strokeWidth = dpToPx(1)
            viewBinding.ivHoldIcon.setImageResource(R.drawable.ic_lock_open)
            viewBinding.ivHoldIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.textPrimary))
            viewBinding.tvHoldLabel.text = getString(R.string.level_hold_result)
            viewBinding.tvHoldLabel.setTextColor(normalText)
        }
    }

    // Bật tắt chế độ toàn màn hình để biến thiết bị thành thước phẳng chuyên dụng.
    private fun toggleFullscreen() {
        if (isFullscreen) {
            exitFullscreen()
        } else {
            enterFullscreen()
        }
    }

    // Chuyển màn hình sang chế độ toàn diện (ẩn thanh tiêu đề và hệ thống).
    private fun enterFullscreen() {
        isFullscreen = true
        viewBinding.toolbarLayout.root.visibility = View.GONE
        viewBinding.tabModeContainer.visibility = View.GONE
        viewBinding.ivFullscreenIcon.setImageResource(R.drawable.ic_fullscreen_exit)
        viewBinding.tvFullscreenLabel.text = getString(R.string.level_fullscreen_exit)

        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    // Thoát chế độ toàn màn hình và khôi phục giao diện điều khiển chuẩn.
    private fun exitFullscreen() {
        isFullscreen = false
        viewBinding.toolbarLayout.root.visibility = View.VISIBLE
        viewBinding.tabModeContainer.visibility = View.VISIBLE
        viewBinding.ivFullscreenIcon.setImageResource(R.drawable.ic_fullscreen)
        viewBinding.tvFullscreenLabel.text = getString(R.string.level_fullscreen)

        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, true)
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Lưu độ nghiêng hiện tại làm mốc 0 thăng bằng chuẩn.
    private fun calibrateZero() {
        pitchOffset = filteredPitch + pitchOffset
        rollOffset = filteredRoll + rollOffset
        prefs.edit().putFloat("pitch_offset", pitchOffset).putFloat("roll_offset", rollOffset).apply()
        view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        Toast.makeText(requireContext(), R.string.level_calibrated_success, Toast.LENGTH_SHORT).show()
    }

    // Khôi phục mốc 0 thăng bằng chuẩn về giá trị mặc định của cảm biến.
    private fun resetCalibration() {
        pitchOffset = 0f
        rollOffset = 0f
        prefs.edit().remove("pitch_offset").remove("roll_offset").apply()
        view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        Toast.makeText(requireContext(), R.string.level_reset_success, Toast.LENGTH_SHORT).show()
    }

    // Xử lý sự kiện thay đổi dữ liệu gia tốc kế, lọc nhiễu và cập nhật ống thủy.
    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        if (isHeld) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Tính góc nghiêng pitch (trục Y) và roll (trục X)
        val rawPitch = Math.toDegrees(atan2(-y.toDouble(), sqrt((x * x + z * z).toDouble()))).toFloat()
        val rawRoll = Math.toDegrees(atan2(x.toDouble(), z.toDouble())).toFloat()

        val currentPitch = rawPitch - pitchOffset
        val currentRoll = rawRoll - rollOffset

        // Bộ lọc thông thấp (low-pass filter) khử rung nhẹ
        val alpha = 0.82f
        filteredPitch = alpha * filteredPitch + (1 - alpha) * currentPitch
        filteredRoll = alpha * filteredRoll + (1 - alpha) * currentRoll

        renderReadings(filteredPitch, filteredRoll)

        // Phản hồi rung haptic khi các trục đạt mức thăng bằng chuẩn
        val isBalanced = abs(filteredPitch) < 0.6f && abs(filteredRoll) < 0.6f
        if (isBalanced && !wasBalanced) {
            view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
        if (!isBalanced && abs(filteredPitch) > 1.0f || abs(filteredRoll) > 1.0f) {
            wasBalanced = false
        } else if (isBalanced) {
            wasBalanced = true
        }
    }

    // Hiển thị các giá trị góc đo và điều chỉnh vị trí bọt khí trên giao diện tương ứng.
    private fun renderReadings(pitch: Float, roll: Float) {
        val strRoll = formatDegrees(roll)
        val strPitch = formatDegrees(pitch)

        when (currentMode) {
            LevelMode.ALL -> {
                viewBinding.horizontalVialAll.setAngle(roll)
                viewBinding.verticalVialAll.setAngle(pitch)
                viewBinding.bubbleLevelViewAll.setAngles(pitch, roll)
                viewBinding.tvAngleXAll.text = strRoll
                viewBinding.tvAngleYAll.text = strPitch
            }
            LevelMode.HORIZONTAL -> {
                viewBinding.horizontalVialSingle.setAngle(roll)
                viewBinding.tvAngleHorizontalSingle.text = strRoll
            }
            LevelMode.VERTICAL -> {
                viewBinding.verticalVialSingle.setAngle(pitch)
                viewBinding.tvAngleVerticalSingle.text = strPitch
            }
            LevelMode.CIRCULAR -> {
                viewBinding.bubbleLevelViewSingle.setAngles(pitch, roll)
                viewBinding.tvAngleXCircular.text = getString(R.string.level_axis_x_format, strRoll)
                viewBinding.tvAngleYCircular.text = getString(R.string.level_axis_y_format, strPitch)
            }
        }
    }

    // Định dạng số đo góc ra chuỗi với dấu phẩy thập phân và ký hiệu độ chuẩn.
    private fun formatDegrees(angle: Float): String {
        return String.format(Locale.US, "%.1f°", abs(angle)).replace('.', ',')
    }

    // Chuyển đổi đơn vị dp sang px cho các thông số vẽ và stroke.
    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    // Xử lý sự kiện thay đổi độ chính xác của cảm biến gia tốc kế.
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
