package com.example.drivelog.ui.screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.drivelog.R
import com.example.drivelog.databinding.FragmentDriverDiaryBinding
import com.example.drivelog.ui.components.TripAdapter
import com.example.drivelog.ui.components.formatMoney
import com.example.drivelog.viewmodel.DiaryUiState
import com.example.drivelog.viewmodel.DriverDiaryViewModel
import kotlinx.coroutines.launch

class DriverDiaryFragment : Fragment() {

    private var _binding: FragmentDriverDiaryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DriverDiaryViewModel by viewModels()
    private val tripAdapter = TripAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDriverDiaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tripsList.adapter = tripAdapter
        binding.previousDayButton.setOnClickListener { viewModel.goToPreviousDay() }
        binding.nextDayButton.setOnClickListener { viewModel.goToNextDay() }
        binding.retryButton.setOnClickListener { viewModel.retry() }
        binding.addTripButton.setOnClickListener { openAddTrip() }

        setFragmentResultListener(AddTripFragment.REQUEST_KEY) { _, _ ->
            viewModel.refresh()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: DiaryUiState) {
        binding.selectedDate.text = state.selectedDate
        binding.loadingIndicator.isVisible = state is DiaryUiState.Loading
        binding.errorGroup.isVisible = state is DiaryUiState.Error
        binding.summaryCard.isVisible = state is DiaryUiState.Success
        binding.tripsList.isVisible = state is DiaryUiState.Success
        binding.emptyTrips.isVisible = state is DiaryUiState.Success && state.trips.isEmpty()

        when (state) {
            is DiaryUiState.Loading -> {
                tripAdapter.submitList(emptyList())
            }
            is DiaryUiState.Error -> {
                binding.errorMessage.text = state.message
                tripAdapter.submitList(emptyList())
            }
            is DiaryUiState.Success -> {
                bindSummary(state)
                tripAdapter.submitList(state.trips)
            }
        }
    }

    private fun bindSummary(state: DiaryUiState.Success) {
        val summary = state.summary
        binding.tripsCountValue.text = summary.tripsCount.toString()
        binding.revenueValue.text = formatMoney(summary.revenue)
        binding.commissionValue.text = formatMoney(summary.commission)
        binding.netIncomeValue.text = formatMoney(summary.netIncome)
        binding.cashValue.text = formatMoney(summary.cash)
        binding.cardValue.text = formatMoney(summary.card)
    }

    private fun openAddTrip() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, AddTripFragment())
            .addToBackStack(AddTripFragment::class.java.simpleName)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.tripsList.adapter = null
        _binding = null
    }
}
