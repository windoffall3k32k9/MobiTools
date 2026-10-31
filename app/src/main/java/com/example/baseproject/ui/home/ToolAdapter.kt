package com.example.baseproject.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.baseproject.R
import com.example.baseproject.databinding.ItemToolCardBinding
import com.example.baseproject.model.ToolDescriptor

/**
 * Adapter hiển thị lưới danh sách công cụ tại trang chủ MobiTools.
 */
class ToolAdapter(
    private val onToolClick: (ToolDescriptor) -> Unit,
    private val onFavoriteClick: (ToolDescriptor, Boolean) -> Unit
) : ListAdapter<ToolDescriptor, ToolAdapter.ToolViewHolder>(ToolDiffCallback()) {

    private var favoriteToolIds: Set<String> = emptySet()

    // Cập nhật tập hợp các công cụ đang được yêu thích.
    fun setFavorites(favoriteIds: Set<String>) {
        this.favoriteToolIds = favoriteIds
        notifyItemRangeChanged(0, itemCount)
    }

    inner class ToolViewHolder(val binding: ItemToolCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToolViewHolder {
        val binding = ItemToolCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ToolViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ToolViewHolder, position: Int) {
        val item = getItem(position)
        val isFav = favoriteToolIds.contains(item.id)

        holder.binding.ivToolIcon.setImageResource(item.iconResId)
        holder.binding.tvToolTitle.setText(item.titleResId)
        holder.binding.tvToolDesc.setText(item.descriptionResId)

        if (isFav) {
            holder.binding.btnCardFavorite.setImageResource(R.drawable.ic_star_filled)
        } else {
            holder.binding.btnCardFavorite.setImageResource(R.drawable.ic_star_outline)
        }

        holder.binding.root.setOnClickListener {
            onToolClick(item)
        }

        holder.binding.btnCardFavorite.setOnClickListener {
            onFavoriteClick(item, !isFav)
        }
    }

    private class ToolDiffCallback : DiffUtil.ItemCallback<ToolDescriptor>() {
        override fun areItemsTheSame(oldItem: ToolDescriptor, newItem: ToolDescriptor): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ToolDescriptor, newItem: ToolDescriptor): Boolean {
            return oldItem == newItem
        }
    }
}
