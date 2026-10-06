package com.example.drivelog.data.model

import com.google.gson.annotations.SerializedName

data class DailySummary(
    @SerializedName("date") val date: String,
    @SerializedName("trips_count") val tripsCount: Int,
    @SerializedName("revenue") val revenue: Double,
    @SerializedName("commission") val commission: Double,
    @SerializedName("net_income") val netIncome: Double,
    @SerializedName("cash") val cash: Double,
    @SerializedName("card") val card: Double,
)
