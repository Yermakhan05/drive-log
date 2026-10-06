package com.example.drivelog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.drivelog.data.api.RetrofitClient
import com.example.drivelog.data.model.Trip
import com.example.drivelog.data.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Locale

class AddTripViewModel(
    private val repository: TripRepository = TripRepository(RetrofitClient.tripApi),
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddTripUiState>(AddTripUiState.Idle)
    val uiState: StateFlow<AddTripUiState> = _uiState.asStateFlow()

    fun submitTrip(
        id: String,
        start: String,
        end: String,
        amount: String,
        payment: String,
        commission: String,
    ) {
        val trimmedId = id.trim()
        val trimmedStart = start.trim()
        val trimmedEnd = end.trim()
        val amountValue = amount.trim().toDoubleOrNull()
        val commissionValue = commission.trim().toDoubleOrNull()

        val validationMessage = validate(
            id = trimmedId,
            start = trimmedStart,
            end = trimmedEnd,
            amount = amountValue,
            commission = commissionValue,
        )
        if (validationMessage != null) {
            _uiState.value = AddTripUiState.Error(validationMessage)
            return
        }

        viewModelScope.launch {
            _uiState.value = AddTripUiState.Loading
            try {
                repository.createTrip(
                    Trip(
                        id = trimmedId,
                        start = trimmedStart,
                        end = trimmedEnd,
                        amount = amountValue ?: 0.0,
                        payment = payment,
                        commission = commissionValue ?: 0.0,
                    )
                )
                _uiState.value = AddTripUiState.Success
            } catch (error: HttpException) {
                _uiState.value = AddTripUiState.Error(apiErrorMessage(error))
            } catch (error: Exception) {
                _uiState.value = AddTripUiState.Error(
                    error.message ?: "Could not add trip. Please try again.",
                )
            }
        }
    }

    fun clearError() {
        if (_uiState.value is AddTripUiState.Error) {
            _uiState.value = AddTripUiState.Idle
        }
    }

    private fun validate(
        id: String,
        start: String,
        end: String,
        amount: Double?,
        commission: Double?,
    ): String? {
        if (id.isBlank()) return "ID cannot be empty."
        if (amount == null || amount <= 0) return "Amount must be greater than 0."
        if (commission == null || commission < 0) return "Commission must be greater than or equal to 0."

        val startDate = parseDateTime(start) ?: return "Start must be a valid date and time."
        val endDate = parseDateTime(end) ?: return "End must be a valid date and time."
        if (!endDate.after(startDate)) return "End must be later than start."

        return null
    }

    private fun apiErrorMessage(error: HttpException): String {
        return when (error.code()) {
            409 -> "Trip with this ID already exists."
            else -> readableApiError(error)
        }
    }

    private fun readableApiError(error: HttpException): String {
        val body = error.response()?.errorBody()?.string().orEmpty()
        val parsed = parseErrors(body)
        return parsed ?: "API error ${error.code()}: ${error.message()}"
    }

    private fun parseErrors(body: String): String? {
        if (body.isBlank()) return null
        return runCatching {
            val root = JSONObject(body)
            val errors = root.opt("errors") ?: root
            flattenErrorValue(errors).joinToString(separator = "\n")
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun flattenErrorValue(value: Any?): List<String> {
        return when (value) {
            is JSONObject -> value.keys().asSequence().flatMap { key ->
                flattenErrorValue(value.get(key)).asSequence().map { message ->
                    if (key == "non_field_errors") message else "$key: $message"
                }
            }.toList()
            is JSONArray -> (0 until value.length()).flatMap { index ->
                flattenErrorValue(value.get(index))
            }
            null -> emptyList()
            else -> listOf(value.toString())
        }
    }

    private fun parseDateTime(value: String) = dateTimeFormats.firstNotNullOfOrNull { format ->
        runCatching { format.parse(value) }.getOrNull()
    }

    companion object {
        private val dateTimeFormats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US),
        ).onEach { it.isLenient = false }
    }
}
