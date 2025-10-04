package com.st10321779.rootedwealth.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.st10321779.rootedwealth.databinding.FragmentHistoryBinding
import com.st10321779.rootedwealth.theme.ThemeManager

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        ThemeManager.applyTheme(requireContext(), ThemeManager.getSelectedTheme(requireContext()), binding.root)

        // TODO: Setup RecyclerView and ViewModel to show expense history
        binding.tvPlaceholder.text = "History & Analytics will be displayed here."

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}