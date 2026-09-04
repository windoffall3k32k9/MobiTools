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


abstract class BaseActivity<V : ViewBinding> : AppCompatActivity() {

    lateinit var viewBinding: V
        private set

    /** Alias ngắn để các màn hình hiện tại có thể refactor mà không đổi logic. */
    protected val binding: V
        get() = viewBinding

    val handler = Handler(Looper.getMainLooper())

    var loadingDialog: Dialog? = null

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

    open fun onBack() {
        finish()
    }

    open fun handleOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBack()
            }
        })
    }

    fun replaceFragment(containerId: Int, fragment: Fragment) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            addToBackStack(fragment::class.java.simpleName)
            replace(containerId, fragment, fragment::class.java.simpleName)
        }
    }

    fun addFragment(containerId: Int, fragment: Fragment) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            addToBackStack(fragment::class.java.simpleName)
            add(containerId, fragment, fragment::class.java.simpleName)
        }
    }

    fun hideFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            hide(fragment)
        }
    }

    fun showFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            show(fragment)
        }
    }

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

    fun showLoading() {
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            val dialog = loadingDialog ?: createLoadingDialog().also {
                loadingDialog = it
            }
            if (!dialog.isShowing) dialog.show()
        }
    }

    fun hideLoading() {
        runOnUiThread {
            loadingDialog?.dismiss()
        }
    }

    fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        runOnUiThread {
            Toast.makeText(this, message, duration).show()
        }
    }

    fun setStatusBarColor(color: Int, lightStatus: Boolean) {
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = color
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = lightStatus
        }
    }
}
