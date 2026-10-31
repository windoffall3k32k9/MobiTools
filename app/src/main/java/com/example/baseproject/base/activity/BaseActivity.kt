package com.example.baseproject.base.activity

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.viewbinding.ViewBinding
import com.example.baseproject.localization.AppLanguageManager

/**
 * Activity cơ sở quản lý vòng đời, đa ngôn ngữ, thanh điều hướng và hiển thị hộp thoại tải.
 */
abstract class BaseActivity<V : ViewBinding> : AppCompatActivity() {

    lateinit var viewBinding: V
        private set

    /** Alias ngắn để các màn hình hiện tại có thể truy cập ViewBinding trực tiếp. */
    protected val binding: V
        get() = viewBinding

    val handler = Handler(Looper.getMainLooper())

    var loadingDialog: Dialog? = null

    // Bọc context của Activity với Locale ngôn ngữ đang được kích hoạt.
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(AppLanguageManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = provideViewBinding()
        setContentView(viewBinding.root)

        initViews()
        initData()
        initObserver()
        handleOnBackPressed()
    }

    abstract fun provideViewBinding(): V

    open fun initViews() = Unit

    open fun initData() = Unit

    open fun initObserver() = Unit

    // Xử lý quay lại: nếu có Fragment trong backstack thì pop, ngược lại đóng Activity.
    open fun onBack() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            finish()
        }
    }

    // Đăng ký callback xử lý phím quay lại hệ thống.
    open fun handleOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBack()
            }
        })
    }

    // Thay thế Fragment trong khung chứa và thêm vào ngăn xếp điều hướng (backstack).
    fun replaceFragment(containerId: Int, fragment: Fragment) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            addToBackStack(fragment::class.java.simpleName)
            replace(containerId, fragment, fragment::class.java.simpleName)
        }
    }

    // Thêm Fragment mới vào khung chứa và lưu vào ngăn xếp điều hướng.
    fun addFragment(containerId: Int, fragment: Fragment) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            addToBackStack(fragment::class.java.simpleName)
            add(containerId, fragment, fragment::class.java.simpleName)
        }
    }

    // Ẩn tạm thời một Fragment đang hiển thị.
    fun hideFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            hide(fragment)
        }
    }

    // Hiển thị lại Fragment đã bị ẩn.
    fun showFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            show(fragment)
        }
    }

    // Khởi tạo hộp thoại loading xoay tròn mặc định.
    open fun createLoadingDialog(): Dialog {
        val progressBar = ProgressBar(this)
        val size = (64 * resources.displayMetrics.density).toInt()
        val container = FrameLayout(this).apply {
            setPadding(size / 2, size / 2, size / 2, size / 2)
            addView(
                progressBar,
                FrameLayout.LayoutParams(size, size, Gravity.CENTER)
            )
        }

        return AlertDialog.Builder(this)
            .setView(container)
            .setCancelable(true)
            .create()
    }

    // Hiển thị hộp thoại loading trên luồng giao diện chính.
    fun showLoading() {
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            val dialog = loadingDialog ?: createLoadingDialog().also {
                loadingDialog = it
            }
            if (!dialog.isShowing) dialog.show()
        }
    }

    // Đóng hộp thoại loading đang hiển thị.
    fun hideLoading() {
        runOnUiThread {
            loadingDialog?.dismiss()
        }
    }

    // Hiển thị thông báo Toast nhanh gọn trên giao diện.
    fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        runOnUiThread {
            Toast.makeText(this, message, duration).show()
        }
    }

    // Cấu hình màu nền và kiểu icon cho thanh trạng thái hệ thống.
    fun setStatusBarColor(color: Int, lightStatus: Boolean) {
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = color
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = lightStatus
        }
    }
}
