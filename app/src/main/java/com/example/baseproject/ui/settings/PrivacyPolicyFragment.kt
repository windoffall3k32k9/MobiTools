package com.example.baseproject.ui.settings

import android.view.ViewGroup
import com.example.baseproject.base.fragment.BaseFragment
import com.example.baseproject.databinding.FragmentPrivacyPolicyBinding

/**
 * Màn hình Chính sách bảo mật tuyên ngôn nguyên tắc hoạt động offline và an toàn dữ liệu.
 */
class PrivacyPolicyFragment : BaseFragment<FragmentPrivacyPolicyBinding>() {

    // Khởi tạo ViewBinding liên kết với layout chính sách bảo mật.
    override fun provideViewBinding(container: ViewGroup?): FragmentPrivacyPolicyBinding {
        return FragmentPrivacyPolicyBinding.inflate(layoutInflater, container, false)
    }

    // Thiết lập sự kiện nút quay lại thanh tiêu đề để quay về màn hình trước.
    override fun initViews() {
        viewBinding.btnToolbarBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}
