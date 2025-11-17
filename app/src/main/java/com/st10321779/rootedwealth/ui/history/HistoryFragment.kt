package com.st10321779.rootedwealth.ui.history

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.st10321779.rootedwealth.R
import com.st10321779.rootedwealth.data.local.dao.CategorySpending
import com.st10321779.rootedwealth.data.model.HistoryItem
import com.st10321779.rootedwealth.databinding.FragmentHistoryBinding
import com.st10321779.rootedwealth.theme.ThemeManager
import com.st10321779.rootedwealth.ui.edit.EditActivity
import com.st10321779.rootedwealth.viewmodels.FilterPeriod
import com.st10321779.rootedwealth.viewmodels.HistoryViewModel
import com.st10321779.rootedwealth.viewmodels.TimeSeriesDataPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        ThemeManager.applyTheme(requireContext(), ThemeManager.getSelectedTheme(requireContext()), binding.root)

        setupRecyclerView()
        setupFilterChips()
        setupAnalytics()
        observeViewModel()

        return binding.root
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(
            onEditClick = { item ->
                // the new navigation logic
                val intent = Intent(requireContext(), EditActivity::class.java)
                when (item) {
                    is HistoryItem.ExpenseItem -> {
                        intent.putExtra(EditActivity.EXTRA_ITEM_ID, item.expense.id)
                        intent.putExtra(EditActivity.EXTRA_IS_EXPENSE, true)
                    }
                    is HistoryItem.IncomeItem -> {
                        intent.putExtra(EditActivity.EXTRA_ITEM_ID, item.income.id)
                        intent.putExtra(EditActivity.EXTRA_IS_EXPENSE, false)
                    }
                }
                startActivity(intent)
            },
            onDeleteClick = { item ->
                showDeleteConfirmationDialog(item)
            }
        )
        binding.rvHistory.adapter = historyAdapter
    }
    private fun showDeleteConfirmationDialog(item: HistoryItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this entry?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteHistoryItem(item)
                Toast.makeText(requireContext(), "Entry deleted.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chipToday -> viewModel.setPeriod(FilterPeriod.TODAY)
                R.id.chipWeek -> viewModel.setPeriod(FilterPeriod.WEEK)
                R.id.chipMonth -> viewModel.setPeriod(FilterPeriod.MONTH)
                R.id.chipLastMonth -> viewModel.setPeriod(FilterPeriod.LAST_MONTH)
                R.id.chipCustom -> {
                    showCustomDateRangePicker()
                }
            }
        }
    }private fun showCustomDateRangePicker() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // Show Start Date Picker
        val startDatePicker = DatePickerDialog(
            requireContext(),
            { _, startYear, startMonth, startDay ->
                val startCal = Calendar.getInstance().apply { set(startYear, startMonth, startDay) }
                val startDate = startCal.time

                // after Start Date is set, show End Date Picker
                val endDatePicker = DatePickerDialog(
                    requireContext(),
                    { _, endYear, endMonth, endDay ->
                        val endCal = Calendar.getInstance().apply { set(endYear, endMonth, endDay) }
                        // set time to end of day for inclusive filtering
                        endCal.set(Calendar.HOUR_OF_DAY, 23)
                        endCal.set(Calendar.MINUTE, 59)
                        endCal.set(Calendar.SECOND, 59)
                        val endDate = endCal.time

                        //Update the ViewModel with the custom range
                        viewModel.setCustomPeriod(startDate, endDate)

                        // Update the chip text to show the selected range
                        binding.chipCustom.text =
                            "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}"
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
                endDatePicker.datePicker.minDate =
                    startDate.time // Prevent picking an end date before the start date
                endDatePicker.setTitle("Select End Date")
                endDatePicker.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        startDatePicker.setTitle("Select Start Date")
        startDatePicker.show()
    }

    private fun setupAnalytics() {
        binding.chipGroupChartType.setOnCheckedChangeListener { _, checkedId ->
            binding.analyticsPieChart.visibility = if (checkedId == R.id.chipPieChart) View.VISIBLE else View.GONE
            binding.analyticsBarChart.visibility = if (checkedId == R.id.chipBarChart) View.VISIBLE else View.GONE
            binding.analyticsLineChart.visibility = if (checkedId == R.id.chipLineChart) View.VISIBLE else View.GONE
        }
        // Configure chart appearances
        setupPieChartAppearance()
        setupBarChartAppearance()
        setupLineChartAppearance()
    }

    private fun observeViewModel() {
        viewModel.combinedHistory.observe(viewLifecycleOwner) { historyItems ->
            binding.tvEmptyHistory.visibility = if (historyItems.isNullOrEmpty()) View.VISIBLE else View.GONE
            historyAdapter.submitList(historyItems)
        }

        viewModel.spendingByCategory.observe(viewLifecycleOwner) { spendingList ->
            drawPieChart(spendingList)
            drawBarChart(spendingList)
        }

        viewModel.alignmentTrackerData.observe(viewLifecycleOwner) { info ->
            binding.tvAlignmentLabel.text = info.label
            binding.tvAlignmentInsight.text = info.insight
        }

        viewModel.timelineData.observe(viewLifecycleOwner) { timelinePoints ->
            drawLineChart(timelinePoints)
        }
    }

    // CHART DRAWING FUNCTIONS
    private fun setupPieChartAppearance() {
        binding.analyticsPieChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 58f
            transparentCircleRadius = 61f
            setUsePercentValues(true)
            setEntryLabelColor(Color.BLACK) // This will be overridden by the dataset
            legend.isEnabled = false
            // Disable rotation on touch
            isRotationEnabled = false
        }
    }

    private fun drawPieChart(data: List<CategorySpending>) {
        if (data.isEmpty()) {
            binding.analyticsPieChart.clear()
            binding.analyticsPieChart.invalidate()
            return
        }

        val entries = data.map { PieEntry(it.total.toFloat(), it.categoryName) }
        val themeTextColor = ContextCompat.getColor(requireContext(), ThemeManager.getSelectedTheme(requireContext()).textColorRes)
        val dataSet = PieDataSet(entries, "").apply {

            // LABEL FIXES
            // draw labels inside
            yValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE
            xValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE
            /*
            // configure the lines that connect labels to slices
            valueLinePart1Length = 0.6f
            valueLinePart2Length = 0.3f
            valueLineColor = themeTextColor*/

            // set text colors
            valueTextColor = Color.BLACK
            valueTextSize = 12f
            //setEntryLabelColor(themeTextColor)
            //entryLabelTextSize = 10f

            //Hide the slice labels (like "Groceries")
            // and only show the formatted percentage value.
            setDrawIcons(false)
            setDrawValues(true)

            // Standard colors
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            sliceSpace = 2f
        }
        binding.analyticsPieChart.data = PieData(dataSet).apply {
            setValueFormatter(object : ValueFormatter() {
                // control what is shown inside the slice
                override fun getFormattedValue(value: Float): String {
                    // Only show a value if it's a significant slice (e.g., > 5%)
                    if (value > 5) {
                        return "${"%.0f".format(value)}%"
                    }
                    return "" // Return empty string for small slices
                }
            })
        }
        binding.analyticsPieChart.setDrawEntryLabels(true)
        binding.analyticsPieChart.invalidate()
    }

    private fun setupBarChartAppearance() {
        binding.analyticsBarChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
            xAxis.setDrawGridLines(false)
            axisLeft.setDrawGridLines(true)

            val finalTextColor = ThemeManager.getCalculatedTextColor(requireContext())

            //Label fixes
            val xAxis = this.xAxis
            xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            // Rotate the labels to give them more space
            xAxis.labelRotationAngle = -45f
            // Set the text color to match the theme
            //val themeTextColor = ContextCompat.getColor(requireContext(), ThemeManager.getSelectedTheme(requireContext()).textColorRes)

            axisLeft.gridColor = Color.argb(50, Color.red(finalTextColor), Color.green(finalTextColor), Color.blue(finalTextColor))
            axisLeft.axisLineColor = finalTextColor
            xAxis.axisLineColor = finalTextColor

            xAxis.textColor = finalTextColor
            axisLeft.textColor = finalTextColor
        }
    }
    private fun drawBarChart(data: List<CategorySpending>) {
        val entries = data.mapIndexed { index, spending -> BarEntry(index.toFloat(), spending.total.toFloat()) }
        val labels = data.map { it.categoryName }

        binding.analyticsBarChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String = labels.getOrNull(value.toInt()) ?: ""
        }

        val dataSet = BarDataSet(entries, "Expenses").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextColor = ContextCompat.getColor(requireContext(), ThemeManager.getSelectedTheme(requireContext()).textColorRes)
        }
        binding.analyticsBarChart.data = BarData(dataSet)
        binding.analyticsBarChart.invalidate()
    }

    private fun setupLineChartAppearance() {
        binding.analyticsLineChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            axisLeft.setDrawGridLines(true)
            axisRight.isEnabled = false

            val finalTextColor = ThemeManager.getCalculatedTextColor(requireContext())

            // X-axis
            val xAxis = this.xAxis
            xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f // One label per day

            // Set text color to match theme
            //val themeTextColor = ContextCompat.getColor(requireContext(), ThemeManager.getSelectedTheme(requireContext()).textColorRes)

            axisLeft.gridColor = Color.argb(50, Color.red(finalTextColor), Color.green(finalTextColor), Color.blue(finalTextColor))
            axisLeft.axisLineColor = finalTextColor
            xAxis.axisLineColor = finalTextColor

            xAxis.textColor = finalTextColor
            axisLeft.textColor = finalTextColor
        }
    }

    private fun drawLineChart(data: List<TimeSeriesDataPoint>) {

        if (data.isEmpty()) {
            binding.analyticsLineChart.clear()
            binding.analyticsLineChart.invalidate()
            return
        }

        // The first date in our period is our reference timestamp (time zero for the chart)
        val referenceTimestamp = data.first().date.time

        val entries = data.map { dataPoint ->
            // X-value is the number of days since the start of the period
            val daysSinceStart = TimeUnit.MILLISECONDS.toDays(dataPoint.date.time - referenceTimestamp)
            // Y-value is the net amount
            Entry(daysSinceStart.toFloat(), dataPoint.netAmount.toFloat())
        }

        // X-axis label formatting
        binding.analyticsLineChart.xAxis.valueFormatter = object : ValueFormatter() {
            private val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
                // Convert the "days since start" float back into a date for the label
                val dateInMillis = referenceTimestamp + TimeUnit.DAYS.toMillis(value.toLong())
                return dateFormat.format(Date(dateInMillis))
            }
        }

        val dataSet = LineDataSet(entries, "Daily Net Flow").apply {
            color = ContextCompat.getColor(requireContext(), R.color.accent_default)
            valueTextColor = ContextCompat.getColor(requireContext(), ThemeManager.getSelectedTheme(requireContext()).textColorRes)
            setCircleColor(color)
            circleHoleColor = color
            circleRadius = 4f
            lineWidth = 2.5f
            // Draw a filled area below the line
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.line_chart_fill) // We'll create this next
        }

        binding.analyticsLineChart.data = LineData(dataSet)
        binding.analyticsLineChart.invalidate()
       /* // Placeholder: uses category data. For a real time-series, you'd need a different query.
        val entries = data.mapIndexed { index, spending -> Entry(index.toFloat(), spending.total.toFloat()) }
        val dataSet = LineDataSet(entries, "Spending").apply {
            color = ContextCompat.getColor(requireContext(), R.color.accent_default)
            valueTextColor = ContextCompat.getColor(requireContext(), ThemeManager.getSelectedTheme(requireContext()).textColorRes)
            setCircleColor(color)
        }
        binding.analyticsLineChart.data = LineData(dataSet)
        binding.analyticsLineChart.invalidate()*/
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}