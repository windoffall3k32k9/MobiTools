package com.example.baseproject.ui.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.baseproject.R
import com.example.baseproject.base.fragment.BaseFragment
import com.example.baseproject.data.database.AppDatabase
import com.example.baseproject.data.database.dao.ToolDao
import com.example.baseproject.databinding.FragmentHomeBinding
import com.example.baseproject.localization.AppLanguageManager
import com.example.baseproject.model.ToolDescriptor
import com.example.baseproject.model.ToolGroup
import com.example.baseproject.registry.ToolRegistry
import com.example.baseproject.ui.settings.GuideFragment
import com.example.baseproject.ui.settings.PermissionsFragment
import com.example.baseproject.ui.settings.PrivacyPolicyFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Màn hình chính của MobiTools cung cấp tìm kiếm, lọc danh mục và truy cập nhanh vào từng công cụ.
 */
class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private val toolDao: ToolDao by lazy {
        AppDatabase.getInstance(requireContext()).toolDao()
    }

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var toolAdapter: ToolAdapter

    private var selectedCategoryId: String = "ALL"
    private var searchQuery: String = ""
    private var favoriteToolIds: Set<String> = emptySet()
    private var recentToolIds: List<String> = emptyList()

    // Khởi tạo ViewBinding liên kết với layout màn hình chính.
    override fun provideViewBinding(container: ViewGroup?): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(layoutInflater, container, false)
    }

    // Thiết lập danh mục, danh sách công cụ, thanh tìm kiếm và menu ba gạch chuẩn MobiCheck.
    override fun initViews() {
        setupCategoriesRecyclerView()
        setupToolsRecyclerView()
        setupSearchListeners()
        setupDrawerMenu()
    }

    override fun onResume() {
        super.onResume()
        updateCurrentLanguageLabel()
    }

    // Lắng nghe dữ liệu danh sách yêu thích và lịch sử gần đây từ Room.
    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    toolDao.getFavorites().collectLatest { entities ->
                        favoriteToolIds = entities.map { it.toolId }.toSet()
                        toolAdapter.setFavorites(favoriteToolIds)
                        if (selectedCategoryId == "FAVORITES") {
                            applyFilterAndSearch()
                        }
                    }
                }
                launch {
                    toolDao.getRecents(15).collectLatest { entities ->
                        recentToolIds = entities.map { it.toolId }
                        if (selectedCategoryId == "RECENTS") {
                            applyFilterAndSearch()
                        }
                    }
                }
            }
        }
    }

    // Cấu hình menu tiện ích chuẩn MobiCheck dạng popup mở rộng ở góc trên bên phải.
    private fun setupDrawerMenu() {
        viewBinding.btnOpenMenu.setOnClickListener { openDrawer() }
        viewBinding.btnCloseMenu.setOnClickListener { closeDrawer() }
        viewBinding.viewDrawerBackdrop.setOnClickListener { closeDrawer() }
        updateCurrentLanguageLabel()

        viewBinding.menuItemGuide.setOnClickListener {
            closeDrawer()
            replaceFragment(R.id.fragment_container, GuideFragment())
        }

        viewBinding.menuItemLanguage.setOnClickListener {
            closeDrawer()
            showLanguageDialog()
        }

        viewBinding.menuItemPermissions.setOnClickListener {
            closeDrawer()
            replaceFragment(R.id.fragment_container, PermissionsFragment())
        }

        viewBinding.menuItemPrivacy.setOnClickListener {
            closeDrawer()
            replaceFragment(R.id.fragment_container, PrivacyPolicyFragment())
        }
    }

    // Mở popup menu góc trên bên phải với hiệu ứng mở rộng mượt mà.
    fun openDrawer() {
        viewBinding.layoutDrawerMenu.visibility = View.VISIBLE
        viewBinding.layoutDrawerMenu.alpha = 0f
        viewBinding.cardMenuPopup.scaleX = 0.85f
        viewBinding.cardMenuPopup.scaleY = 0.85f
        viewBinding.cardMenuPopup.pivotX = 270f * resources.displayMetrics.density
        viewBinding.cardMenuPopup.pivotY = 0f
        viewBinding.layoutDrawerMenu.animate().alpha(1f).setDuration(160).start()
        viewBinding.cardMenuPopup.animate().scaleX(1f).scaleY(1f).setDuration(160).start()
    }

    // Đóng popup menu với hiệu ứng thu nhỏ và mờ dần.
    fun closeDrawer() {
        viewBinding.cardMenuPopup.animate().scaleX(0.85f).scaleY(0.85f).setDuration(120).start()
        viewBinding.layoutDrawerMenu.animate().alpha(0f).setDuration(120).withEndAction {
            viewBinding.layoutDrawerMenu.visibility = View.GONE
        }.start()
    }

    // Kiểm tra menu popup có đang mở hay không.
    fun isDrawerOpen(): Boolean {
        return viewBinding.layoutDrawerMenu.visibility == View.VISIBLE
    }

    // Cập nhật nhãn ngôn ngữ hiện tại hiển thị trên mục menu.
    private fun updateCurrentLanguageLabel() {
        val context = context ?: return
        val current = when (AppLanguageManager.currentMode(context)) {
            AppLanguageManager.Mode.VIETNAMESE -> getString(R.string.language_vietnamese)
            AppLanguageManager.Mode.ENGLISH -> getString(R.string.language_english)
            AppLanguageManager.Mode.SYSTEM -> getString(R.string.language_system)
        }
        viewBinding.tvCurrentLanguage.text = current
    }

    // Hiển thị hộp thoại chọn ngôn ngữ ứng dụng độc lập tương tự MobiCheck.
    private fun showLanguageDialog() {
        val context = context ?: return
        val modes = arrayOf(
            AppLanguageManager.Mode.VIETNAMESE,
            AppLanguageManager.Mode.ENGLISH,
            AppLanguageManager.Mode.SYSTEM
        )
        val labels = arrayOf(
            getString(R.string.language_vietnamese),
            getString(R.string.language_english),
            getString(R.string.language_system)
        )
        val currentMode = AppLanguageManager.currentMode(context)
        var selectedMode = currentMode

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.language_dialog_title)
            .setSingleChoiceItems(labels, modes.indexOf(currentMode)) { _, index ->
                selectedMode = modes[index]
            }
            .setNegativeButton(R.string.common_cancel, null)
            .setPositiveButton(R.string.common_save) { _, _ ->
                if (selectedMode != currentMode) {
                    AppLanguageManager.setMode(context, selectedMode)
                    requireActivity().recreate()
                }
            }
            .show()
    }

    // Thiết lập thanh danh mục cuộn ngang.
    private fun setupCategoriesRecyclerView() {
        val categories = listOf(
            CategoryFilterItem("ALL", R.string.group_all),
            CategoryFilterItem("FAVORITES", R.string.group_favorites),
            CategoryFilterItem("RECENTS", R.string.group_recents),
            CategoryFilterItem(ToolGroup.CALCULATION_TIME.name, R.string.group_calculation_time),
            CategoryFilterItem(ToolGroup.DEVICE_UTILITY.name, R.string.group_device_utility),
            CategoryFilterItem(ToolGroup.TEXT_CONVERSION.name, R.string.group_text_conversion),
            CategoryFilterItem(ToolGroup.MEASUREMENT_SENSOR.name, R.string.group_measurement_sensor),
            CategoryFilterItem(ToolGroup.OBSERVATION_DATA.name, R.string.group_observation_data)
        )

        categoryAdapter = CategoryAdapter(categories) { selectedItem ->
            selectedCategoryId = selectedItem.id
            applyFilterAndSearch()
        }

        viewBinding.rvCategories.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }
    }

    // Thiết lập lưới danh sách công cụ dạng 2 cột.
    private fun setupToolsRecyclerView() {
        toolAdapter = ToolAdapter(
            onToolClick = { descriptor ->
                openTool(descriptor)
            },
            onFavoriteClick = { descriptor, isFav ->
                viewLifecycleOwner.lifecycleScope.launch {
                    toolDao.toggleFavorite(descriptor.id, isFav)
                }
            }
        )

        viewBinding.rvTools.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = toolAdapter
        }

        applyFilterAndSearch()
    }

    // Lắng nghe sự kiện gõ tìm kiếm và nút xóa nội dung tìm kiếm.
    private fun setupSearchListeners() {
        viewBinding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString() ?: ""
                viewBinding.btnClearSearch.visibility = if (searchQuery.isNotEmpty()) View.VISIBLE else View.GONE
                applyFilterAndSearch()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        viewBinding.btnClearSearch.setOnClickListener {
            viewBinding.etSearch.text?.clear()
        }
    }

    // Mở màn hình công cụ sau khi kiểm tra tính tương thích phần cứng.
    private fun openTool(descriptor: ToolDescriptor) {
        val context = context ?: return
        if (!descriptor.isHardwareSupported(context)) {
            val reason = descriptor.unsupportedMessageResId ?: R.string.hardware_not_supported
            Toast.makeText(context, reason, Toast.LENGTH_SHORT).show()
            return
        }

        val fragment = descriptor.createFragment()
        replaceFragment(R.id.fragment_container, fragment)
    }

    // Áp dụng bộ lọc danh mục và từ khóa tìm kiếm lên danh sách công cụ.
    private fun applyFilterAndSearch() {
        val context = context ?: return
        var baseList = when (selectedCategoryId) {
            "ALL" -> ToolRegistry.getAll()
            "FAVORITES" -> ToolRegistry.getAll().filter { favoriteToolIds.contains(it.id) }
            "RECENTS" -> {
                val toolMap = ToolRegistry.getAll().associateBy { it.id }
                recentToolIds.mapNotNull { toolMap[it] }
            }
            else -> {
                try {
                    val group = ToolGroup.valueOf(selectedCategoryId)
                    ToolRegistry.getByGroup(group)
                } catch (e: Exception) {
                    ToolRegistry.getAll()
                }
            }
        }

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            baseList = baseList.filter { descriptor ->
                val title = context.getString(descriptor.titleResId).lowercase()
                val desc = context.getString(descriptor.descriptionResId).lowercase()
                title.contains(q) || desc.contains(q)
            }
        }

        toolAdapter.submitList(baseList)

        if (baseList.isEmpty()) {
            viewBinding.layoutEmpty.visibility = View.VISIBLE
            viewBinding.rvTools.visibility = View.GONE
            val emptyMsg = when (selectedCategoryId) {
                "FAVORITES" -> R.string.no_favorites_yet
                "RECENTS" -> R.string.no_recents_yet
                else -> R.string.no_tools_found
            }
            viewBinding.tvEmptyTitle.setText(emptyMsg)
        } else {
            viewBinding.layoutEmpty.visibility = View.GONE
            viewBinding.rvTools.visibility = View.VISIBLE
        }
    }
}
