package com.example.baseproject.ui.converter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import com.example.baseproject.R
import com.example.baseproject.base.BaseToolFragment
import com.example.baseproject.databinding.FragmentUnitConverterBinding
import com.example.baseproject.ui.calculator.CalculatorFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale
import kotlin.math.abs

/**
 * Trung tâm Chuyển đổi đa năng 12 trong 1: Ngoại tệ, Độ dài, Trọng lượng, Diện tích,
 * Thời gian, Dữ liệu, Giảm giá, Thể tích, Hệ thống số, Tốc độ, Nhiệt độ và BMI.
 */
class UnitConverterFragment : BaseToolFragment<FragmentUnitConverterBinding>() {

    override val toolId: String = "unit_converter"
    override val toolTitleResId: Int = R.string.tool_unit_converter_title

    private var isShowingGrid: Boolean = true
    private var activeCategory: ConverterCategory = ConverterCategory.CURRENCY

    // Trạng thái cho nhóm chuyển đổi đơn vị chuẩn / tiền tệ / nhiệt độ
    private var currentUnits: List<UnitOption> = ConverterEngine.currencyUnits
    private var fromUnitIndex: Int = 0
    private var toUnitIndex: Int = 1

    // Trạng thái cho Hệ thống số (cơ số mặc định: 10 - DEC)
    private var currentRadix: Int = 10

    // Bộ nhớ lưu tỷ giá tùy chỉnh của người dùng
    private val prefs by lazy {
        requireContext().getSharedPreferences("mobitools_converter_prefs", Context.MODE_PRIVATE)
    }

    private val quickChipViews = mutableMapOf<ConverterCategory, TextView>()

    // Khởi tạo đối tượng ViewBinding cho giao diện chuyển đổi đa năng.
    override fun provideViewBinding(container: ViewGroup?): FragmentUnitConverterBinding {
        return FragmentUnitConverterBinding.inflate(layoutInflater, container, false)
    }

    // Thiết lập toàn bộ thành phần giao diện, sự kiện điều hướng và các bộ tính toán.
    override fun initViews() {
        setupTopBarAndBackNavigation()
        setupCategoryGridTiles()
        buildQuickCategoryChips()
        setupUnitConverterWorkspace()
        setupDiscountWorkspace()
        setupNumeralWorkspace()
        setupBmiWorkspace()
        showCategoryGrid()
    }

