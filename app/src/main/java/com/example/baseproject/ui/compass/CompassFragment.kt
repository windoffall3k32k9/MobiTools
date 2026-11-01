package com.example.baseproject.ui.compass

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.baseproject.R
import com.example.baseproject.base.BaseToolFragment
import com.example.baseproject.databinding.FragmentCompassBinding
import com.example.baseproject.utils.AppPermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Công cụ la bàn điện tử xoay toàn bộ mặt số chi tiết 360 độ và hiển thị vị trí thiết bị.
 */
class CompassFragment : BaseToolFragment<FragmentCompassBinding>(), SensorEventListener, LocationListener {

    override val toolId: String = "compass"
    override val toolTitleResId: Int = R.string.tool_compass_title

    private var sensorManager: SensorManager? = null
    private var rotationVectorSensor: Sensor? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var hasAccelerometer = false
    private var hasMagnetometer = false
    private var currentFilteredAzimuth = 0f

    private var locationManager: LocationManager? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        checkAndRequestLocation()
    }

    // Khởi tạo ViewBinding liên kết với giao diện la bàn số.
    override fun provideViewBinding(container: ViewGroup?): FragmentCompassBinding {
        return FragmentCompassBinding.inflate(layoutInflater, container, false)
    }

    // Khởi tạo cảm biến định hướng, bộ định vị GPS và sự kiện cấp quyền.
    override fun initViews() {
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (rotationVectorSensor == null && (accelerometer == null || magnetometer == null)) {
            viewBinding.tvCompassSensorWarning.visibility = View.VISIBLE
        }

        viewBinding.btnGrantLocationPermission.setOnClickListener {
            permissionLauncher.launch(AppPermissionManager.getAllRequiredPermissions())
        }

        checkAndRequestLocation()
    }

    override fun onResume() {
        super.onResume()
        rotationVectorSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        } ?: run {
            accelerometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            magnetometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        }

        checkAndRequestLocation()
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
        locationManager?.removeUpdates(this)
    }

    // Nhận dữ liệu cảm biến và tính toán góc phương vị chuẩn (azimuth 0..359 độ).
    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        var rawAzimuth: Float? = null

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            var azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            if (azimuth < 0) azimuth += 360f
            rawAzimuth = azimuth
        } else {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
                hasAccelerometer = true
            } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
                hasMagnetometer = true
            }

            if (hasAccelerometer && hasMagnetometer) {
                val success = SensorManager.getRotationMatrix(
                    rotationMatrix,
                    null,
                    accelerometerReading,
                    magnetometerReading
                )
                if (success) {
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    var azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                    if (azimuth < 0) azimuth += 360f
                    rawAzimuth = azimuth
                }
            }
        }

        rawAzimuth?.let { updateCompassDial(it) }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    // Cập nhật góc quay của toàn bộ mặt số la bàn và nội dung hiển thị ở tâm.
    private fun updateCompassDial(targetAzimuth: Float) {
        // Bộ lọc làm mịn góc xoay tránh nhảy giật
        val diff = targetAzimuth - currentFilteredAzimuth
        val normalizedDiff = when {
            diff > 180f -> diff - 360f
            diff < -180f -> diff + 360f
            else -> diff
        }
        currentFilteredAzimuth = (currentFilteredAzimuth + normalizedDiff * 0.25f + 360f) % 360f

        val roundedDegree = currentFilteredAzimuth.roundToInt() % 360
        viewBinding.tvDirectionFullName.text = getDirectionFullName(roundedDegree)
        viewBinding.tvAzimuthDegree.text = "$roundedDegree°"

        // Xoay toàn bộ mặt số la bàn ngược chiều góc phương vị
        viewBinding.compassDialView.rotation = -currentFilteredAzimuth
    }

    // Lấy tên đầy đủ của hướng địa lý bằng tiếng Việt (không viết tắt).
    private fun getDirectionFullName(degree: Int): String {
        return when (degree) {
            in 0..22, in 338..360 -> "Bắc"
            in 23..67 -> "Đông Bắc"
            in 68..112 -> "Đông"
            in 113..157 -> "Đông Nam"
            in 158..202 -> "Nam"
            in 203..247 -> "Tây Nam"
            in 248..292 -> "Tây"
            in 293..337 -> "Tây Bắc"
            else -> "Bắc"
        }
    }

    // Kiểm tra quyền vị trí và yêu cầu cập nhật vị trí thiết bị từ GPS/Network.
    private fun checkAndRequestLocation() {
        val context = context ?: return
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            viewBinding.btnGrantLocationPermission.visibility = View.VISIBLE
            viewBinding.tvCoordinates.text = "Chưa cấp quyền vị trí"
            viewBinding.tvLocationAddress.text = "Nhấn nút bên dưới để cấp quyền định vị hiển thị tọa độ"
            return
        }

        viewBinding.btnGrantLocationPermission.visibility = View.GONE

        val lm = locationManager ?: return
        val lastGps = runCatching { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull()
        val lastNet = runCatching { lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }.getOrNull()
        val bestLast = lastGps ?: lastNet

        bestLast?.let { onLocationChanged(it) }

        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            runCatching {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 5f, this)
            }
        }
        if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            runCatching {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 5f, this)
            }
        }
    }

    // Nhận thông tin tọa độ mới và hiển thị định dạng độ phút giây kèm địa phương.
    override fun onLocationChanged(location: Location) {
        val latDms = formatToDms(location.latitude, isLatitude = true)
        val lonDms = formatToDms(location.longitude, isLatitude = false)
        viewBinding.tvCoordinates.text = "$latDms  $lonDms"

        // Phân giải địa chỉ ngoại tuyến / trực tuyến từ Geocoder
        resolveAddress(location.latitude, location.longitude)
    }

    override fun onProviderEnabled(provider: String) = Unit
    override fun onProviderDisabled(provider: String) = Unit
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

    // Chuyển đổi tọa độ thập phân sang định dạng Độ, Phút, Giây (DMS) trực quan.
    private fun formatToDms(coordinate: Double, isLatitude: Boolean): String {
        val positive = coordinate >= 0
        val absCoord = abs(coordinate)
        val degrees = absCoord.toInt()
        val minutes = ((absCoord - degrees) * 60).toInt()
        val seconds = ((((absCoord - degrees) * 60) - minutes) * 60).roundToInt()

        val hemisphere = if (isLatitude) {
            if (positive) "B" else "N"
        } else {
            if (positive) "Đ" else "T"
        }

        return String.format(Locale.US, "%d°%02d'%02d\" %s", degrees, minutes, seconds, hemisphere)
    }

    // Tra cứu tên phường xã thành phố từ tọa độ bằng Geocoder trên luồng nền IO.
    private fun resolveAddress(latitude: Double, longitude: Double) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    var resultList: List<android.location.Address>? = null
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        resultList = addresses
                    }
                    resultList
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)
                }

                val address = addresses?.firstOrNull()
                val displayAddress = address?.let {
                    it.getAddressLine(0) ?: run {
                        val parts = listOfNotNull(
                            it.featureName,
                            it.thoroughfare,
                            it.subLocality,
                            it.subAdminArea,
                            it.adminArea,
                            it.countryName
                        )
                        parts.distinct().joinToString(", ")
                    }
                } ?: "Đang xác định vị trí…"

                withContext(Dispatchers.Main) {
                    viewBinding.tvLocationAddress.text = displayAddress
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (viewBinding.tvLocationAddress.text.isNullOrEmpty() || viewBinding.tvLocationAddress.text == "Đang xác định vị trí…") {
                        viewBinding.tvLocationAddress.text = "Vị trí GPS đã sẵn sàng"
                    }
                }
            }
        }
    }
}
