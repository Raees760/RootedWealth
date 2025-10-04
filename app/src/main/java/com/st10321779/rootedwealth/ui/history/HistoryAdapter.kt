package com.st10321779.rootedwealth.ui.history

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.st10321779.rootedwealth.data.model.HistoryItem
import com.st10321779.rootedwealth.databinding.ItemHistoryExpenseBinding
import com.st10321779.rootedwealth.databinding.ItemHistoryIncomeBinding
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryAdapter : ListAdapter<HistoryItem, RecyclerView.ViewHolder>(HistoryDiffCallback()) {

    private var expandedPosition = -1
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    companion object {
        private const val TYPE_EXPENSE = 0
        private const val TYPE_INCOME = 1
    }

    // A generic ViewHolder that our specific holders will extend
    abstract class BaseViewHolder<T>(binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {
        abstract fun bind(item: T)
    }

    // ViewHolder specifically for Expense items
    inner class ExpenseViewHolder(private val binding: ItemHistoryExpenseBinding) : BaseViewHolder<HistoryItem.ExpenseItem>(binding) {
        override fun bind(item: HistoryItem.ExpenseItem) {
            val expense = item.expense
            val isExpanded = adapterPosition == expandedPosition

            binding.tvCategoryName.text = expense.categoryName
            binding.tvAmount.text = currencyFormat.format(expense.amount)
            binding.tvDate.text = dateFormat.format(expense.date)

            try {
                binding.viewCategoryColor.setBackgroundColor(Color.parseColor(expense.categoryColor))
            } catch (e: Exception) {
                binding.viewCategoryColor.setBackgroundColor(Color.GRAY)
            }

            // Handle visibility of the entire expanded group
            binding.groupExpandedDetails.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.tvFullNotes.text = expense.notes ?: "No notes provided."
            if (isExpanded && !expense.imageUri.isNullOrBlank()) {
                try {
                    // This will now work correctly because the URI is a content:// URI
                    binding.ivExpenseImage.setImageURI(android.net.Uri.parse(expense.imageUri))
                    binding.ivExpenseImage.visibility = View.VISIBLE
                } catch (e: Exception) {
                    e.printStackTrace()
                    binding.ivExpenseImage.visibility = View.GONE
                }
            } else {
                binding.ivExpenseImage.visibility = View.GONE
            }

            val isLinked = expense.isLinked
            binding.btnEdit.isEnabled = !isLinked
            binding.btnDelete.isEnabled = !isLinked

            itemView.setOnClickListener {
                val previousExpandedPosition = expandedPosition
                expandedPosition = if (isExpanded) -1 else adapterPosition
                // We need to notify both the old and new positions to handle collapse/expand correctly
                if (previousExpandedPosition != -1) {
                    notifyItemChanged(previousExpandedPosition)
                }
                notifyItemChanged(expandedPosition)
            }
        }
    }

    // ViewHolder specifically for Income items
    inner class IncomeViewHolder(private val binding: ItemHistoryIncomeBinding) : BaseViewHolder<HistoryItem.IncomeItem>(binding) {
        override fun bind(item: HistoryItem.IncomeItem) {
            val income = item.income
            binding.tvIncomeSource.text = income.source
            binding.tvIncomeAmount.text = "+ ${currencyFormat.format(income.amount)}"
            binding.tvIncomeDate.text = dateFormat.format(income.date)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is HistoryItem.ExpenseItem -> TYPE_EXPENSE
            is HistoryItem.IncomeItem -> TYPE_INCOME
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_EXPENSE -> ExpenseViewHolder(ItemHistoryExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            TYPE_INCOME -> IncomeViewHolder(ItemHistoryIncomeBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is ExpenseViewHolder -> holder.bind(item as HistoryItem.ExpenseItem)
            is IncomeViewHolder -> holder.bind(item as HistoryItem.IncomeItem)
        }
    }
}

class HistoryDiffCallback : DiffUtil.ItemCallback<HistoryItem>() {
    override fun areItemsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
        return when {
            oldItem is HistoryItem.ExpenseItem && newItem is HistoryItem.ExpenseItem ->
                oldItem.expense.id == newItem.expense.id
            oldItem is HistoryItem.IncomeItem && newItem is HistoryItem.IncomeItem ->
                oldItem.income.id == newItem.income.id
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
        return oldItem == newItem
    }
}