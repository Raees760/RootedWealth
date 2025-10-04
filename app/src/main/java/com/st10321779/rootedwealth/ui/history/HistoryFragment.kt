package com.st10321779.rootedwealth.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.st10321779.rootedwealth.R
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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        ThemeManager.applyTheme(requireContext(), ThemeManager.getSelectedTheme(requireContext()), binding.root)

        setupRecyclerView()
        setupFilterChips()
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

    private fun observeViewModel() {
        viewModel.combinedHistory.observe(viewLifecycleOwner) { historyItems ->
            if (historyItems.isNullOrEmpty()) {
                binding.rvHistory.visibility = View.GONE
                binding.tvEmptyHistory.visibility = View.VISIBLE
            } else {
                binding.rvHistory.visibility = View.VISIBLE
                binding.tvEmptyHistory.visibility = View.GONE
                historyAdapter.submitList(historyItems)
            }
        }

        // Observe analytics data (placeholders for now)
        viewModel.getAlignmentTrackerData().observe(viewLifecycleOwner) { alignmentText ->
            binding.tvAlignmentTracker.text = alignmentText
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}