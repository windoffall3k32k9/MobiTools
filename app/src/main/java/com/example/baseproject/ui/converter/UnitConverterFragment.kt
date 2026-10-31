package com.example.baseproject.ui.converter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.baseproject.R
import com.example.baseproject.base.BaseToolFragment
import com.example.baseproject.databinding.FragmentUnitConverterBinding
import java.text.DecimalFormat

/**
 * Định nghĩa một đơn vị đo lường cùng hệ số quy đổi về đơn vị cơ sở.
 */
data class UnitItem(
    val label: String,
    val factorToBase: Double
)

/**
 * Công cụ chuyển đổi đơn vị đo lường hỗ trợ chiều dài, khối lượng, nhiệt độ và dung lượng.
 */
class UnitConverterFragment : BaseToolFragment<FragmentUnitConverterBinding>() {

    override val toolId: String = "unit_converter"
    override val toolTitleResId: Int = R.string.tool_unit_converter_title

    private val lengthUnits = listOf(
        UnitItem("Mét (m)", 1.0),
        UnitItem("Kilômét (km)", 1000.0),
        UnitItem("Centimét (cm)", 0.01),
        UnitItem("Milimét (mm)", 0.001),
        UnitItem("Inch (in)", 0.0254),
        UnitItem("Feet (ft)", 0.3048),
        UnitItem("Yard (yd)", 0.9144),
        UnitItem("Dặm (mile)", 1609.344)
    )

    private val weightUnits = listOf(
        UnitItem("Kilôgam (kg)", 1.0),
        UnitItem("Gam (g)", 0.001),
        UnitItem("Miligam (mg)", 0.000001),
        UnitItem("Pound (lb)", 0.45359237),
        UnitItem("Ounce (oz)", 0.0283495231),
        UnitItem("Tấn (t)", 1000.0)
    )

    private val dataUnits = listOf(
        UnitItem("Byte (B)", 1.0),
        UnitItem("Kilobyte (KB)", 1024.0),
        UnitItem("Megabyte (MB)", 1048576.0),
        UnitItem("Gigabyte (GB)", 1073741824.0),
        UnitItem("Terabyte (TB)", 1099511627776.0)
    )

    private val tempUnits = listOf("Độ C (°C)", "Độ F (°F)", "Kelvin (K)")

    private var currentMode = 0 // 0: Length, 1: Weight, 2: Temp, 3: Data
    private val decimalFormat = DecimalFormat("#,###.######")

    override fun provideViewBinding(container: ViewGroup?): FragmentUnitConverterBinding {
        return FragmentUnitConverterBinding.inflate(layoutInflater, container, false)
    }

    override fun initViews() {
        setupCategoryToggle()
        loadCategoryUnits()
        setupListeners()
    }

    // Thiết lập thanh chọn nhóm đơn vị đo lường.
    private fun setupCategoryToggle() {
        viewBinding.toggleCategoryGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                currentMode = when (checkedId) {
                    R.id.btnCatWeight -> 1
                    R.id.btnCatTemp -> 2
                    R.id.btnCatData -> 3
                    else -> 0
                }
                loadCategoryUnits()
                performConversion()
            }
        }
    }

    // Nạp danh sách đơn vị vào Spinner theo nhóm được chọn.
    private fun loadCategoryUnits() {
        val unitNames = when (currentMode) {
            0 -> lengthUnits.map { it.label }
            1 -> weightUnits.map { it.label }
            2 -> tempUnits
            else -> dataUnits.map { it.label }
        }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, unitNames)
        viewBinding.spinnerFromUnit.adapter = adapter
        viewBinding.spinnerToUnit.adapter = adapter

        if (unitNames.size > 1) {
            viewBinding.spinnerFromUnit.setSelection(0)
            viewBinding.spinnerToUnit.setSelection(1)
        }
    }

    // Gắn sự kiện thay đổi dữ liệu nhập và các nút hoán đổi/sao chép.
    private fun setupListeners() {
        viewBinding.etFromValue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                performConversion()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        val itemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                performConversion()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        viewBinding.spinnerFromUnit.onItemSelectedListener = itemSelectedListener
        viewBinding.spinnerToUnit.onItemSelectedListener = itemSelectedListener

        viewBinding.btnSwapUnits.setOnClickListener {
            val fromPos = viewBinding.spinnerFromUnit.selectedItemPosition
            val toPos = viewBinding.spinnerToUnit.selectedItemPosition
            viewBinding.spinnerFromUnit.setSelection(toPos)
            viewBinding.spinnerToUnit.setSelection(fromPos)
            performConversion()
        }

        viewBinding.btnCopyResult.setOnClickListener {
            val res = viewBinding.tvToResult.text.toString()
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Unit Result", res))
            Toast.makeText(requireContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        }
    }

    // Thực thi quy đổi đơn vị dựa trên giá trị nhập và cặp đơn vị hiện hành.
    private fun performConversion() {
        val inputStr = viewBinding.etFromValue.text?.toString()?.trim() ?: ""
        if (inputStr.isEmpty()) {
            viewBinding.tvToResult.text = "0"
            return
        }

        val value = inputStr.toDoubleOrNull()
        if (value == null) {
            viewBinding.tvToResult.text = "Giá trị không hợp lệ"
            return
        }

        val fromIndex = viewBinding.spinnerFromUnit.selectedItemPosition
        val toIndex = viewBinding.spinnerToUnit.selectedItemPosition

        if (currentMode == 2) {
            val converted = convertTemperature(value, fromIndex, toIndex)
            viewBinding.tvToResult.text = decimalFormat.format(converted)
            return
        }

        val unitList = when (currentMode) {
            0 -> lengthUnits
            1 -> weightUnits
            else -> dataUnits
        }

        if (fromIndex in unitList.indices && toIndex in unitList.indices) {
            val baseValue = value * unitList[fromIndex].factorToBase
            val converted = baseValue / unitList[toIndex].factorToBase
            viewBinding.tvToResult.text = decimalFormat.format(converted)
        }
    }

    // Chuyển đổi nhiệt độ giữa Độ C, Độ F và Kelvin.
    private fun convertTemperature(value: Double, from: Int, to: Int): Double {
        if (from == to) return value
        val celsius = when (from) {
            0 -> value
            1 -> (value - 32.0) * 5.0 / 9.0
            2 -> value - 273.15
            else -> value
        }
        return when (to) {
            0 -> celsius
            1 -> (celsius * 9.0 / 5.0) + 32.0
            2 -> celsius + 273.15
            else -> celsius
        }
    }
}
