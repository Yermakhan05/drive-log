package com.example.drivelog.ui.screen

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.drivelog.R
import com.example.drivelog.databinding.FragmentAddTripBinding
import com.example.drivelog.viewmodel.AddTripUiState
import com.example.drivelog.viewmodel.AddTripViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddTripFragment : Fragment() {

    private var _binding: FragmentAddTripBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddTripViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAddTripBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPaymentMenu()
        setupDateTimePickers()
        binding.cancelButton.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.saveButton.setOnClickListener { submitTrip() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it) }
            }
        }
    }

    private fun setupPaymentMenu() {
        val labels = resources.getStringArray(R.array.payment_labels)
        binding.paymentInput.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels)
        )
        binding.paymentInput.setText(labels.first(), false)
    }

    private fun setupDateTimePickers() {
        listOf(binding.startInput, binding.endInput).forEach { input ->
            input.isFocusable = false
            input.isCursorVisible = false
            input.keyListener = null
            input.setOnClickListener { showDateTimePicker(input) }
        }
    }

    private fun showDateTimePicker(input: com.google.android.material.textfield.TextInputEditText) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                showTimePicker(input, calendar)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    private fun showTimePicker(
        input: com.google.android.material.textfield.TextInputEditText,
        calendar: Calendar,
    ) {
        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                input.setText(displayDateTimeFormatter.format(calendar.time))
                input.tag = apiDateTimeFormatter.format(calendar.time)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true,
        ).show()
    }

    private fun submitTrip() {
        viewModel.clearError()
        binding.errorMessage.isVisible = false
        binding.errorMessage.text = null

        viewModel.submitTrip(
            id = binding.idInput.text?.toString().orEmpty(),
            start = binding.startInput.tag?.toString() ?: binding.startInput.text?.toString().orEmpty(),
            end = binding.endInput.tag?.toString() ?: binding.endInput.text?.toString().orEmpty(),
            amount = binding.amountInput.text?.toString().orEmpty(),
            payment = selectedPayment(),
            commission = binding.commissionInput.text?.toString().orEmpty(),
        )
    }

    private fun selectedPayment(): String {
        return when (binding.paymentInput.text?.toString()) {
            getString(R.string.payment_card) -> "card"
            else -> "cash"
        }
    }

    private fun render(state: AddTripUiState) {
        binding.progressBar.isVisible = state is AddTripUiState.Loading
        binding.saveButton.isEnabled = state !is AddTripUiState.Loading
        binding.cancelButton.isEnabled = state !is AddTripUiState.Loading

        when (state) {
            AddTripUiState.Idle,
            AddTripUiState.Loading,
            -> Unit
            is AddTripUiState.Error -> {
                binding.errorMessage.text = state.message
                binding.errorMessage.isVisible = true
            }
            AddTripUiState.Success -> {
                setFragmentResult(REQUEST_KEY, Bundle.EMPTY)
                parentFragmentManager.popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val REQUEST_KEY = "add_trip_result"
        private val displayDateTimeFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        private val apiDateTimeFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
    }
}
