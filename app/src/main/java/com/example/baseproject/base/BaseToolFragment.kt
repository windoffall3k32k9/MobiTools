package com.example.baseproject.base

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.example.baseproject.R
import com.example.baseproject.base.fragment.BaseFragment
import com.example.baseproject.data.database.AppDatabase
import com.example.baseproject.data.database.dao.ToolDao
import kotlinx.coroutines.launch

/**
 * Lớp cơ sở cho toàn bộ các màn hình công cụ trong MobiTools.
 * Tự động ghi nhận lịch sử mở công cụ và đồng bộ trạng thái yêu thích.
 */
abstract class BaseToolFragment<V : ViewBinding> : BaseFragment<V>() {

    protected val toolDao: ToolDao by lazy {
        AppDatabase.getInstance(requireContext()).toolDao()
    }

    abstract val toolId: String

    @get:StringRes
    abstract val toolTitleResId: Int

    protected var isFavorite: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        recordToolUsage()
        observeFavoriteState()
    }

    // Thiết lập thanh tiêu đề chuẩn của công cụ gồm nút quay lại, tiêu đề và nút yêu thích.
    private fun setupToolbar() {
        val btnBack = view?.findViewById<ImageView>(R.id.btnToolbarBack)
        val tvTitle = view?.findViewById<TextView>(R.id.tvToolbarTitle)
        val btnFavorite = view?.findViewById<ImageView>(R.id.btnToolbarFavorite)

        btnBack?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        tvTitle?.setText(toolTitleResId)

        btnFavorite?.setOnClickListener {
            toggleFavorite()
        }
    }

    // Ghi nhận thời điểm sử dụng công cụ vào cơ sở dữ liệu Room.
    private fun recordToolUsage() {
        lifecycleScope.launch {
            try {
                toolDao.recordUsage(toolId, System.currentTimeMillis())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Lắng nghe và cập nhật biểu tượng ngôi sao yêu thích từ cơ sở dữ liệu.
    private fun observeFavoriteState() {
        val btnFavorite = view?.findViewById<ImageView>(R.id.btnToolbarFavorite) ?: return
        lifecycleScope.launch {
            try {
                val entity = toolDao.getById(toolId)
                isFavorite = entity?.isFavorite == true
                updateFavoriteIcon(btnFavorite)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Đảo trạng thái yêu thích của công cụ và lưu vào Room.
    private fun toggleFavorite() {
        val btnFavorite = view?.findViewById<ImageView>(R.id.btnToolbarFavorite) ?: return
        lifecycleScope.launch {
            try {
                isFavorite = !isFavorite
                toolDao.toggleFavorite(toolId, isFavorite)
                updateFavoriteIcon(btnFavorite)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Cập nhật giao diện nút yêu thích theo trạng thái hiện hành.
    private fun updateFavoriteIcon(imageView: ImageView) {
        if (isFavorite) {
            imageView.setImageResource(R.drawable.ic_star_filled)
        } else {
            imageView.setImageResource(R.drawable.ic_star_outline)
        }
    }
}
