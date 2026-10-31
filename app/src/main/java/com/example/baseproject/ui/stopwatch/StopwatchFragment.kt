package com.example.baseproject.ui.stopwatch

import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.baseproject.R
import com.example.baseproject.base.BaseToolFragment
import com.example.baseproject.databinding.FragmentStopwatchBinding
import com.example.baseproject.databinding.ItemLapTimeBinding
import java.util.Locale

/**
 * Mục dữ liệu ghi nhận một vòng bấm giờ.
 */
data class LapItem(
    val lapIndex: Int,
    val lapTimeMs: Long,
    val totalTimeMs: Long
)

/**
 * Công cụ đồng hồ bấm giờ chính xác cao với tính năng ghi vòng (lap time).
 */
class StopwatchFragment : BaseToolFragment<FragmentStopwatchBinding>() {

    override val toolId: String = "stopwatch"
    override val toolTitleResId: Int = R.string.tool_stopwatch_title

    private var isRunning = false
    private var baseTime = 0L
    private var accumulatedTime = 0L
    private var lastLapTime = 0L

    private val laps = mutableListOf<LapItem>()
    private lateinit var lapAdapter: LapAdapter

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                val currentTotal = accumulatedTime + (SystemClock.elapsedRealtime() - baseTime)
                viewBinding.tvStopwatchTime.text = formatTime(currentTotal)
                handler.postDelayed(this, 30)
            }
        }
    }

    override fun provideViewBinding(container: ViewGroup?): FragmentStopwatchBinding {
        return FragmentStopwatchBinding.inflate(layoutInflater, container, false)
    }

    override fun initViews() {
        lapAdapter = LapAdapter(laps)
        viewBinding.rvLaps.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = lapAdapter
        }

        setupButtons()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateRunnable)
    }

    // Thiết lập hành vi cho các nút điều khiển bấm giờ.
    private fun setupButtons() {
        viewBinding.btnPrimary.setOnClickListener {
            if (!isRunning) {
                startStopwatch()
            } else {
                pauseStopwatch()
            }
        }

        viewBinding.btnSecondary.setOnClickListener {
            if (isRunning) {
                recordLap()
            } else {
                resetStopwatch()
            }
        }
    }

    // Bắt đầu đếm thời gian.
    private fun startStopwatch() {
        isRunning = true
        baseTime = SystemClock.elapsedRealtime()
        handler.post(updateRunnable)

        viewBinding.btnPrimary.setText(R.string.pause)
        viewBinding.btnPrimary.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.colorWarning)
        viewBinding.btnSecondary.setText(R.string.lap)
        viewBinding.btnSecondary.isEnabled = true
    }

    // Tạm dừng đếm thời gian.
    private fun pauseStopwatch() {
        isRunning = false
        accumulatedTime += SystemClock.elapsedRealtime() - baseTime
        handler.removeCallbacks(updateRunnable)

        viewBinding.btnPrimary.setText(R.string.resume)
        viewBinding.btnPrimary.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.colorPrimary)
        viewBinding.btnSecondary.setText(R.string.reset)
        viewBinding.btnSecondary.isEnabled = true
    }

    // Đặt lại toàn bộ dữ liệu bấm giờ về 0.
    private fun resetStopwatch() {
        isRunning = false
        accumulatedTime = 0L
        lastLapTime = 0L
        handler.removeCallbacks(updateRunnable)

        viewBinding.tvStopwatchTime.text = "00:00:00.00"
        viewBinding.btnPrimary.setText(R.string.start)
        viewBinding.btnPrimary.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.colorPrimary)
        viewBinding.btnSecondary.setText(R.string.reset)
        viewBinding.btnSecondary.isEnabled = false

        val count = laps.size
        laps.clear()
        lapAdapter.notifyItemRangeRemoved(0, count)
    }

    // Ghi nhận một mốc vòng (lap) vào danh sách hiển thị.
    private fun recordLap() {
        val currentTotal = accumulatedTime + (SystemClock.elapsedRealtime() - baseTime)
        val lapDuration = currentTotal - lastLapTime
        lastLapTime = currentTotal

        val lapIndex = laps.size + 1
        laps.add(0, LapItem(lapIndex, lapDuration, currentTotal))
        lapAdapter.notifyItemInserted(0)
        viewBinding.rvLaps.scrollToPosition(0)
    }

    // Định dạng số mili-giây thành chuỗi chuẩn Giờ:Phút:Giây.Phần trăm.
    private fun formatTime(ms: Long): String {
        val hundredths = (ms / 10) % 100
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        val hours = (ms / (1000 * 60 * 60))
        return String.format(Locale.US, "%02d:%02d:%02d.%02d", hours, minutes, seconds, hundredths)
    }
}

/**
 * Adapter hiển thị danh sách các vòng bấm giờ.
 */
class LapAdapter(private val items: List<LapItem>) : RecyclerView.Adapter<LapAdapter.LapViewHolder>() {

    inner class LapViewHolder(val binding: ItemLapTimeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LapViewHolder {
        val binding = ItemLapTimeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LapViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: LapViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvLapIndex.text = "Vòng #${item.lapIndex}"
        holder.binding.tvLapTime.text = formatLapTime(item.lapTimeMs)
        holder.binding.tvTotalTime.text = formatLapTime(item.totalTimeMs)
    }

    // Định dạng chuỗi thời gian cho từng mục trong danh sách vòng.
    private fun formatLapTime(ms: Long): String {
        val hundredths = (ms / 10) % 100
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        val hours = (ms / (1000 * 60 * 60))
        return String.format(Locale.US, "%02d:%02d:%02d.%02d", hours, minutes, seconds, hundredths)
    }
}
