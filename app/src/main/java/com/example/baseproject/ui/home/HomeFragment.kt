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

    // Thiết lập danh mục, danh sách công cụ, thanh tìm kiếm và menu ba chấm.
    override fun initViews() {
        setupCategoriesRecyclerView()
        setupToolsRecyclerView()
        setupSearchListeners()
        setupHeaderMenu()
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

    // Thiết lập sự kiện bấm nút ba chấm mở menu tiện ích mở rộng.
    private fun setupHeaderMenu() {
        viewBinding.btnMoreMenu.setOnClickListener { v ->
            val context = context ?: return@setOnClickListener
            val popup = PopupMenu(context, v)
            popup.menuInflater.inflate(R.menu.menu_home_header, popup.menu)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_guide -> {
                        replaceFragment(R.id.fragment_container, GuideFragment())
                        true
                    }
                    R.id.menu_language -> {
                        showLanguageDialog()
                        true
                    }
                    R.id.menu_permissions -> {
                        replaceFragment(R.id.fragment_container, PermissionsFragment())
                        true
                    }
                    R.id.menu_privacy -> {
                        replaceFragment(R.id.fragment_container, PrivacyPolicyFragment())
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
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
