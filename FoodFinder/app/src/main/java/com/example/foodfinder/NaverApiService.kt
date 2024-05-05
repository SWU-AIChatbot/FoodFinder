package com.example.foodfinder


import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface NaverApiService {
    @Headers(
        "X-NCP-APIGW-API-KEY-ID: 9i62rvnuq7",
        "X-NCP-APIGW-API-KEY: eplXBmsvwkMQRVJFLw1iLS8qiTHk8oQDK6nmsjoa"
    )

    @GET("krdict/v1/romanization")
    suspend fun getRomanization(@Query("query") query: String): RomanizationResponse

    companion object {
        private const val BASE_URL = "https://naveropenapi.apigw.ntruss.com/"

        fun create(): NaverApiService {
            val client = OkHttpClient.Builder().build()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(NaverApiService::class.java)
        }
    }

    data class RomanizationResponse(
        val aResult: List<ResultItem>
    )

    data class ResultItem(
        val sFirstName: String,
        val aItems: List<NameItem>
    )

    data class NameItem(
        val name: String,
        val score: String
    )
}

