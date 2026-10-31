package com.example.baseproject.ui.settings

import android.view.ViewGroup
import com.example.baseproject.base.fragment.BaseFragment
import com.example.baseproject.databinding.FragmentGuideBinding

/**
 * Màn hình Hướng dẫn sử dụng chi tiết toàn bộ các tính năng và mẹo điều hướng trong MobiTools.
 */
class GuideFragment : BaseFragment<FragmentGuideBinding>() {

    // Khởi tạo ViewBinding liên kết với giao diện hướng dẫn sử dụng.
    override fun provideViewBinding(container: ViewGroup?): FragmentGuideBinding {
        return FragmentGuideBinding.inflate(layoutInflater, container, false)
    }

    // Thiết lập sự kiện nút quay lại thanh tiêu đề để pop màn hình khỏi ngăn xếp.
    override fun initViews() {
        viewBinding.btnToolbarBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}
