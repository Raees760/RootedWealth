package com.st10321779.rootedwealth.ui.categories

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.st10321779.rootedwealth.data.local.entity.Category
import com.st10321779.rootedwealth.databinding.ItemCategoryBinding

class CategoryAdapter(
    private val onEditClick: (Category) -> Unit,
    private val onDeleteClick: (Category) -> Unit
) : ListAdapter<Category, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    inner class CategoryViewHolder(private val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: Category) {
            binding.tvCategoryName.text = category.name
            try {
                binding.viewCategoryColor.setBackgroundColor(Color.parseColor(category.color))
            } catch (e: Exception) {
                binding.viewCategoryColor.setBackgroundColor(Color.GRAY)
            }

            if (category.isDefault) {
                // Default category: non-editable/deletable
                binding.btnCategoryDelete.visibility = View.GONE
                binding.ivDefaultIcon.visibility = View.VISIBLE
                itemView.setOnClickListener(null) // Not clickable
            } else {
                // Custom category: editable/deletable
                binding.btnCategoryDelete.visibility = View.VISIBLE
                binding.ivDefaultIcon.visibility = View.GONE
                binding.btnCategoryDelete.setOnClickListener { onDeleteClick(category) }
                itemView.setOnClickListener { onEditClick(category) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
    override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean = oldItem == newItem
}