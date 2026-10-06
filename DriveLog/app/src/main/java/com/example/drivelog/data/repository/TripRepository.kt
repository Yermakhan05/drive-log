package com.example.drivelog.data.repository

import com.example.drivelog.data.api.TripApi
import com.example.drivelog.data.model.DailySummary
import com.example.drivelog.data.model.Trip

class TripRepository(
    private val api: TripApi,
) {
    suspend fun getTrips(date: String): List<Trip> = api.getTrips(date)

    suspend fun getSummary(date: String): DailySummary = api.getSummary(date)

    suspend fun createTrip(trip: Trip): Trip = api.createTrip(trip)
}
