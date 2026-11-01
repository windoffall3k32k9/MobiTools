package com.example.baseproject

import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import com.example.baseproject.base.activity.BaseActivity
import com.example.baseproject.databinding.ActivityMainBinding
import com.example.baseproject.ui.home.HomeFragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity chính của MobiTools đóng vai trò điều phối các Fragment công cụ.
 */
@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private var lastBackPressTime: Long = 0L

    // Khởi tạo đối tượng ViewBinding liên kết với layout chính.
    override fun provideViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    // Thiết lập giao diện tràn viền và khởi chạy màn hình chính ban đầu.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.fragmentContainer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.fragment_container, HomeFragment())
            }
        }
    }

    // Xử lý phím quay lại: đóng menu popup nếu mở, quay lại màn hình trước đó, hoặc ấn 2 lần để thoát.
    override fun handleOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (currentFragment is HomeFragment && currentFragment.isDrawerOpen()) {
                    currentFragment.closeDrawer()
                    return
                }

                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
                    val now = System.currentTimeMillis()
                    if (now - lastBackPressTime < BACK_PRESS_EXIT_THRESHOLD_MS) {
                        finish()
                    } else {
                        lastBackPressTime = now
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.back_press_again_to_exit),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }

    companion object {
        private const val BACK_PRESS_EXIT_THRESHOLD_MS = 2000L
    }
}
