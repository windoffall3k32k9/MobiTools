package com.example.baseproject.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.baseproject.R
import com.example.baseproject.base.fragment.BaseFragment
import com.example.baseproject.databinding.FragmentPermissionsBinding
import com.example.baseproject.databinding.ItemPermissionRowBinding
import com.example.baseproject.utils.AppPermissionManager
import com.example.baseproject.utils.PermissionItem

/**
 * Màn hình quản lý quyền tập trung, hỗ trợ cấp tất cả quyền chỉ trong 1 lần yêu cầu.
 */
class PermissionsFragment : BaseFragment<FragmentPermissionsBinding>() {

    private lateinit var adapter: PermissionsAdapter

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshPermissionsList()
    }

    // Khởi tạo ViewBinding liên kết với layout quản lý quyền.
    override fun provideViewBinding(container: ViewGroup?): FragmentPermissionsBinding {
        return FragmentPermissionsBinding.inflate(layoutInflater, container, false)
    }

    // Thiết lập danh sách quyền và sự kiện cấp tất cả quyền cùng một lần.
    override fun initViews() {
        viewBinding.btnToolbarBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        adapter = PermissionsAdapter { item ->
            if (!item.isGranted) {
                permissionLauncher.launch(arrayOf(item.permissionKey))
            }
        }

        viewBinding.rvPermissions.layoutManager = LinearLayoutManager(requireContext())
        viewBinding.rvPermissions.adapter = adapter

        viewBinding.btnGrantAllPermissions.setOnClickListener {
            val missing = AppPermissionManager.getMissingPermissions(requireContext())
            if (missing.isNotEmpty()) {
                permissionLauncher.launch(missing.toTypedArray())
            } else {
                showToast(getString(R.string.permissions_status_granted))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionsList()
    }

    // Làm mới trạng thái cấp phép hiển thị trên từng mục quyền.
    private fun refreshPermissionsList() {
        val context = context ?: return
        val list = AppPermissionManager.getPermissionDisplayList(context)
        adapter.submitList(list)

        val hasAll = AppPermissionManager.hasAllPermissions(context)
        if (hasAll) {
            viewBinding.btnGrantAllPermissions.isEnabled = false
            viewBinding.btnGrantAllPermissions.setText(R.string.permissions_status_granted)
            viewBinding.btnGrantAllPermissions.backgroundTintList =
                ContextCompat.getColorStateList(context, R.color.colorSuccess)
        } else {
            viewBinding.btnGrantAllPermissions.isEnabled = true
            viewBinding.btnGrantAllPermissions.setText(R.string.grant_all_permissions)
            viewBinding.btnGrantAllPermissions.backgroundTintList =
                ContextCompat.getColorStateList(context, R.color.colorPrimary)
        }
    }
}

/**
 * Adapter danh sách hiển thị tên, mô tả và trạng thái Đã cấp / Chưa cấp của từng quyền.
 */
class PermissionsAdapter(
    private val onItemClick: (PermissionItem) -> Unit
) : RecyclerView.Adapter<PermissionsAdapter.ViewHolder>() {

    private var items: List<PermissionItem> = emptyList()

    // Cập nhật danh sách quyền mới và thông báo hiển thị lại toàn bộ danh sách.
    fun submitList(newItems: List<PermissionItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemPermissionRowBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPermissionRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvPermissionTitle.text = item.title
        holder.binding.tvPermissionDesc.text = item.description

        val context = holder.itemView.context
        if (item.isGranted) {
            holder.binding.tvPermissionStatus.text = context.getString(R.string.permissions_status_granted)
            holder.binding.tvPermissionStatus.setTextColor(ContextCompat.getColor(context, R.color.colorSuccess))
            holder.binding.tvPermissionStatus.setBackgroundColor(0x1A10B981)
        } else {
            holder.binding.tvPermissionStatus.text = context.getString(R.string.permissions_status_not_granted)
            holder.binding.tvPermissionStatus.setTextColor(ContextCompat.getColor(context, R.color.textSecondary))
            holder.binding.tvPermissionStatus.setBackgroundColor(0x1A64748B)
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }
}
