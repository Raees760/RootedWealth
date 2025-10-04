package com.st10321779.rootedwealth.ui.history

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.st10321779.rootedwealth.R
import com.st10321779.rootedwealth.data.local.dao.CategorySpending
import com.st10321779.rootedwealth.databinding.FragmentHistoryBinding
import com.st10321779.rootedwealth.theme.ThemeManager
import com.st10321779.rootedwealth.viewmodels.FilterPeriod
import com.st10321779.rootedwealth.viewmodels.HistoryViewModel

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
        historyAdapter = HistoryAdapter()
        binding.rvHistory.adapter = historyAdapter
    }

    private fun setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chipToday -> viewModel.setPeriod(FilterPeriod.TODAY)
                R.id.chipWeek -> viewModel.setPeriod(FilterPeriod.WEEK)
                R.id.chipMonth -> viewModel.setPeriod(FilterPeriod.MONTH)
                R.id.chipLastMonth -> viewModel.setPeriod(FilterPeriod.LAST_MONTH)
            }
        }
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
            drawLineChart(spendingList) // Note: Uses same data for now
        }

        viewModel.alignmentTrackerData.observe(viewLifecycleOwner) { info ->
            binding.tvAlignmentLabel.text = info.label
            binding.tvAlignmentInsight.text = info.insight
        }
    }

    // CHART DRAWING FUNCTIONS
    private fun setupPieChartAppearance() {
        binding.analyticsPieChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            legend.isEnabled = false
        }
    }

    private fun drawPieChart(data: List<CategorySpending>) {
        val entries = data.map { PieEntry(it.total.toFloat(), it.categoryName) }
        val dataSet = PieDataSet(entries, "Expenses").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextColor = ContextCompat.getColor(requireContext(), ThemeManager.getSelectedTheme(requireContext()).textColorRes)
            valueTextSize = 12f
        }
        binding.analyticsPieChart.data = PieData(dataSet)
        binding.analyticsPieChart.invalidate()
    }

    private fun setupBarChartAppearance() {
        binding.analyticsBarChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
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
            xAxis.setDrawGridLines(false)
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
        }
    }

    private fun drawLineChart(data: List<CategorySpending>) {
        // Placeholder: uses category data. For a real time-series, you'd need a different query.
        val entries = data.mapIndexed { index, spending -> Entry(index.toFloat(), spending.total.toFloat()) }
        val dataSet = LineDataSet(entries, "Spending").apply {
            color = ContextCompat.getColor(requireContext(), R.color.accent_default)
            valueTextColor = ContextCompat.getColor(requireContext(), ThemeManager.getSelectedTheme(requireContext()).textColorRes)
            setCircleColor(color)
        }
        binding.analyticsLineChart.data = LineData(dataSet)
        binding.analyticsLineChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}