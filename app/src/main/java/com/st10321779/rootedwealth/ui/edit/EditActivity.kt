package com.st10321779.rootedwealth.ui.edit

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.st10321779.rootedwealth.data.local.entity.Category
import com.st10321779.rootedwealth.data.local.entity.Expense
import com.st10321779.rootedwealth.data.local.entity.Income
import com.st10321779.rootedwealth.databinding.ActivityEditBinding
import com.st10321779.rootedwealth.theme.ThemeManager
import com.st10321779.rootedwealth.viewmodels.EditViewModel
import java.text.SimpleDateFormat
import java.util.*

class EditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditBinding
    private val viewModel: EditViewModel by viewModels()

    private var itemId: Long = -1
    private var isExpense: Boolean = true

    private var categories: List<Category> = emptyList()
    private var selectedDate: Date = Date()
    private var originalExpense: Expense? = null
    private var originalIncome: Income? = null
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    companion object {
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_IS_EXPENSE = "extra_is_expense"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeManager.applyTheme(this, ThemeManager.getSelectedTheme(this), binding.root)

        itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1)
        isExpense = intent.getBooleanExtra(EXTRA_IS_EXPENSE, true)

        if (itemId == -1L) {
            Toast.makeText(this, "Error: Invalid item.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        observeViewModel()
        viewModel.loadItem(itemId, isExpense)
    }

    private fun setupUI() {
        if (isExpense) {
            binding.tvEditTitle.text = "Edit Expense"
            binding.spinnerEditCategory.visibility = View.VISIBLE
            binding.etEditSource.visibility = View.GONE
            setupCategorySpinner()
        } else {
            binding.tvEditTitle.text = "Edit Income"
            binding.spinnerEditCategory.visibility = View.GONE
            binding.etEditSource.visibility = View.VISIBLE
        }

        binding.btnEditDatePicker.setOnClickListener { showDatePicker() }
        binding.btnSaveChanges.setOnClickListener { saveChanges() }
    }

    private fun observeViewModel() {
        if (isExpense) {
            viewModel.expenseToEdit.observe(this) { expense ->
                expense?.let {
                    originalExpense = it
                    binding.etEditAmount.setText(it.amount.toString())
                    binding.etEditNotes.setText(it.notes)
                    selectedDate = it.date
                    binding.btnEditDatePicker.text = dateFormat.format(it.date)
                }
            }
        } else {
            viewModel.incomeToEdit.observe(this) { income ->
                income?.let {
                    originalIncome = it
                    binding.etEditAmount.setText(it.amount.toString())
                    binding.etEditSource.setText(it.source)
                    binding.etEditNotes.setText(it.notes)
                    selectedDate = it.date
                    binding.btnEditDatePicker.text = dateFormat.format(it.date)
                }
            }
        }
    }

    private fun saveChanges() {
        val amount = binding.etEditAmount.text.toString().toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
            return
        }

        if (isExpense) {
            val selectedCategory = categories[binding.spinnerEditCategory.selectedItemPosition]
            originalExpense?.let {
                val updatedExpense = it.copy(
                    amount = amount,
                    categoryId = selectedCategory.id,
                    date = selectedDate,
                    notes = binding.etEditNotes.text.toString()
                )
                viewModel.updateExpense(updatedExpense)
            }
        } else {
            originalIncome?.let {
                val updatedIncome = it.copy(
                    amount = amount,
                    source = binding.etEditSource.text.toString(),
                    date = selectedDate,
                    notes = binding.etEditNotes.text.toString()
                )
                viewModel.updateIncome(updatedIncome)
            }
        }
        Toast.makeText(this, "Changes saved!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun setupCategorySpinner() { /* ... same as in AddExpenseActivity ... */ }
    private fun showDatePicker() { /* ... same as in AddExpenseActivity ... */ }
}