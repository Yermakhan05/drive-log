package com.example.drivelog.viewmodel

sealed class AddTripUiState {
    data object Idle : AddTripUiState()
    data object Loading : AddTripUiState()
    data object Success : AddTripUiState()
    data class Error(val message: String) : AddTripUiState()
}
