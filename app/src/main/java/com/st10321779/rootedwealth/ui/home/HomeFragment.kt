package com.st10321779.rootedwealth.ui.home

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.st10321779.rootedwealth.R
import com.st10321779.rootedwealth.add.AddExpenseActivity
import com.st10321779.rootedwealth.add.AddIncomeActivity
import com.st10321779.rootedwealth.databinding.FragmentHomeBinding
import com.st10321779.rootedwealth.settings.SettingsActivity
import com.st10321779.rootedwealth.theme.ThemeManager
import com.st10321779.rootedwealth.viewmodels.HomeViewModel
import java.text.NumberFormat
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root = binding.root

        setupUI()
        observeViewModel()

        return root
    }

    private fun setupUI() {
        ThemeManager.applyTheme(requireContext(), ThemeManager.getSelectedTheme(requireContext()), binding.root)

        binding.btnAddExpense.setOnClickListener {
            startActivity(Intent(requireContext(), AddExpenseActivity::class.java))
        }
        binding.btnAddIncome.setOnClickListener {
            startActivity(Intent(requireContext(), AddIncomeActivity::class.java))
        }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        setupPieChart()
    }

    private fun observeViewModel() {
        var currentSpent = 0.0
        var currentBudget = 0.0f
        homeViewModel.totalSpentThisMonth.observe(viewLifecycleOwner) { spent ->
            currentSpent = spent ?: 0.0
            updateBudgetSummary(currentSpent, currentBudget.toDouble())
        }
        homeViewModel.monthlyBudget.observe(viewLifecycleOwner) { budget ->
            currentBudget = budget
            updateBudgetSummary(currentSpent, currentBudget.toDouble())
        }

        // Placeholder data for pie chart
        //updatePieChart(listOf(PieEntry(450f, "Groceries"), PieEntry(200f, "Transport"), PieEntry(800f, "Takeout")))

        var totalSpent = 0.0
        var totalIncome = 0.0

        homeViewModel.totalSpentThisMonth.observe(viewLifecycleOwner) { spent ->
            totalSpent = spent ?: 0.0
            updatePieChartCenterText(totalSpent, totalIncome)
            // ... update budget summary
        }

        homeViewModel.totalIncomeThisMonth.observe(viewLifecycleOwner) { income ->
            totalIncome = income ?: 0.0
            updatePieChartCenterText(totalSpent, totalIncome)
        }
        homeViewModel.spendingByCategory.observe(viewLifecycleOwner) { spendingList ->
            val entries = spendingList.map { PieEntry(it.total.toFloat(), it.categoryName) }
            updatePieChart(entries)
        }
        //Observe gamification data
        homeViewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.tvStreak.text = "🔥 Streak: ${state.streakCount} Days"
            binding.tvCoins.text = "💰 Coins: ${state.coinBalance}"
            binding.tvBankLinkBanner.visibility = if (state.isBankLinked) View.VISIBLE else View.GONE
        }
    }

    private fun updatePieChartCenterText(spent: Double, income: Double) {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
        binding.pieChart.centerText = "Income: ${currencyFormat.format(income)}\nSpent: ${currencyFormat.format(spent)}"
    }
    private fun updateBudgetSummary(spent: Double, budget: Double) {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
        binding.tvBudgetRatio.text = "${currencyFormat.format(spent)} / ${currencyFormat.format(budget)}"

        val progress = if (budget > 0) (spent / budget * 100).toInt() else 0
        binding.pbMonthlyProgress.progress = progress

        val progressColor = when {
            progress < 80 -> R.color.budget_green
            progress <= 100 -> R.color.budget_amber
            else -> R.color.budget_red
        }
        binding.pbMonthlyProgress.progressTintList = ContextCompat.getColorStateList(requireContext(), progressColor)
    }

    private fun setupPieChart() {
        binding.pieChart.description.isEnabled = false
        binding.pieChart.isDrawHoleEnabled = true
        binding.pieChart.holeRadius = 58f
        binding.pieChart.transparentCircleRadius = 61f
        binding.pieChart.setUsePercentValues(true)
        binding.pieChart.setEntryLabelColor(Color.BLACK)
        binding.pieChart.legend.isEnabled = false
    }

    private fun updatePieChart(entries: List<PieEntry>) {
        if (entries.isEmpty()) {
            binding.pieChart.visibility = View.VISIBLE
            binding.pieChart.clear() // Clear old data
            binding.pieChart.centerText = "No expenses this month"
            binding.pieChart.invalidate()
            binding.tvNoDataChart.visibility = View.VISIBLE
            return
        }

        binding.pieChart.visibility = View.VISIBLE
        binding.tvNoDataChart.visibility = View.GONE

        val dataSet = PieDataSet(entries, "Monthly Expenses")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.sliceSpace = 3f

        val data = PieData(dataSet)
        data.setValueTextSize(12f)

        // Set text color based on the current theme
        val theme = ThemeManager.getSelectedTheme(requireContext())
        val textColor = ContextCompat.getColor(requireContext(), theme.textColorRes)
        binding.pieChart.setEntryLabelColor(textColor)
        data.setValueTextColor(textColor)

        binding.pieChart.data = data
        binding.pieChart.invalidate() // refresh
    }

    override fun onResume() {
        super.onResume()
        // Re-apply theme in case it was changed in Settings
        ThemeManager.applyTheme(requireContext(), ThemeManager.getSelectedTheme(requireContext()), binding.root)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}