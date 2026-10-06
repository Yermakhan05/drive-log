package com.example.drivelog.data.api

import com.example.drivelog.data.model.DailySummary
import com.example.drivelog.data.model.Trip
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface TripApi {
    @GET("api/trips/")
    suspend fun getTrips(@Query("date") date: String): List<Trip>

    @GET("api/trips/summary/")
    suspend fun getSummary(@Query("date") date: String): DailySummary

    @POST("api/trips/")
    suspend fun createTrip(@Body trip: Trip): Trip
}
