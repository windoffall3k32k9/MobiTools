package com.example.baseproject.ui.calculator

import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.baseproject.R
import com.example.baseproject.base.fragment.BaseFragment
import com.example.baseproject.databinding.FragmentCalculatorHistoryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Màn hình trang riêng biệt hiển thị lịch sử tính toán được phân tách chi tiết theo từng ngày.
 */
class CalculatorHistoryFragment : BaseFragment<FragmentCalculatorHistoryBinding>() {

    private lateinit var historyAdapter: CalculatorHistoryAdapter

    // Nạp layout view binding cho trang Lịch sử tính toán.
    override fun provideViewBinding(container: ViewGroup?): FragmentCalculatorHistoryBinding {
        return FragmentCalculatorHistoryBinding.inflate(layoutInflater, container, false)
    }

    // Khởi tạo các thành phần giao diện, adapter và nạp danh sách lịch sử.
    override fun initViews() {
        setupTopBar()
        setupRecyclerView()
        loadAndDisplayHistory()
    }

    // Cấu hình sự kiện nút quay lại và nút xóa toàn bộ lịch sử trên thanh tiêu đề.
    private fun setupTopBar() {
        viewBinding.btnHistoryBack.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            parentFragmentManager.popBackStack()
        }

        viewBinding.btnHistoryClear.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            confirmClearHistory()
        }
    }

    // Khởi tạo RecyclerView và gắn Adapter với sự kiện chọn bản ghi để nạp lại vào máy tính.
    private fun setupRecyclerView() {
        historyAdapter = CalculatorHistoryAdapter { selectedRecord ->
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY_HISTORY_SELECT,
                bundleOf(
                    EXTRA_EXPRESSION to selectedRecord.expression,
                    EXTRA_RESULT to selectedRecord.result
                )
            )
            parentFragmentManager.popBackStack()
        }

        viewBinding.rvHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
    }

    // Đọc lịch sử tính toán từ bộ nhớ, nhóm theo từng ngày và cập nhật hiển thị.
    private fun loadAndDisplayHistory() {
        val context = requireContext()
        val records = CalculatorHistoryManager.getHistory(context)

        if (records.isEmpty()) {
            viewBinding.rvHistory.visibility = View.GONE
            viewBinding.layoutHistoryEmpty.visibility = View.VISIBLE
            viewBinding.btnHistoryClear.visibility = View.GONE
            return
        }

        viewBinding.rvHistory.visibility = View.VISIBLE
        viewBinding.layoutHistoryEmpty.visibility = View.GONE
        viewBinding.btnHistoryClear.visibility = View.VISIBLE

        // Nhóm các phép tính theo tiêu đề ngày (Hôm nay, Hôm qua, dd/MM/yyyy)
        val rows = mutableListOf<CalculatorHistoryAdapter.HistoryRow>()
        val grouped = records.groupBy { CalculatorHistoryManager.formatDateHeader(context, it.timestamp) }

        for ((dateHeader, items) in grouped) {
            rows.add(CalculatorHistoryAdapter.HistoryRow.Header(dateHeader))
            for (item in items) {
                rows.add(CalculatorHistoryAdapter.HistoryRow.Record(item))
            }
        }

        historyAdapter.submitList(rows)
    }

    // Hiển thị hộp thoại xác nhận trước khi xóa sạch dữ liệu lịch sử tính toán.
    private fun confirmClearHistory() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.calc_clear_history)
            .setMessage(R.string.calc_clear_history_confirm)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                CalculatorHistoryManager.clearHistory(requireContext())
                loadAndDisplayHistory()
                Toast.makeText(requireContext(), R.string.calc_clear_history, Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object {
        const val REQUEST_KEY_HISTORY_SELECT = "request_key_calculator_history_select"
        const val EXTRA_EXPRESSION = "extra_expression"
        const val EXTRA_RESULT = "extra_result"

        // Khởi tạo một phiên bản mới của CalculatorHistoryFragment.
        fun newInstance(): CalculatorHistoryFragment {
            return CalculatorHistoryFragment()
        }
    }
}
