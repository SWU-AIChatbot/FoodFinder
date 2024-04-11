package com.example.foodfinder

import retrofit2.http.GET
import retrofit2.http.Query

interface ExchangeRateApiService {
    @GET("latest")
    suspend fun getLatestExchangeRates(
        @Query("apikey") apiKey: String,
        @Query("currencies") currencies: String,
        @Query("base_currency") baseCurrency: String
    ): ExchangeRateResponse

    data class ExchangeRateResponse(
        val data: ExchangeRateData
    )

    data class ExchangeRateData(
        val USD: Double
    )
}



