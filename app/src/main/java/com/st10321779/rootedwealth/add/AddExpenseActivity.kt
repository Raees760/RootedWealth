package com.st10321779.rootedwealth.add

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.st10321779.rootedwealth.data.local.entity.Category
import com.st10321779.rootedwealth.data.local.entity.Expense
import com.st10321779.rootedwealth.databinding.ActivityAddExpenseBinding
import com.st10321779.rootedwealth.theme.ThemeManager
import com.st10321779.rootedwealth.viewmodels.AddExpenseViewModel
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.Date

class AddExpenseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddExpenseBinding
    private val viewModel: AddExpenseViewModel by viewModels()
    private var categories: List<Category> = emptyList()
    private var selectedDate: Date = Date()
    private var selectedImageUri: Uri? = null
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.ivExpenseImagePreview.setImageURI(it)
            binding.ivExpenseImagePreview.visibility = View.VISIBLE
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, launch the image picker
            imagePickerLauncher.launch("image/*")
        } else {
            // Permission denied, show a toast
            Toast.makeText(this, "Permission denied to read storage", Toast.LENGTH_SHORT).show()
        }
    }
    private fun checkPermissionAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is already granted, launch the image picker
                imagePickerLauncher.launch("image/*")
            }
            else -> {
                // Permission is not granted, request it
                permissionLauncher.launch(permission)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeManager.applyTheme(this, ThemeManager.getSelectedTheme(this), binding.root)

        setupCategorySpinner()
        setupDatePicker()

        binding.btnSaveExpense.setOnClickListener {
            saveExpense()
        }
        binding.btnAddImage.setOnClickListener {
            checkPermissionAndPickImage()
        }
    }

    private fun setupCategorySpinner() {
        viewModel.allCategories.observe(this) { categoryList ->
            categories = categoryList
            val categoryNames = categoryList.map { it.name }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerCategory.adapter = adapter
        }
    }

    private fun setupDatePicker() {
        binding.btnDatePicker.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
                selectedDate = selectedCalendar.time
                binding.btnDatePicker.text = "$selectedDay/${selectedMonth + 1}/$selectedYear"
            }, year, month, day)
            datePickerDialog.show()
        }
    }


    private fun saveExpense() {
        val amountStr = binding.etAmount.text.toString()
        if (amountStr.isEmpty() || amountStr.toDoubleOrNull() ?: 0.0 <= 0) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }
        if (binding.spinnerCategory.selectedItemPosition < 0 || categories.isEmpty()) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        val permanentImageUriString = selectedImageUri?.let { uri ->
            copyImageToInternalStorage(uri)?.toString()
        }

        val amount = amountStr.toDouble()
        val selectedCategory = categories[binding.spinnerCategory.selectedItemPosition]
        val notes = binding.etNotes.text.toString()

        val newExpense = Expense(
            amount = amount,
            date = selectedDate,
            categoryId = selectedCategory.id,
            notes = notes,
            imageUri = null // Image functionality can be added here
        )

        viewModel.addExpense(newExpense)
        Toast.makeText(this, "Expense saved!", Toast.LENGTH_SHORT).show()
        finish() // Close the activity after saving
    }
    private fun copyImageToInternalStorage(uri: Uri): Uri? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            // Create a file in the app's private files directory
            val file = File(filesDir, "expense_image_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            // Return the URI of the newly created file
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}