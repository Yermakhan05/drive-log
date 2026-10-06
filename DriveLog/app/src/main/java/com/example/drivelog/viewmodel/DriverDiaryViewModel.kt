package com.example.drivelog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.drivelog.data.api.RetrofitClient
import com.example.drivelog.data.repository.TripRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class DriverDiaryViewModel(
    private val repository: TripRepository = TripRepository(RetrofitClient.tripApi),
) : ViewModel() {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        isLenient = false
        timeZone = TimeZone.getDefault()
    }

    private val selectedDate = MutableStateFlow(today())
    private val refreshTick = MutableStateFlow(0)

    val uiState: StateFlow<DiaryUiState> = combine(selectedDate, refreshTick) { date, _ ->
        date
    }.flatMapLatest { date ->
        flow {
            emit(DiaryUiState.Loading(date))
            try {
                val summary = repository.getSummary(date)
                val trips = repository.getTrips(date)
                emit(DiaryUiState.Success(date, summary, trips))
            } catch (error: Exception) {
                emit(
                    DiaryUiState.Error(
                        selectedDate = date,
                        message = error.message ?: "Failed to load diary",
                    )
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DiaryUiState.Loading(selectedDate.value),
    )

    fun goToPreviousDay() {
        selectedDate.value = shiftDate(selectedDate.value, -1)
    }

    fun goToNextDay() {
        selectedDate.value = shiftDate(selectedDate.value, 1)
    }

    fun retry() {
        refreshTick.value += 1
    }

    fun refresh() {
        refreshTick.value += 1
    }

    private fun today(): String = dateFormatter.format(Date())

    private fun shiftDate(date: String, days: Int): String {
        val calendar = Calendar.getInstance()
        calendar.time = dateFormatter.parse(date) ?: Date()
        calendar.add(Calendar.DAY_OF_MONTH, days)
        return dateFormatter.format(calendar.time)
    }
}
