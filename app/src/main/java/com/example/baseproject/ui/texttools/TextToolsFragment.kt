package com.example.baseproject.ui.texttools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.Toast
import com.example.baseproject.R
import com.example.baseproject.base.BaseToolFragment
import com.example.baseproject.databinding.FragmentTextToolsBinding
import java.util.Locale

/**
 * Công cụ xử lý và định dạng văn bản ngoại tuyến hỗ trợ thống kê ký tự và chuyển đổi hàng loạt.
 */
class TextToolsFragment : BaseToolFragment<FragmentTextToolsBinding>() {

    override val toolId: String = "text_tools"
    override val toolTitleResId: Int = R.string.tool_text_tools_title

    override fun provideViewBinding(container: ViewGroup?): FragmentTextToolsBinding {
        return FragmentTextToolsBinding.inflate(layoutInflater, container, false)
    }

    override fun initViews() {
        setupTextWatcher()
        setupTransformationButtons()
        setupActionButtons()
        updateStatistics("")
    }

    // Lắng nghe thay đổi nội dung văn bản để cập nhật số lượng ký tự, từ và dòng.
    private fun setupTextWatcher() {
        viewBinding.etContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateStatistics(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    // Thiết lập các nút biến đổi cấu trúc và định dạng văn bản.
    private fun setupTransformationButtons() {
        viewBinding.btnUppercase.setOnClickListener {
            applyTransformation { it.uppercase(Locale.getDefault()) }
        }

        viewBinding.btnLowercase.setOnClickListener {
            applyTransformation { it.lowercase(Locale.getDefault()) }
        }

        viewBinding.btnCapitalize.setOnClickListener {
            applyTransformation { text ->
                text.split(" ").joinToString(" ") { word ->
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                }
            }
        }

        viewBinding.btnTrimSpaces.setOnClickListener {
            applyTransformation { text ->
                text.lines().joinToString("\n") { line ->
                    line.trim().replace("\\s+".toRegex(), " ")
                }
            }
        }

        viewBinding.btnRemoveEmptyLines.setOnClickListener {
            applyTransformation { text ->
                text.lines().filter { it.isNotBlank() }.joinToString("\n")
            }
        }

        viewBinding.btnRemoveDuplicates.setOnClickListener {
            applyTransformation { text ->
                text.lines().distinct().joinToString("\n")
            }
        }

        viewBinding.btnSortAz.setOnClickListener {
            applyTransformation { text ->
                text.lines().sorted().joinToString("\n")
            }
        }

        viewBinding.btnReverse.setOnClickListener {
            applyTransformation { it.reversed() }
        }
    }

    // Thiết lập các nút sao chép, chia sẻ và xóa trắng nội dung.
    private fun setupActionButtons() {
        viewBinding.btnClearAll.setOnClickListener {
            viewBinding.etContent.text?.clear()
        }

        viewBinding.btnCopyText.setOnClickListener {
            val text = viewBinding.etContent.text?.toString() ?: ""
            if (text.isEmpty()) return@setOnClickListener
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Processed Text", text))
            Toast.makeText(requireContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        }

        viewBinding.btnShareText.setOnClickListener {
            val text = viewBinding.etContent.text?.toString() ?: ""
            if (text.isEmpty()) return@setOnClickListener
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(shareIntent, "Chia sẻ văn bản qua"))
        }
    }

    // Thực thi hàm biến đổi và đưa văn bản kết quả vào ô nhập liệu.
    private inline fun applyTransformation(transform: (String) -> String) {
        val current = viewBinding.etContent.text?.toString() ?: ""
        if (current.isBlank()) return
        val result = transform(current)
        viewBinding.etContent.setText(result)
        viewBinding.etContent.setSelection(result.length)
    }

    // Cập nhật các chỉ số thống kê về ký tự, từ và dòng.
    private fun updateStatistics(text: String) {
        val chars = text.length
        val words = if (text.isBlank()) 0 else text.trim().split("\\s+".toRegex()).count { it.isNotEmpty() }
        val lines = if (text.isEmpty()) 0 else text.lines().size

        viewBinding.tvCharCount.text = "$chars ký tự"
        viewBinding.tvWordCount.text = "$words từ"
        viewBinding.tvLineCount.text = "$lines dòng"
    }
}
