package com.example.baseproject.ui.soundmeter

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.baseproject.R
import com.example.baseproject.base.BaseToolFragment
import com.example.baseproject.databinding.FragmentSoundMeterBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Công cụ đo độ ồn âm thanh tương đối qua micro theo giá trị dBFS quy đổi.
 */
class SoundMeterFragment : BaseToolFragment<FragmentSoundMeterBinding>() {

    override val toolId: String = "sound_meter"
    override val toolTitleResId: Int = R.string.tool_sound_meter_title

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var isRecording = false

    private var minDb = 999.0
    private var maxDb = 0.0
    private var totalDb = 0.0
    private var sampleCount = 0

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewBinding.layoutPermissionRequest.visibility = View.GONE
            startRecording()
        } else {
            viewBinding.layoutPermissionRequest.visibility = View.VISIBLE
        }
    }

    override fun provideViewBinding(container: ViewGroup?): FragmentSoundMeterBinding {
        return FragmentSoundMeterBinding.inflate(layoutInflater, container, false)
    }

    override fun initViews() {
        viewBinding.btnGrantAudioPermission.setOnClickListener {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionAndStart()
    }

    override fun onPause() {
        super.onPause()
        stopRecording()
    }

    // Kiểm tra quyền truy cập microphone trước khi khởi chạy thu âm.
    private fun checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            viewBinding.layoutPermissionRequest.visibility = View.GONE
            startRecording()
        } else {
            viewBinding.layoutPermissionRequest.visibility = View.VISIBLE
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Khởi tạo tiến trình lấy mẫu âm thanh từ phần cứng micro.
    private fun startRecording() {
        if (isRecording) return

        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        if (bufferSize <= 0) return

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                return
            }

            audioRecord?.startRecording()
            isRecording = true

            recordingJob = lifecycleScope.launch(Dispatchers.Default) {
                val buffer = ShortArray(bufferSize)
                while (isActive && isRecording) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        var sum = 0.0
                        for (i in 0 until read) {
                            sum += buffer[i].toDouble() * buffer[i].toDouble()
                        }
                        val rms = sqrt(sum / read)
                        // Quy đổi tương đối sang thang decibel
                        val db = if (rms > 1.0) (20 * log10(rms)).coerceIn(10.0, 100.0) else 10.0

                        withContext(Dispatchers.Main) {
                            updateAudioUi(db)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Dừng thu âm và giải phóng tài nguyên micro.
    private fun stopRecording() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioRecord = null
    }

    // Cập nhật giá trị đo, thanh đo và các chỉ số thống kê lên giao diện.
    private fun updateAudioUi(db: Double) {
        if (db < minDb) minDb = db
        if (db > maxDb) maxDb = db
        totalDb += db
        sampleCount++
        val avgDb = totalDb / sampleCount

        viewBinding.tvCurrentDb.text = String.format(Locale.US, "%.1f", db)
        viewBinding.progressDb.progress = db.toInt()
        viewBinding.tvMinDb.text = String.format(Locale.US, "%.1f", minDb)
        viewBinding.tvMaxDb.text = String.format(Locale.US, "%.1f", maxDb)
        viewBinding.tvAvgDb.text = String.format(Locale.US, "%.1f", avgDb)

        val (desc, colorRes) = when {
            db < 40.0 -> "Yên tĩnh" to R.color.colorSuccess
            db < 60.0 -> "Bình thường" to R.color.colorPrimary
            db < 75.0 -> "Hơi ồn" to R.color.colorWarning
            else -> "Rất ồn" to R.color.colorDanger
        }

        viewBinding.tvNoiseLevelDescription.text = desc
        viewBinding.tvNoiseLevelDescription.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
    }
}
