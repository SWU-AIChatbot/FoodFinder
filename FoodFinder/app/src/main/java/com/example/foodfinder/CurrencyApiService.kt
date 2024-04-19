package com.example.foodfinder

import retrofit2.http.GET
import retrofit2.http.Query

interface CurrencyApiService {
    @GET("convert")
    suspend fun getExchangeRate(
        @Query("q") query: String
    ): ExchangeRateResponse
}

data class ExchangeRateResponse(
    val success: Boolean,
    val results: Map<String, ExchangeRateInfo>?
)

data class ExchangeRateInfo(
    val rate: Double,
    val timestamp: Long
)
