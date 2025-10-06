package com.st10321779.rootedwealth.add

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.st10321779.rootedwealth.data.local.entity.Income
import com.st10321779.rootedwealth.databinding.ActivityAddIncomeBinding
import com.st10321779.rootedwealth.theme.ThemeManager
import com.st10321779.rootedwealth.viewmodels.HomeViewModel
import java.util.*

class AddIncomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddIncomeBinding
    private val viewModel: HomeViewModel by viewModels()
    private var selectedDate: Date = Date()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddIncomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeManager.applyTheme(this, ThemeManager.getSelectedTheme(this), binding.root)

        setupDatePicker()

        binding.btnSaveIncome.setOnClickListener { saveIncome() }
    }


    private fun setupDatePicker() {
        binding.btnIncomeDatePicker.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
                selectedDate = selectedCalendar.time
                binding.btnIncomeDatePicker.text = "$selectedDay/${selectedMonth + 1}/$selectedYear"
            }, year, month, day)
            datePickerDialog.show()
        }
    }

    private fun saveIncome() {
        val amountStr = binding.etIncomeAmount.text.toString()
        val source = binding.etIncomeSource.text.toString()

        if (amountStr.isBlank() || amountStr.toDouble() <= 0) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }
        if (source.isBlank()) {
            Toast.makeText(this, "Please enter a source", Toast.LENGTH_SHORT).show()
            return
        }

        val newIncome = Income(
            amount = amountStr.toDouble(),
            date = selectedDate,
            source = source,
            notes = null
        )
        viewModel.addIncome(newIncome) // We will add this to the ViewModel
        Toast.makeText(this, "Income saved!", Toast.LENGTH_SHORT).show()
        finish()
    }
}