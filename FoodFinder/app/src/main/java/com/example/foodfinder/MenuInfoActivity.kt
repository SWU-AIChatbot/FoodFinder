package com.example.foodfinder

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MenuInfoActivity : AppCompatActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_info)
        val checkBtn = findViewById<ImageView>(R.id.check_btn)
        val usdTv = findViewById<TextView>(R.id.usd_tv)
        val kwrEt = findViewById<EditText>(R.id.kwr_et)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.freecurrencyapi.com/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val exchangeRateApiService = retrofit.create(ExchangeRateApiService::class.java)


        checkBtn.setOnClickListener {
            // kwr_et에 입력된 값을 가져옴
            val kwrAmount = kwrEt.text.toString().toDoubleOrNull()

            if (kwrAmount != null) {

                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        val response = exchangeRateApiService.getLatestExchangeRates(
                            "fca_live_UEh7ozfhJgsNKf9gpGgUGRxSdqEYQL1bEbDR66vA",
                            "USD",
                            "KRW"
                        )
                        val usdRate = response.data.USD ?: 0.0 // USD 환율을 가져옴

                        val usdAmount = kwrAmount * usdRate

                        // 계산된 환율을 usd_tv에 표시
                        withContext(Dispatchers.Main) {
                            usdTv.text = String.format("%.2f", usdAmount)
                            Log.d("환율계산","usd -> ${usdTv.text}")


                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MenuInfoActivity, "Error occurred", Toast.LENGTH_SHORT)
                                .show()
                        }
                        e.printStackTrace()
                    }
                }

            } else {
                // kwr_et에 유효한 숫자가 입력되지 않은 경우 사용자에게 메시지 표시
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

