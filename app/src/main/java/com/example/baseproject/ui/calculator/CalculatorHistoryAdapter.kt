package com.example.baseproject.ui.calculator

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.baseproject.databinding.ItemCalculatorHistoryBinding
import com.example.baseproject.databinding.ItemCalculatorHistoryHeaderBinding

/**
 * Adapter hiển thị danh sách lịch sử tính toán được gom nhóm theo từng ngày.
 */
class CalculatorHistoryAdapter(
    private val onItemClick: (CalculationRecord) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    sealed class HistoryRow {
        data class Header(val dateTitle: String) : HistoryRow()
        data class Record(val data: CalculationRecord) : HistoryRow()
    }

    private val rows = mutableListOf<HistoryRow>()

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_RECORD = 1
    }

    // Cập nhật danh sách hàng hiển thị (tiêu đề ngày và các phép tính).
    fun submitList(newRows: List<HistoryRow>) {
        rows.clear()
        rows.addAll(newRows)
        notifyDataSetChanged()
    }

    // Xác định kiểu hiển thị giữa tiêu đề ngày và bản ghi phép tính.
    override fun getItemViewType(position: Int): Int {
        return when (rows[position]) {
            is HistoryRow.Header -> TYPE_HEADER
            is HistoryRow.Record -> TYPE_RECORD
        }
    }

    // Khởi tạo ViewHolder tương ứng với kiểu hiển thị.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            val binding = ItemCalculatorHistoryHeaderBinding.inflate(inflater, parent, false)
            HeaderViewHolder(binding)
        } else {
            val binding = ItemCalculatorHistoryBinding.inflate(inflater, parent, false)
            RecordViewHolder(binding)
        }
    }

    // Gắn dữ liệu vào từng ô hiển thị theo vị trí.
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is HistoryRow.Header -> (holder as HeaderViewHolder).bind(row.dateTitle)
            is HistoryRow.Record -> (holder as RecordViewHolder).bind(row.data, onItemClick)
        }
    }

    // Trả về tổng số mục trong danh sách hiển thị.
    override fun getItemCount(): Int = rows.size

    // ViewHolder hiển thị dòng tiêu đề ngày phân cách nhóm.
    class HeaderViewHolder(
        private val binding: ItemCalculatorHistoryHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String) {
            binding.tvHeaderDate.text = title
        }
    }

    // ViewHolder hiển thị từng bản ghi phép tính và kết quả.
    class RecordViewHolder(
        private val binding: ItemCalculatorHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(record: CalculationRecord, onClick: (CalculationRecord) -> Unit) {
            binding.tvHistoryExpression.text = record.expression
            binding.tvHistoryResult.text = "= ${record.result}"
            binding.tvHistoryTime.text = CalculatorHistoryManager.formatTime(record.timestamp)

            binding.root.setOnClickListener {
                onClick(record)
            }
        }
    }
}
