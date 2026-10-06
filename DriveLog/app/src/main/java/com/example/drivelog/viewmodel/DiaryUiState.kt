package com.example.drivelog.viewmodel

import com.example.drivelog.data.model.DailySummary
import com.example.drivelog.data.model.Trip

sealed class DiaryUiState {
    abstract val selectedDate: String

    data class Loading(
        override val selectedDate: String,
    ) : DiaryUiState()

    data class Success(
        override val selectedDate: String,
        val summary: DailySummary,
        val trips: List<Trip>,
    ) : DiaryUiState()

    data class Error(
        override val selectedDate: String,
        val message: String,
    ) : DiaryUiState()
}
