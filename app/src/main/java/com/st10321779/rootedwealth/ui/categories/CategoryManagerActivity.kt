package com.st10321779.rootedwealth.ui.categories

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.st10321779.rootedwealth.R
import com.st10321779.rootedwealth.data.local.entity.Category
import com.st10321779.rootedwealth.databinding.ActivityCategoryManagerBinding
import com.st10321779.rootedwealth.theme.ThemeManager
import com.st10321779.rootedwealth.viewmodels.CategoryViewModel
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerClickListener
import com.flask.colorpicker.builder.ColorPickerDialogBuilder

class CategoryManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryManagerBinding
    private val viewModel: CategoryViewModel by viewModels()
    private lateinit var categoryAdapter: CategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeManager.applyTheme(this, ThemeManager.getSelectedTheme(this), binding.root)

        setupRecyclerView()
        observeViewModel()

        binding.fabAddCategory.setOnClickListener {
            showAddEditDialog(null)
        }
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter(
            onEditClick = { category -> showAddEditDialog(category) },
            onDeleteClick = { category -> showDeleteConfirmationDialog(category) }
        )
        binding.rvCategories.layoutManager = LinearLayoutManager(this)
        binding.rvCategories.adapter = categoryAdapter
    }

    private fun observeViewModel() {
        viewModel.allCategories.observe(this) { categories ->
            categoryAdapter.submitList(categories)
        }
    }

    private fun showAddEditDialog(category: Category?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_category, null)
        val etName = dialogView.findViewById<EditText>(R.id.etCategoryName)
        //get references to the new views
        val btnChooseColor = dialogView.findViewById<android.widget.Button>(R.id.btnChooseColor)
        val viewColorPreview = dialogView.findViewById<View>(R.id.viewColorPreview)

        // variabvle to hold the selected color integer
        var selectedColor = Color.parseColor("#FF5733") // Default color

        val title = if (category == null) "Add Category" else "Edit Category"
        if (category != null) {
            etName.setText(category.name)
            // If editing, set the initial color from the existing category
            try {
                selectedColor = Color.parseColor(category.color)
            } catch (e: Exception) { /* Keep default if parse fails */ }
        }

        //set the initial preview color
        viewColorPreview.setBackgroundColor(selectedColor)

        //set the click listener for the button
        btnChooseColor.setOnClickListener {
            ColorPickerDialogBuilder
                .with(this)
                .setTitle("Choose a category color")
                .initialColor(selectedColor)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .setPositiveButton("OK") { dialog, selectedColorInt , allColors ->
                    selectedColor = selectedColorInt  // Update the selected color
                    viewColorPreview.setBackgroundColor(selectedColorInt ) // Update the preview
                }
                .setNegativeButton("Cancel", null)
                .build()
                .show()
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                //convert the selected integer color back to a hex string for storage
                val colorHex = String.format("#%06X", 0xFFFFFF and selectedColor)

                if (name.isBlank()) {
                    Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (category == null) { // adding new
                    viewModel.addCategory(Category(name = name, color = colorHex, icon = "", isDefault = false))
                } else { // editing existing
                    viewModel.updateCategory(category.copy(name = name, color = colorHex))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmationDialog(category: Category) {
        AlertDialog.Builder(this)
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete '${category.name}'? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteCategory(category)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun isValidHexColor(color: String): Boolean {
        return try {
            Color.parseColor(color)
            true
        } catch (e: Exception) {
            false
        }
    }
}