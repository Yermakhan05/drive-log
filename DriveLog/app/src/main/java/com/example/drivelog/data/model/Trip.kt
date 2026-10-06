package com.example.drivelog.data.model

import com.google.gson.annotations.SerializedName

data class Trip(
    @SerializedName("id") val id: String,
    @SerializedName("start") val start: String,
    @SerializedName("end") val end: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("payment") val payment: String,
    @SerializedName("commission") val commission: Double,
)
