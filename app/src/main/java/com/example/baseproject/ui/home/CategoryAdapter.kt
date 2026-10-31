package com.example.baseproject.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.baseproject.R
import com.example.baseproject.databinding.ItemCategoryChipBinding

/**
 * Mục lọc danh mục hiển thị trên thanh cuộn ngang của trang chủ.
 */
data class CategoryFilterItem(
    val id: String,
    val titleResId: Int
)

/**
 * Adapter hiển thị các danh mục lọc công cụ trên trang chủ.
 */
class CategoryAdapter(
    private val items: List<CategoryFilterItem>,
    private val onCategorySelected: (CategoryFilterItem) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var selectedIndex = 0

    inner class CategoryViewHolder(val binding: ItemCategoryChipBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryChipBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val item = items[position]
        val isSelected = position == selectedIndex
        val context = holder.itemView.context

        holder.binding.tvChipTitle.setText(item.titleResId)

        if (isSelected) {
            holder.binding.tvChipTitle.setBackgroundResource(R.drawable.bg_chip_selected)
            holder.binding.tvChipTitle.setTextColor(ContextCompat.getColor(context, R.color.white))
        } else {
            holder.binding.tvChipTitle.setBackgroundResource(R.drawable.bg_chip_unselected)
            holder.binding.tvChipTitle.setTextColor(
                ContextCompat.getColor(context, R.color.textSecondary)
            )
        }

        holder.binding.root.setOnClickListener {
            val previousIndex = selectedIndex
            selectedIndex = holder.bindingAdapterPosition
            notifyItemChanged(previousIndex)
            notifyItemChanged(selectedIndex)
            onCategorySelected(item)
        }
    }

    // Chọn danh mục theo mã định danh.
    fun setSelectedId(id: String) {
        val index = items.indexOfFirst { it.id == id }
        if (index != -1 && index != selectedIndex) {
            val prev = selectedIndex
            selectedIndex = index
            notifyItemChanged(prev)
            notifyItemChanged(selectedIndex)
        }
    }
}