    // Thiết lập thanh tiêu đề, chuyển tab Máy tính/Chuyển đổi và xử lý nút Back thông minh.
    private fun setupTopBarAndBackNavigation() {
        viewBinding.btnConverterBack.setOnClickListener {
            vibrateLight()
            handleBackNavigation()
        }

        viewBinding.btnShowCategoryGrid.setOnClickListener {
            vibrateLight()
            showCategoryGrid()
        }

        viewBinding.tvHeaderTabCalculator.setOnClickListener {
            vibrateLight()
            parentFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.fragment_container, CalculatorFragment())
            }
        }

        viewBinding.tvHeaderTabConverter.setOnClickListener {
            if (!isShowingGrid) {
                vibrateLight()
                showCategoryGrid()
            }
        }

        viewBinding.btnResetActiveTool.setOnClickListener {
            vibrateLight()
            resetCurrentCategoryDefaults()
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!isShowingGrid) {
                        showCategoryGrid()
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    // Xử lý điều hướng quay lại: nếu đang ở chi tiết công cụ thì trở về lưới 12 danh mục trước.
    private fun handleBackNavigation() {
        if (!isShowingGrid) {
            showCategoryGrid()
        } else {
            parentFragmentManager.popBackStack()
        }
    }

    // Gắn sự kiện nhấn cho 12 ô chức năng trên lưới 3x4.
    private fun setupCategoryGridTiles() {
        val b = viewBinding
        val tileMap = mapOf(
            b.tileCatCurrency to ConverterCategory.CURRENCY,
            b.tileCatLength to ConverterCategory.LENGTH,
            b.tileCatWeight to ConverterCategory.WEIGHT,
            b.tileCatArea to ConverterCategory.AREA,
            b.tileCatTime to ConverterCategory.TIME,
            b.tileCatData to ConverterCategory.DATA,
            b.tileCatDiscount to ConverterCategory.DISCOUNT,
            b.tileCatVolume to ConverterCategory.VOLUME,
            b.tileCatNumeral to ConverterCategory.NUMERAL_SYSTEM,
            b.tileCatSpeed to ConverterCategory.SPEED,
            b.tileCatTemp to ConverterCategory.TEMPERATURE,
            b.tileCatBmi to ConverterCategory.BMI
        )

        for ((tileView, category) in tileMap) {
            tileView.setOnClickListener {
                vibrateLight()
                openCategoryDetail(category)
            }
        }
    }

    // Khởi tạo thanh chip ngang gồm 12 danh mục để chuyển đổi nhanh trong màn hình chi tiết.
    private fun buildQuickCategoryChips() {
        val container = viewBinding.containerQuickCategoryChips
        container.removeAllViews()
        quickChipViews.clear()

        val density = resources.displayMetrics.density
        for (category in ConverterCategory.entries) {
            val chip = TextView(requireContext()).apply {
                text = getString(category.titleResId)
                textSize = 13f
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(
                    (14 * density).toInt(),
                    (7 * density).toInt(),
                    (14 * density).toInt(),
                    (7 * density).toInt()
                )
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (36 * density).toInt()
                ).apply {
                    marginEnd = (8 * density).toInt()
                }
                layoutParams = params
                setOnClickListener {
                    vibrateLight()
                    if (activeCategory != category) {
                        openCategoryDetail(category)
                    }
                }
            }
            quickChipViews[category] = chip
            container.addView(chip)
        }
    }

    // Cập nhật trạng thái màu sắc nổi bật cho chip danh mục đang hoạt động.
    private fun updateQuickChipSelection() {
        val density = resources.displayMetrics.density
        val orangeColor = ContextCompat.getColor(requireContext(), R.color.calcOrange)
        val whiteColor = ContextCompat.getColor(requireContext(), R.color.white)
        val secondaryText = ContextCompat.getColor(requireContext(), R.color.textSecondary)
        val surfaceBg = ContextCompat.getColor(requireContext(), R.color.calcButtonBg)

        for ((category, chip) in quickChipViews) {
            val isSelected = category == activeCategory
            val bg = GradientDrawable().apply {
                cornerRadius = 18f * density
                setColor(if (isSelected) orangeColor else surfaceBg)
            }
            chip.background = bg
            chip.setTextColor(if (isSelected) whiteColor else secondaryText)

            if (isSelected) {
                viewBinding.scrollQuickCategories.post {
                    val scrollX = (chip.left - (viewBinding.scrollQuickCategories.width / 2) + (chip.width / 2))
                        .coerceAtLeast(0)
                    viewBinding.scrollQuickCategories.smoothScrollTo(scrollX, 0)
                }
            }
        }
    }

    // Hiển thị màn hình lưới 12 công cụ chuyển đổi (3x4).
    private fun showCategoryGrid() {
        isShowingGrid = true
        viewBinding.layoutConverterGridContainer.visibility = View.VISIBLE
        viewBinding.layoutConverterDetailContainer.visibility = View.GONE
        viewBinding.btnShowCategoryGrid.visibility = View.INVISIBLE
    }

    // Mở màn hình làm việc chi tiết tương ứng với danh mục được người dùng chọn.
    private fun openCategoryDetail(category: ConverterCategory) {
        activeCategory = category
        isShowingGrid = false

        viewBinding.layoutConverterGridContainer.visibility = View.GONE
        viewBinding.layoutConverterDetailContainer.visibility = View.VISIBLE
        viewBinding.btnShowCategoryGrid.visibility = View.VISIBLE

        viewBinding.ivActiveCategoryIcon.setImageResource(category.iconResId)
        viewBinding.tvActiveCategoryTitle.setText(category.titleResId)
        viewBinding.tvActiveCategorySubtitle.text = category.subtitle

        updateQuickChipSelection()

        val isDiscount = category == ConverterCategory.DISCOUNT
        val isNumeral = category == ConverterCategory.NUMERAL_SYSTEM
        val isBmi = category == ConverterCategory.BMI
        val isStandardUnit = !isDiscount && !isNumeral && !isBmi

        viewBinding.workspaceUnitConverter.isVisible = isStandardUnit
        viewBinding.workspaceDiscountConverter.isVisible = isDiscount
        viewBinding.workspaceNumeralConverter.isVisible = isNumeral
        viewBinding.workspaceBmiConverter.isVisible = isBmi

        when {
            isStandardUnit -> loadStandardCategory(category)
            isDiscount -> evaluateDiscount()
            isNumeral -> {
                updateNumeralRadixUI()
                evaluateNumeralSystem()
            }
            isBmi -> evaluateBmi()
        }
    }

    // Khôi phục giá trị mặc định cho công cụ đang mở.
    private fun resetCurrentCategoryDefaults() {
        when (activeCategory) {
            ConverterCategory.DISCOUNT -> {
                viewBinding.etDiscountOriginalPrice.setText("500000")
                viewBinding.etDiscountPercent.setText("20")
                viewBinding.etDiscountExtraPercent.setText("0")
                evaluateDiscount()
            }
            ConverterCategory.NUMERAL_SYSTEM -> {
                currentRadix = 10
                updateNumeralRadixUI()
                viewBinding.etNumeralInput.setText("255")
                evaluateNumeralSystem()
            }
            ConverterCategory.BMI -> {
                viewBinding.etBmiHeight.setText("168")
                viewBinding.etBmiWeight.setText("62")
                evaluateBmi()
            }
            else -> {
                fromUnitIndex = 0
                toUnitIndex = if (currentUnits.size > 1) 1 else 0
                val defaultVal = if (activeCategory == ConverterCategory.TEMPERATURE) "25" else "1"
                viewBinding.etFromValue.setText(defaultVal)
                viewBinding.etFromValue.setSelection(defaultVal.length)
                updateSelectedUnitLabels()
                performUnitConversion()
            }
        }
    }

    // =========================================================================
    // 1. NHÓM CHUYỂN ĐỔI ĐƠN VỊ CHUẨN / NGOẠI TỆ / NHIỆT ĐỘ (9 DANH MỤC)
    // =========================================================================

    // Nạp danh sách đơn vị và giá trị khởi tạo cho danh mục đo lường được chọn.
    private fun loadStandardCategory(category: ConverterCategory) {
        currentUnits = ConverterEngine.getUnitsForCategory(category)
        fromUnitIndex = 0
        toUnitIndex = if (currentUnits.size > 1) 1 else 0

        viewBinding.btnCustomizeRate.isVisible = (category == ConverterCategory.CURRENCY)

        val currentInput = viewBinding.etFromValue.text?.toString().orEmpty()
        if (currentInput.isBlank()) {
            val defaultVal = if (category == ConverterCategory.TEMPERATURE) "25" else "1"
            viewBinding.etFromValue.setText(defaultVal)
        }

        updateSelectedUnitLabels()
        performUnitConversion()
    }

    // Thiết lập các sự kiện tương tác cho khối chuyển đổi đơn vị chuẩn.
    private fun setupUnitConverterWorkspace() {
        val b = viewBinding

        b.btnSelectFromUnit.setOnClickListener {
            vibrateLight()
            showUnitPickerDialog(isFromUnit = true)
        }

        b.btnSelectToUnit.setOnClickListener {
            vibrateLight()
            showUnitPickerDialog(isFromUnit = false)
        }

        b.btnSwapUnits.setOnClickListener {
            vibrateLight()
            val tmp = fromUnitIndex
            fromUnitIndex = toUnitIndex
            toUnitIndex = tmp
            updateSelectedUnitLabels()
            performUnitConversion()
        }

        b.btnClearFromValue.setOnClickListener {
            vibrateLight()
            b.etFromValue.text?.clear()
        }

        b.etFromValue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                performUnitConversion()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        val quickChips = mapOf(
            b.chipQuickVal1 to "1",
            b.chipQuickVal10 to "10",
            b.chipQuickVal100 to "100",
            b.chipQuickVal1000 to "1000"
        )
        for ((chip, valStr) in quickChips) {
            chip.setOnClickListener {
                vibrateLight()
                b.etFromValue.setText(valStr)
                b.etFromValue.setSelection(valStr.length)
            }
        }

        b.btnCustomizeRate.setOnClickListener {
            vibrateLight()
            showCustomCurrencyRateDialog()
        }

        b.btnCopyResult.setOnClickListener {
            vibrateLight()
            val toUnit = currentUnits.getOrNull(toUnitIndex)
            val textToCopy = if (toUnit != null) {
                "${b.tvToResult.text} ${toUnit.symbol}"
            } else {
                b.tvToResult.text.toString()
            }
            copyToClipboard("Converted Value", textToCopy)
        }
    }

    // Hiển thị hộp thoại chọn đơn vị nguồn hoặc đơn vị đích.
    private fun showUnitPickerDialog(isFromUnit: Boolean) {
        if (currentUnits.isEmpty()) return
        val labels = currentUnits.map { it.displayLabel() }.toTypedArray()
        val currentIdx = if (isFromUnit) fromUnitIndex else toUnitIndex
        val title = if (isFromUnit) getString(R.string.conv_from_unit) else getString(R.string.conv_to_unit)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setSingleChoiceItems(labels, currentIdx) { dialog, which ->
                if (isFromUnit) {
                    fromUnitIndex = which
                } else {
                    toUnitIndex = which
                }
                updateSelectedUnitLabels()
                performUnitConversion()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.common_cancel, null)
            .show()
    }

    // Cập nhật nhãn tên đơn vị và ký hiệu viết tắt trên thẻ nguồn và thẻ đích.
    private fun updateSelectedUnitLabels() {
        val fromUnit = currentUnits.getOrNull(fromUnitIndex) ?: return
        val toUnit = currentUnits.getOrNull(toUnitIndex) ?: return

        viewBinding.tvFromUnitName.text = fromUnit.displayLabel()
        viewBinding.tvFromUnitCodeBadge.text = fromUnit.symbol

        viewBinding.tvToUnitName.text = toUnit.displayLabel()
        viewBinding.tvToUnitCodeBadge.text = toUnit.symbol
    }

    // Thực hiện quy đổi giá trị giữa đơn vị nguồn, đơn vị đích và cập nhật bảng tham chiếu toàn bộ đơn vị.
    private fun performUnitConversion() {
        val fromUnit = currentUnits.getOrNull(fromUnitIndex) ?: return
        val toUnit = currentUnits.getOrNull(toUnitIndex) ?: return

        val customRate = getCustomCurrencyRate(fromUnit.code, toUnit.code)
        updateFormulaInfoText(fromUnit, toUnit, customRate)

        val rawStr = viewBinding.etFromValue.text?.toString()?.trim().orEmpty()
        if (rawStr.isEmpty()) {
            viewBinding.tvToResult.text = "0"
            viewBinding.tvUnitWarning.visibility = View.GONE
            renderAllUnitsReferenceList(0.0, fromUnit)
            return
        }

        val inputVal = ConverterEngine.parseNormalizedDouble(rawStr)
        if (inputVal == null) {
            viewBinding.tvToResult.text = getString(R.string.calc_error_syntax)
            viewBinding.tvUnitWarning.visibility = View.GONE
            return
        }

        // Kiểm tra cảnh báo vật lý cho nhiệt độ dưới độ 0 tuyệt đối hoặc giá trị âm không hợp lý
        if (activeCategory == ConverterCategory.TEMPERATURE &&
            ConverterEngine.isBelowAbsoluteZero(inputVal, fromUnit.code)
        ) {
            viewBinding.tvUnitWarning.visibility = View.VISIBLE
            viewBinding.tvUnitWarning.text = "Lưu ý: Giá trị thấp hơn độ không tuyệt đối (-273,15 °C / 0 K)"
        } else if (activeCategory != ConverterCategory.TEMPERATURE && inputVal < 0.0) {
            viewBinding.tvUnitWarning.visibility = View.VISIBLE
            viewBinding.tvUnitWarning.text = "Lưu ý: Bạn đang nhập giá trị âm cho đại lượng đo lường"
        } else {
            viewBinding.tvUnitWarning.visibility = View.GONE
        }

        val converted = if (activeCategory == ConverterCategory.TEMPERATURE) {
            ConverterEngine.convertTemperature(inputVal, fromUnit.code, toUnit.code)
        } else {
            ConverterEngine.convertLinear(inputVal, fromUnit, toUnit, customRate)
        }

        val isCurrency = activeCategory == ConverterCategory.CURRENCY
        viewBinding.tvToResult.text = ConverterEngine.formatNumber(converted, isCurrency)

        renderAllUnitsReferenceList(inputVal, fromUnit)
    }

    // Cập nhật dòng hiển thị công thức tỷ lệ quy đổi trực tiếp giữa 2 đơn vị đang chọn.
    private fun updateFormulaInfoText(
        fromUnit: UnitOption,
        toUnit: UnitOption,
        customRate: Double?
    ) {
        if (activeCategory == ConverterCategory.TEMPERATURE) {
            val oneConv = ConverterEngine.convertTemperature(0.0, fromUnit.code, toUnit.code)
            val formatted = ConverterEngine.formatNumber(oneConv)
            viewBinding.tvUnitFormulaInfo.text = "0 ${fromUnit.symbol} = $formatted ${toUnit.symbol}"
            return
        }

        val ratio = ConverterEngine.getUnitRatio(activeCategory, fromUnit, toUnit, customRate)
        val formattedRatio = ConverterEngine.formatNumber(ratio, activeCategory == ConverterCategory.CURRENCY)
        val customSuffix = if (customRate != null) " (Tỷ giá tùy chỉnh)" else ""
        viewBinding.tvUnitFormulaInfo.text = "1 ${fromUnit.symbol} = $formattedRatio ${toUnit.symbol}$customSuffix"
    }

    // Vẽ danh sách quy đổi đồng thời sang toàn bộ các đơn vị trong cùng danh mục.
    private fun renderAllUnitsReferenceList(inputVal: Double, fromUnit: UnitOption) {
        val container = viewBinding.containerAllUnitsRows
        container.removeAllViews()
        val density = resources.displayMetrics.density
        val isCurrency = activeCategory == ConverterCategory.CURRENCY

        currentUnits.forEachIndexed { index, targetUnit ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(
                    (14 * density).toInt(),
                    (10 * density).toInt(),
                    (14 * density).toInt(),
                    (10 * density).toInt()
                )
                isClickable = true
                isFocusable = true
                val outValue = android.util.TypedValue()
                requireContext().theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                setBackgroundResource(outValue.resourceId)
            }

            val tvName = TextView(requireContext()).apply {
                text = targetUnit.displayLabel()
                textSize = 13.5f
                setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        if (index == toUnitIndex) R.color.calcOrange else R.color.textSecondary
                    )
                )
                if (index == toUnitIndex) setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val customRate = getCustomCurrencyRate(fromUnit.code, targetUnit.code)
            val resultVal = if (activeCategory == ConverterCategory.TEMPERATURE) {
                ConverterEngine.convertTemperature(inputVal, fromUnit.code, targetUnit.code)
            } else {
                ConverterEngine.convertLinear(inputVal, fromUnit, targetUnit, customRate)
            }
            val formattedVal = ConverterEngine.formatNumber(resultVal, isCurrency)

            val tvVal = TextView(requireContext()).apply {
                text = "$formattedVal ${targetUnit.symbol}"
                textSize = 14.5f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        if (index == toUnitIndex) R.color.calcOrange else R.color.textPrimary
                    )
                )
            }

            row.setOnClickListener {
                vibrateLight()
                toUnitIndex = index
                updateSelectedUnitLabels()
                performUnitConversion()
                copyToClipboard("Unit Reference", "$formattedVal ${targetUnit.symbol}")
            }

            row.addView(tvName)
            row.addView(tvVal)
            container.addView(row)

            if (index < currentUnits.lastIndex) {
                val divider = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        (1 * density).toInt()
                    ).apply {
                        marginStart = (14 * density).toInt()
                        marginEnd = (14 * density).toInt()
                    }
                    setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorBorder))
                }
                container.addView(divider)
            }
        }
    }

    // Lấy tỷ giá tùy chỉnh đã lưu giữa cặp tiền tệ (nếu có).
    private fun getCustomCurrencyRate(fromCode: String, toCode: String): Double? {
        if (activeCategory != ConverterCategory.CURRENCY || fromCode == toCode) return null
        val key = "rate_${fromCode}_${toCode}"
        if (!prefs.contains(key)) return null
        val saved = prefs.getFloat(key, -1f).toDouble()
        return if (saved > 0.0) saved else null
    }

    // Hiển thị hộp thoại cho phép người dùng nhập tỷ giá ngoại tệ tùy chỉnh hoặc đặt về mặc định.
    private fun showCustomCurrencyRateDialog() {
        val fromUnit = currentUnits.getOrNull(fromUnitIndex) ?: return
        val toUnit = currentUnits.getOrNull(toUnitIndex) ?: return
        if (fromUnit.code == toUnit.code) {
            Toast.makeText(requireContext(), "Hai đơn vị tiền tệ đang trùng nhau", Toast.LENGTH_SHORT).show()
            return
        }

        val currentCustom = getCustomCurrencyRate(fromUnit.code, toUnit.code)
        val defaultRatio = ConverterEngine.getUnitRatio(ConverterCategory.CURRENCY, fromUnit, toUnit, null)
        val initialRate = currentCustom ?: defaultRatio

        val density = resources.displayMetrics.density
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (22 * density).toInt(),
                (12 * density).toInt(),
                (22 * density).toInt(),
                (4 * density).toInt()
            )
        }

        val etRate = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(ConverterEngine.formatNumber(initialRate, false).replace(".", "").replace(",", "."))
            hint = "Nhập tỷ giá cho 1 ${fromUnit.symbol} -> ${toUnit.symbol}"
            setSelection(text.length)
        }
        container.addView(etRate)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Tỷ giá: 1 ${fromUnit.symbol} = ? ${toUnit.symbol}")
            .setView(container)
            .setPositiveButton(R.string.common_save) { _, _ ->
                val newRate = ConverterEngine.parseNormalizedDouble(etRate.text?.toString())
                if (newRate != null && newRate > 0.0) {
                    prefs.edit()
                        .putFloat("rate_${fromUnit.code}_${toUnit.code}", newRate.toFloat())
                        .putFloat("rate_${toUnit.code}_${fromUnit.code}", (1.0 / newRate).toFloat())
                        .apply()
                    performUnitConversion()
                } else {
                    Toast.makeText(requireContext(), "Tỷ giá phải lớn hơn 0", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton(R.string.conv_reset_rate) { _, _ ->
                prefs.edit()
                    .remove("rate_${fromUnit.code}_${toUnit.code}")
                    .remove("rate_${toUnit.code}_${fromUnit.code}")
                    .apply()
                performUnitConversion()
            }
            .setNegativeButton(R.string.common_cancel, null)
            .show()
    }

    // =========================================================================
    // 2. CÔNG CỤ TÍNH GIẢM GIÁ (DISCOUNT CALCULATOR)
    // =========================================================================

    // Thiết lập sự kiện cho các ô nhập giá gốc, % giảm giá và các nút chọn nhanh.
    private fun setupDiscountWorkspace() {
        val b = viewBinding
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                evaluateDiscount()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        }

        b.etDiscountOriginalPrice.addTextChangedListener(watcher)
        b.etDiscountPercent.addTextChangedListener(watcher)
        b.etDiscountExtraPercent.addTextChangedListener(watcher)

        b.btnClearDiscountPrice.setOnClickListener {
            vibrateLight()
            b.etDiscountOriginalPrice.text?.clear()
        }

        val priceChips = mapOf(
            b.chipPrice100k to "100000",
            b.chipPrice250k to "250000",
            b.chipPrice500k to "500000",
            b.chipPrice1M to "1000000"
        )
        for ((chip, priceStr) in priceChips) {
            chip.setOnClickListener {
                vibrateLight()
                b.etDiscountOriginalPrice.setText(priceStr)
                b.etDiscountOriginalPrice.setSelection(priceStr.length)
            }
        }

        val discountChips = mapOf(
            b.chipDisc5 to "5",
            b.chipDisc10 to "10",
            b.chipDisc15 to "15",
            b.chipDisc20 to "20",
            b.chipDisc25 to "25",
            b.chipDisc30 to "30",
            b.chipDisc50 to "50",
            b.chipDisc70 to "70"
        )
        for ((chip, percentStr) in discountChips) {
            chip.setOnClickListener {
                vibrateLight()
                b.etDiscountPercent.setText(percentStr)
                b.etDiscountPercent.setSelection(percentStr.length)
            }
        }

        b.btnCopyDiscountResult.setOnClickListener {
            vibrateLight()
            copyToClipboard("Discount Price", b.tvDiscountFinalPrice.text.toString())
        }
    }

    // Đánh giá và cập nhật kết quả tính giảm giá theo thời gian thực.
    private fun evaluateDiscount() {
        val b = viewBinding
        val result = ConverterEngine.calculateDiscount(
            originalPriceInput = b.etDiscountOriginalPrice.text?.toString(),
            discountPercentInput = b.etDiscountPercent.text?.toString(),
            extraDiscountPercentInput = b.etDiscountExtraPercent.text?.toString()
        )

        if (!result.isValid) {
            b.tvDiscountFinalPrice.text = result.errorMessage ?: getString(R.string.calc_error_syntax)
            b.tvDiscountSavedAmount.text = "0"
            b.tvDiscountEffectiveBadge.text = "0%"
            return
        }

        b.tvDiscountFinalPrice.text = ConverterEngine.formatNumber(result.finalPrice, isCurrency = true)
        b.tvDiscountSavedAmount.text = ConverterEngine.formatNumber(result.savedAmount, isCurrency = true)
        b.tvDiscountEffectiveBadge.text = "-${ConverterEngine.formatOneDecimal(result.effectiveDiscountPercent)}%"
    }

    // =========================================================================
    // 3. CÔNG CỤ CHUYỂN ĐỔI HỆ THỐNG SỐ (DEC / HEX / OCT / BIN)
    // =========================================================================

    // Thiết lập sự kiện cho bộ chuyển đổi Hệ thống số và thanh phím phụ Hex/Bin.
    private fun setupNumeralWorkspace() {
        val b = viewBinding

        val radixTabs = mapOf(
            b.tabRadixDec to 10,
            b.tabRadixHex to 16,
            b.tabRadixOct to 8,
            b.tabRadixBin to 2
        )

        for ((tabView, newRadix) in radixTabs) {
            tabView.setOnClickListener {
                vibrateLight()
                if (currentRadix != newRadix) {
                    // Tự động chuyển đổi giá trị đang nhập sang cơ số mới nếu hợp lệ
                    val currentRes = ConverterEngine.convertNumeralSystem(
                        b.etNumeralInput.text?.toString(),
                        currentRadix
                    )
                    currentRadix = newRadix
                    updateNumeralRadixUI()
                    if (currentRes.isValid && !b.etNumeralInput.text.isNullOrBlank()) {
                        val convertedInput = when (newRadix) {
                            10 -> currentRes.decValue
                            16 -> currentRes.hexValue
                            8 -> currentRes.octValue
                            2 -> currentRes.binValue.replace(" ", "")
                            else -> ""
                        }
                        b.etNumeralInput.setText(convertedInput)
                        b.etNumeralInput.setSelection(convertedInput.length)
                    } else {
                        evaluateNumeralSystem()
                    }
                }
            }
        }

        b.btnClearNumeralInput.setOnClickListener {
            vibrateLight()
            b.etNumeralInput.text?.clear()
        }

        b.etNumeralInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                evaluateNumeralSystem()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        val quickKeys = mapOf(
            b.btnHexKey0 to "0",
            b.btnHexKey1 to "1",
            b.btnHexKeyA to "A",
            b.btnHexKeyB to "B",
            b.btnHexKeyC to "C",
            b.btnHexKeyD to "D",
            b.btnHexKeyE to "E",
            b.btnHexKeyF to "F"
        )
        for ((keyView, charStr) in quickKeys) {
            keyView.setOnClickListener {
                vibrateLight()
                b.etNumeralInput.append(charStr)
            }
        }

        b.btnCopyDec.setOnClickListener {
            vibrateLight()
            copyToClipboard("DEC", b.tvNumeralDecResult.text.toString())
        }
        b.btnCopyHex.setOnClickListener {
            vibrateLight()
            copyToClipboard("HEX", b.tvNumeralHexResult.text.toString())
        }
        b.btnCopyOct.setOnClickListener {
            vibrateLight()
            copyToClipboard("OCT", b.tvNumeralOctResult.text.toString())
        }
        b.btnCopyBin.setOnClickListener {
            vibrateLight()
            copyToClipboard("BIN", b.tvNumeralBinResult.text.toString().replace(" ", ""))
        }
    }

    // Cập nhật giao diện tab cơ số đang chọn và bật/tắt các phím chữ cái A-F tương ứng.
    private fun updateNumeralRadixUI() {
        val b = viewBinding
        val orangeColor = ContextCompat.getColor(requireContext(), R.color.calcOrange)
        val orangeLight = ContextCompat.getColor(requireContext(), R.color.calcOrangeLight)
        val secondaryColor = ContextCompat.getColor(requireContext(), R.color.textSecondary)
        val normalBg = ContextCompat.getColor(requireContext(), R.color.calcButtonBg)
        val density = resources.displayMetrics.density

        val tabs = listOf(
            b.tabRadixDec to 10,
            b.tabRadixHex to 16,
            b.tabRadixOct to 8,
            b.tabRadixBin to 2
        )

        for ((tab, radix) in tabs) {
            val active = (radix == currentRadix)
            val drawable = GradientDrawable().apply {
                cornerRadius = 12f * density
                setColor(if (active) orangeLight else normalBg)
                if (active) setStroke((1.5f * density).toInt(), orangeColor)
            }
            tab.background = drawable
            tab.setTextColor(if (active) orangeColor else secondaryColor)
        }

        val hexButtons = listOf(b.btnHexKeyA, b.btnHexKeyB, b.btnHexKeyC, b.btnHexKeyD, b.btnHexKeyE, b.btnHexKeyF)
        val enableHex = (currentRadix == 16)
        for (btn in hexButtons) {
            btn.isEnabled = enableHex
            btn.alpha = if (enableHex) 1.0f else 0.35f
        }
    }

    // Thực thi chuyển đổi hệ thống số và hiển thị kết quả đồng thời cho DEC, HEX, OCT, BIN.
    private fun evaluateNumeralSystem() {
        val b = viewBinding
        val raw = b.etNumeralInput.text?.toString().orEmpty()
        val result = ConverterEngine.convertNumeralSystem(raw, currentRadix)

        if (!result.isValid) {
            b.tvNumeralError.visibility = View.VISIBLE
            b.tvNumeralError.text = result.errorMessage
            return
        }

        b.tvNumeralError.visibility = View.GONE
        b.tvNumeralDecResult.text = result.decValue
        b.tvNumeralHexResult.text = result.hexValue
        b.tvNumeralOctResult.text = result.octValue
        b.tvNumeralBinResult.text = result.binValue
    }

    // =========================================================================
    // 4. CÔNG CỤ TÍNH CHỈ SỐ KHỐI CƠ THỂ (BMI)
    // =========================================================================

    // Thiết lập sự kiện cho bộ tính toán BMI gồm nút tăng giảm và chip chọn nhanh chiều cao, cân nặng.
    private fun setupBmiWorkspace() {
        val b = viewBinding
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                evaluateBmi()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        }

        b.etBmiHeight.addTextChangedListener(watcher)
        b.etBmiWeight.addTextChangedListener(watcher)

        b.btnBmiHeightMinus.setOnClickListener {
            vibrateLight()
            adjustNumericField(b.etBmiHeight, -1.0, 40.0, 260.0)
        }
        b.btnBmiHeightPlus.setOnClickListener {
            vibrateLight()
            adjustNumericField(b.etBmiHeight, 1.0, 40.0, 260.0)
        }

        b.btnBmiWeightMinus.setOnClickListener {
            vibrateLight()
            adjustNumericField(b.etBmiWeight, -1.0, 2.0, 400.0)
        }
        b.btnBmiWeightPlus.setOnClickListener {
            vibrateLight()
            adjustNumericField(b.etBmiWeight, 1.0, 2.0, 400.0)
        }

        val heightChips = mapOf(
            b.chipHeight155 to "155",
            b.chipHeight160 to "160",
            b.chipHeight165 to "165",
            b.chipHeight170 to "170",
            b.chipHeight175 to "175"
        )
        for ((chip, hVal) in heightChips) {
            chip.setOnClickListener {
                vibrateLight()
                b.etBmiHeight.setText(hVal)
                b.etBmiHeight.setSelection(hVal.length)
            }
        }

        val weightChips = mapOf(
            b.chipWeight50 to "50",
            b.chipWeight55 to "55",
            b.chipWeight60 to "60",
            b.chipWeight65 to "65",
            b.chipWeight70 to "70"
        )
        for ((chip, wVal) in weightChips) {
            chip.setOnClickListener {
                vibrateLight()
                b.etBmiWeight.setText(wVal)
                b.etBmiWeight.setSelection(wVal.length)
            }
        }

        b.btnCopyBmiResult.setOnClickListener {
            vibrateLight()
            val summary = "BMI: ${b.tvBmiScoreValue.text} (${b.tvBmiStatusBadge.text})"
            copyToClipboard("BMI Result", summary)
        }
    }

    // Tăng hoặc giảm giá trị số trong ô nhập liệu theo bước nhảy.
    private fun adjustNumericField(editText: EditText, delta: Double, min: Double, max: Double) {
        val current = ConverterEngine.parseNormalizedDouble(editText.text?.toString()) ?: min
        val updated = (current + delta).coerceIn(min, max)
        val textVal = if (updated % 1.0 == 0.0) updated.toInt().toString() else String.format(Locale.US, "%.1f", updated)
        editText.setText(textVal)
        editText.setSelection(textVal.length)
    }

    // Tính toán và hiển thị kết quả phân tích chỉ số BMI, phân loại thể trạng và cân nặng lý tưởng.
    private fun evaluateBmi() {
        val b = viewBinding
        val result = ConverterEngine.calculateBmi(
            heightCmInput = b.etBmiHeight.text?.toString(),
            weightKgInput = b.etBmiWeight.text?.toString()
        )

        if (!result.isValid) {
            b.tvBmiScoreValue.text = "--"
            b.tvBmiStatusBadge.text = "Chưa hợp lệ"
            b.tvBmiAdviceText.text = result.errorMessage ?: ""
            return
        }

        val statusColor = Color.parseColor(result.status.colorHex)
        b.tvBmiScoreValue.text = ConverterEngine.formatOneDecimal(result.bmi)
        b.tvBmiScoreValue.setTextColor(statusColor)

        b.tvBmiStatusBadge.text = result.status.label
        b.tvBmiStatusBadge.setTextColor(statusColor)

        val minIdealStr = ConverterEngine.formatOneDecimal(result.minIdealWeight)
        val maxIdealStr = ConverterEngine.formatOneDecimal(result.maxIdealWeight)
        b.tvBmiIdealWeightRange.text = "$minIdealStr kg – $maxIdealStr kg"

        val diffText = when {
            result.weightDifference < -0.1 -> {
                val needGain = ConverterEngine.formatOneDecimal(abs(result.weightDifference))
                "Gợi ý: Cần tăng thêm khoảng $needGain kg để đạt mức cân nặng chuẩn. ${result.status.advice}"
            }
            result.weightDifference > 0.1 -> {
                val needLose = ConverterEngine.formatOneDecimal(result.weightDifference)
                "Gợi ý: Cần giảm khoảng $needLose kg để về mức cân nặng chuẩn. ${result.status.advice}"
            }
            else -> result.status.advice
        }
        b.tvBmiAdviceText.text = diffText
    }

    // Sao chép chuỗi kết quả vào khay nhớ tạm và hiển thị thông báo Toast.
    private fun copyToClipboard(label: String, value: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(requireContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    // Phát phản hồi xúc giác nhẹ khi người dùng chạm vào các nút chức năng.
    private fun vibrateLight() {
        view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
}
